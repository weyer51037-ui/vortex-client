package dev.vortex.launcher;

import com.google.gson.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * Modrinth API v2 client: search, version lookup, dependency resolution, verified downloads.
 * Safety: downloads only from cdn.modrinth.com over HTTPS, SHA-512 must match what the API reported,
 * filenames are sanitised (no path traversal), and writes go through a temp file.
 */
public final class ModrinthClient {
    public record Hit(String slug, String title, String description, long downloads, String author) {}

    public static final List<String> OPTIMIZER_PACK = List.of(
            "fabric-api", "sodium", "lithium", "ferrite-core", "entityculling", "immediatelyfast", "modernfix", "krypton");

    private static final String API = "https://api.modrinth.com/v2";
    private static final String UA = "VortexClient/0.1 (launcher beta)";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    public List<Hit> search(String query, String mcVersion, int limit) throws IOException, InterruptedException {
        String facets = "[[\"project_type:mod\"],[\"categories:fabric\"],[\"versions:" + mcVersion + "\"]]";
        String url = API + "/search?query=" + enc(query) + "&limit=" + limit + "&index=" + (query.isBlank() ? "downloads" : "relevance")
                + "&facets=" + enc(facets);
        JsonObject o = getJson(url).getAsJsonObject();
        List<Hit> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("hits")) {
            JsonObject h = e.getAsJsonObject();
            out.add(new Hit(h.get("slug").getAsString(), h.get("title").getAsString(), h.get("description").getAsString(),
                    h.get("downloads").getAsLong(), h.has("author") ? h.get("author").getAsString() : ""));
        }
        return out;
    }

    /** Installs a project and its required dependencies. Returns the files written. */
    public List<Path> install(String slugOrId, String mcVersion, Path modsDir, Consumer<String> log)
            throws IOException, InterruptedException {
        Files.createDirectories(modsDir);
        List<Path> written = new ArrayList<>();
        installRec(slugOrId, mcVersion, modsDir, log, new HashSet<>(), written);
        return written;
    }

    private void installRec(String project, String mc, Path dir, Consumer<String> log, Set<String> seen, List<Path> written)
            throws IOException, InterruptedException {
        if (!seen.add(project)) return;
        String url = API + "/project/" + enc(project) + "/version?loaders=" + enc("[\"fabric\"]") + "&game_versions=" + enc("[\"" + mc + "\"]");
        JsonArray versions = getJson(url).getAsJsonArray();
        if (versions.isEmpty()) {
            log.accept("No Fabric build of '" + project + "' for " + mc + " - skipped.");
            return;
        }
        JsonObject v = versions.get(0).getAsJsonObject(); // newest first
        for (JsonElement d : v.getAsJsonArray("dependencies")) {
            JsonObject dep = d.getAsJsonObject();
            if (dep.get("dependency_type").getAsString().equals("required") && dep.has("project_id") && !dep.get("project_id").isJsonNull()) {
                installRec(dep.get("project_id").getAsString(), mc, dir, log, seen, written);
            }
        }
        JsonObject file = null;
        for (JsonElement f : v.getAsJsonArray("files")) {
            JsonObject fo = f.getAsJsonObject();
            if (fo.has("primary") && fo.get("primary").getAsBoolean()) { file = fo; break; }
            if (file == null) file = fo;
        }
        if (file == null) return;
        String name = sanitize(file.get("filename").getAsString());
        Path target = dir.resolve(name).normalize();
        if (!target.startsWith(dir.normalize())) throw new IOException("Unsafe file name from server: " + name);
        if (Files.exists(target)) { log.accept("Already installed: " + name); return; }
        log.accept("Downloading " + name + " ...");
        download(file.get("url").getAsString(), file.getAsJsonObject("hashes").get("sha512").getAsString(), target);
        written.add(target);
        log.accept("Installed " + name);
    }

    private static void download(String url, String sha512, Path target) throws IOException, InterruptedException {
        URI u = URI.create(url);
        if (!"https".equals(u.getScheme()) || !"cdn.modrinth.com".equals(u.getHost())) {
            throw new IOException("Refusing to download from untrusted host: " + u.getHost());
        }
        Path tmp = Files.createTempFile(target.getParent(), "vortex-", ".part");
        try {
            HttpResponse<InputStream> r = HTTP.send(HttpRequest.newBuilder(u).header("User-Agent", UA)
                    .timeout(Duration.ofMinutes(2)).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            if (r.statusCode() / 100 != 2) throw new IOException("Download failed: HTTP " + r.statusCode());
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            try (InputStream in = r.body(); var out = Files.newOutputStream(tmp)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) { md.update(buf, 0, n); out.write(buf, 0, n); }
            }
            String got = HexFormat.of().formatHex(md.digest());
            if (!got.equalsIgnoreCase(sha512)) throw new IOException("Checksum mismatch - download discarded.");
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private static JsonElement getJson(String url) throws IOException, InterruptedException {
        HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", UA)
                .timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() / 100 != 2) throw new IOException("Modrinth returned HTTP " + r.statusCode());
        return JsonParser.parseString(r.body());
    }

    private static String sanitize(String name) {
        String n = name.replaceAll("[^A-Za-z0-9._+\\- ]", "_");
        if (!n.endsWith(".jar")) n += ".jar";
        return n.startsWith(".") ? "_" + n : n;
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
