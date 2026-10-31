package dev.vortex.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.*;

/** Launcher settings + data directory (~/.vortex). */
public final class LauncherConfig {
    public static final Path HOME = Path.of(System.getProperty("user.home"), ".vortex");
    private static final Path FILE = HOME.resolve("launcher.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String clientId = "";
    public String mcVersion = "1.21.1";
    public String theme = "Vortex";
    public String background = "Aurora";
    public String customBackground = "";

    public static LauncherConfig load() {
        try {
            if (Files.exists(FILE)) {
                LauncherConfig c = GSON.fromJson(Files.readString(FILE), LauncherConfig.class);
                if (c != null) return c.sanitize();
            }
        } catch (Exception ignored) {
            // corrupt -> defaults
        }
        return new LauncherConfig();
    }

    private LauncherConfig sanitize() {
        if (clientId == null) clientId = "";
        if (mcVersion == null || !mcVersion.matches("[0-9a-zA-Z._-]{1,20}")) mcVersion = "1.21.1";
        if (theme == null) theme = "Vortex";
        if (background == null) background = "Aurora";
        if (customBackground == null) customBackground = "";
        return this;
    }

    public String effectiveClientId() {
        String env = System.getenv("VORTEX_MS_CLIENT_ID");
        return (env != null && !env.isBlank()) ? env.trim() : clientId.trim();
    }

    public Path modsDir() {
        return HOME.resolve("instances").resolve(mcVersion).resolve("mods");
    }

    public void save() {
        try {
            Files.createDirectories(HOME);
            Path tmp = HOME.resolve("launcher.json.tmp");
            Files.writeString(tmp, GSON.toJson(this));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Could not save launcher config: " + e);
        }
    }
}
