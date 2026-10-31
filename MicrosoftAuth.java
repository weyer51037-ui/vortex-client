package dev.vortex.launcher;

import com.google.gson.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Microsoft -> Xbox Live -> XSTS -> Minecraft Services login using the OAuth *device code* flow
 * (no embedded browser, no password ever touches this app).
 *
 * Needs your own Azure app registration (public client, device code flow allowed) and, for the
 * final Minecraft step, Mojang's approval of that app: https://aka.ms/AppRegInfo
 */
public final class MicrosoftAuth {
    public record DeviceCode(String userCode, String verificationUri, String deviceCode, int interval, int expiresIn) {}
    public record Account(String name, String uuid, String mcAccessToken) {}

    private static final String TENANT = "https://login.microsoftonline.com/consumers/oauth2/v2.0";
    private static final String SCOPE = "XboxLive.signin offline_access";
    private static final Path TOKEN_FILE = LauncherConfig.HOME.resolve("account.json");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    private final String clientId;

    public MicrosoftAuth(String clientId) {
        this.clientId = clientId;
    }

    // ---- step 1: ask Microsoft for a code the user types at microsoft.com/link ----
    public DeviceCode requestCode() throws IOException, InterruptedException {
        JsonObject o = form(TENANT + "/devicecode", "client_id=" + enc(clientId) + "&scope=" + enc(SCOPE));
        if (o.has("error")) throw new IOException("Microsoft: " + describe(o));
        return new DeviceCode(o.get("user_code").getAsString(), o.get("verification_uri").getAsString(),
                o.get("device_code").getAsString(), o.has("interval") ? o.get("interval").getAsInt() : 5,
                o.has("expires_in") ? o.get("expires_in").getAsInt() : 900);
    }

    // ---- step 2: poll until the user finishes in the browser ----
    public Account completeLogin(DeviceCode dc, Consumer<String> status) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + dc.expiresIn() * 1000L;
        int interval = Math.max(2, dc.interval());
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval * 1000L);
            JsonObject o = form(TENANT + "/token", "grant_type=" + enc("urn:ietf:params:oauth:grant-type:device_code")
                    + "&client_id=" + enc(clientId) + "&device_code=" + enc(dc.deviceCode()));
            if (o.has("access_token")) {
                String refresh = o.has("refresh_token") ? o.get("refresh_token").getAsString() : null;
                if (refresh != null) storeRefresh(refresh);
                return minecraftFrom(o.get("access_token").getAsString(), status);
            }
            String err = o.has("error") ? o.get("error").getAsString() : "";
            if (err.equals("authorization_pending")) continue;
            if (err.equals("slow_down")) { interval += 5; continue; }
            throw new IOException("Login failed: " + describe(o));
        }
        throw new IOException("The login code expired. Try again.");
    }

    /** Silent login from the stored refresh token. Returns null if there is none / it was revoked. */
    public Account trySilent(Consumer<String> status) {
        try {
            String refresh = readRefresh();
            if (refresh == null) return null;
            JsonObject o = form(TENANT + "/token", "grant_type=refresh_token&client_id=" + enc(clientId)
                    + "&refresh_token=" + enc(refresh) + "&scope=" + enc(SCOPE));
            if (!o.has("access_token")) return null;
            if (o.has("refresh_token")) storeRefresh(o.get("refresh_token").getAsString());
            return minecraftFrom(o.get("access_token").getAsString(), status);
        } catch (Exception e) {
            return null;
        }
    }

    public static void signOut() {
        try { Files.deleteIfExists(TOKEN_FILE); } catch (IOException ignored) {}
    }

    // ---- Xbox / XSTS / Minecraft ----
    private Account minecraftFrom(String msToken, Consumer<String> status) throws IOException, InterruptedException {
        status.accept("Signing in to Xbox Live...");
        JsonObject xbl = json("https://user.auth.xboxlive.com/user/authenticate", """
                {"Properties":{"AuthMethod":"RPS","SiteName":"user.auth.xboxlive.com","RpsTicket":"d=%s"},
                 "RelyingParty":"http://auth.xboxlive.com","TokenType":"JWT"}""".formatted(msToken), null);
        String xblToken = xbl.get("Token").getAsString();
        String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

        status.accept("Getting Xbox security token...");
        JsonObject xsts = json("https://xsts.auth.xboxlive.com/xsts/authorize", """
                {"Properties":{"SandboxId":"RETAIL","UserTokens":["%s"]},
                 "RelyingParty":"rp://api.minecraftservices.com/","TokenType":"JWT"}""".formatted(xblToken), null);
        if (xsts.has("XErr")) throw new IOException(xstsError(xsts.get("XErr").getAsLong()));
        String xstsToken = xsts.get("Token").getAsString();

        status.accept("Signing in to Minecraft...");
        JsonObject mc = json("https://api.minecraftservices.com/authentication/login_with_xbox",
                "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}", null);
        if (!mc.has("access_token")) {
            throw new IOException("Minecraft rejected the login. If your Azure app is new it must be approved by Mojang first (https://aka.ms/AppRegInfo). Response: " + mc);
        }
        String mcToken = mc.get("access_token").getAsString();

        status.accept("Loading your profile...");
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + mcToken).timeout(Duration.ofSeconds(20)).GET().build();
        HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() == 404) throw new IOException("This Microsoft account does not own Minecraft: Java Edition.");
        if (r.statusCode() / 100 != 2) throw new IOException("Profile request failed (HTTP " + r.statusCode() + ").");
        JsonObject p = JsonParser.parseString(r.body()).getAsJsonObject();
        return new Account(p.get("name").getAsString(), p.get("id").getAsString(), mcToken);
    }

    private static String xstsError(long code) {
        if (code == 2148916233L) return "This Microsoft account has no Xbox profile. Sign in once at xbox.com to create one.";
        if (code == 2148916235L) return "Xbox Live is not available in your country/region.";
        if (code == 2148916236L || code == 2148916237L) return "This account needs adult verification on Xbox.";
        if (code == 2148916238L) return "This is a child account; add it to a Microsoft Family first.";
        return "Xbox error " + code;
    }

    // ---- http helpers ----
    private static JsonObject form(String url, String body) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return parse(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body());
    }

    private static JsonObject json(String url, String body, String bearer) throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return parse(r.body());
    }

    private static JsonObject parse(String s) throws IOException {
        try {
            return JsonParser.parseString(s).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Unexpected response from server.");
        }
    }

    private static String describe(JsonObject o) {
        String d = o.has("error_description") ? o.get("error_description").getAsString() : "";
        String e = o.has("error") ? o.get("error").getAsString() : "unknown";
        String first = d.isEmpty() ? e : d.split("\r?\n")[0];
        if (e.equals("invalid_client") || first.contains("AADSTS700016") || first.contains("AADSTS70002")) {
            first += " (Check your Azure client ID, and that 'Allow public client flows' is enabled.)";
        }
        return first;
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    // ---- refresh-token storage (local file, owner-only permissions where supported) ----
    private static void storeRefresh(String token) {
        try {
            Files.createDirectories(LauncherConfig.HOME);
            JsonObject o = new JsonObject();
            o.addProperty("refresh", token);
            Files.writeString(TOKEN_FILE, o.toString());
            try { Files.setPosixFilePermissions(TOKEN_FILE, PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) {}
        } catch (IOException ignored) {}
    }

    private static String readRefresh() {
        try {
            if (!Files.exists(TOKEN_FILE)) return null;
            return JsonParser.parseString(Files.readString(TOKEN_FILE)).getAsJsonObject().get("refresh").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}
