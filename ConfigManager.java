package dev.vortex.config;

import com.google.gson.*;
import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import dev.vortex.module.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Persists all settings to config/vortex/config.json.
 * - Writes go to a temp file, then an atomic move, so a crash can't leave a half-written file.
 * - A corrupt file is renamed to config.json.corrupt and defaults are used.
 * - Unknown/invalid values are ignored per-setting.
 * - Saves are debounced: call markDirty() anywhere, the tick hook flushes at most every few seconds.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("vortex");
    private static final Path FILE = DIR.resolve("config.json");

    // UI-level (non-module) settings
    public static String theme = "Vortex";
    public static String background = "Aurora";
    public static String customBackgroundPath = "";
    public static boolean customTitleScreen = true;
    public static float backgroundSpeed = 1.0f;

    private static volatile boolean dirty;
    private static long lastSave;

    public static void markDirty() { dirty = true; }

    public static void tick() {
        if (dirty && System.currentTimeMillis() - lastSave > 3000) save();
    }

    public static synchronized void load() {
        try {
            Files.createDirectories(DIR);
            if (!Files.exists(FILE)) { save(); return; }
            String text = Files.readString(FILE, StandardCharsets.UTF_8);
            JsonObject root;
            try {
                root = JsonParser.parseString(text).getAsJsonObject();
            } catch (RuntimeException bad) {
                quarantine();
                return;
            }
            theme = str(root, "theme", theme);
            background = str(root, "background", background);
            customBackgroundPath = str(root, "customBackgroundPath", customBackgroundPath);
            customTitleScreen = root.has("customTitleScreen") && root.get("customTitleScreen").isJsonPrimitive()
                    ? root.get("customTitleScreen").getAsBoolean() : customTitleScreen;
            if (root.has("backgroundSpeed") && root.get("backgroundSpeed").isJsonPrimitive()) {
                try { backgroundSpeed = Math.max(0.1f, Math.min(4f, root.get("backgroundSpeed").getAsFloat())); }
                catch (RuntimeException ignored) {}
            }
            JsonObject mods = root.has("modules") && root.get("modules").isJsonObject() ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : ModuleManager.all()) {
                if (!mods.has(m.id) || !mods.get(m.id).isJsonObject()) continue;
                JsonObject o = mods.getAsJsonObject(m.id);
                if (o.has("enabled") && o.get("enabled").isJsonPrimitive()) {
                    try { m.enabled = o.get("enabled").getAsBoolean(); } catch (RuntimeException ignored) {}
                }
                if (o.has("favorite") && o.get("favorite").isJsonPrimitive()) {
                    try { m.favorite = o.get("favorite").getAsBoolean(); } catch (RuntimeException ignored) {}
                }
                if (o.has("keybind") && o.get("keybind").isJsonPrimitive()) {
                    try { m.keybind = o.get("keybind").getAsInt(); } catch (RuntimeException ignored) {}
                }
                for (Setting s : m.settings) s.fromJson(o.get(s.key));
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[Vortex] Failed to load config, using defaults: " + e);
        }
    }

    public static synchronized void save() {
        try {
            Files.createDirectories(DIR);
            JsonObject root = new JsonObject();
            root.addProperty("theme", theme);
            root.addProperty("background", background);
            root.addProperty("customBackgroundPath", customBackgroundPath);
            root.addProperty("customTitleScreen", customTitleScreen);
            root.addProperty("backgroundSpeed", backgroundSpeed);
            JsonObject mods = new JsonObject();
            for (Module m : ModuleManager.all()) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                o.addProperty("favorite", m.favorite);
                o.addProperty("keybind", m.keybind);
                for (Setting s : m.settings) o.add(s.key, s.toJson());
                mods.add(m.id, o);
            }
            root.add("modules", mods);
            Path tmp = DIR.resolve("config.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            lastSave = System.currentTimeMillis();
        } catch (IOException | RuntimeException e) {
            System.err.println("[Vortex] Failed to save config: " + e);
        }
    }

    private static void quarantine() {
        try {
            Files.move(FILE, DIR.resolve("config.json.corrupt"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
        save();
    }

    private static String str(JsonObject o, String k, String d) {
        try {
            return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d;
        } catch (RuntimeException e) {
            return d;
        }
    }

    private ConfigManager() {}
}
