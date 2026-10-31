package dev.vortex.module;

import dev.vortex.module.Setting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of every module. Hot-path modules are exposed as static fields so mixins never
 * perform map lookups or allocate per frame.
 */
public final class ModuleManager {
    private static final Map<String, Module> MODULES = new LinkedHashMap<>();

    // ---- Performance ----
    public static final Module FPS_LIMITER = reg(new Module("fps_limiter", "FPS Limiter",
            "Caps FPS when the window is unfocused or you are in menus. Saves CPU/GPU, heat and battery.",
            Category.PERFORMANCE, true));
    public static final Setting UNFOCUSED_FPS = FPS_LIMITER.add(Setting.integer("unfocused_fps", "Unfocused FPS", 15, 1, 120));
    public static final Setting MENU_FPS = FPS_LIMITER.add(Setting.integer("menu_fps", "Main menu FPS", 60, 10, 240));

    public static final Module PARTICLES = reg(new Module("particle_optimizer", "Particle Optimizer",
            "Skips cosmetic particles that are far away or over a cap. Never touches gameplay-relevant ones.",
            Category.PERFORMANCE, false));
    public static final Setting PARTICLE_DIST = PARTICLES.add(Setting.integer("max_distance", "Max distance (blocks)", 32, 8, 128));
    public static final Setting PARTICLE_EXPLOSIONS = PARTICLES.add(Setting.bool("hide_explosions", "Hide explosion particles", false));
    public static final Setting PARTICLE_WEATHER = PARTICLES.add(Setting.bool("hide_weather", "Hide rain/splash particles", false));

    public static final Module ENTITY_CULL = reg(new Module("entity_distance", "Entity Render Distance",
            "Stops rendering mobs/items beyond a distance. NEVER hides players or bosses. Off by default.",
            Category.PERFORMANCE, false));
    public static final Setting ENTITY_DIST = ENTITY_CULL.add(Setting.integer("distance", "Distance (blocks)", 64, 32, 256));

    // ---- HUD ----
    public static final Module FPS_HUD = reg(new Module("hud_fps", "FPS Counter",
            "Shows current FPS and frame time.", Category.HUD, true));
    public static final Setting FPS_HUD_X = FPS_HUD.add(Setting.integer("x", "X", 6, 0, 4000));
    public static final Setting FPS_HUD_Y = FPS_HUD.add(Setting.integer("y", "Y", 6, 0, 4000));
    public static final Setting FPS_HUD_SCALE = FPS_HUD.add(Setting.number("scale", "Scale", 1.0f, 0.5f, 3.0f));
    public static final Setting FPS_HUD_BG = FPS_HUD.add(Setting.number("bg_opacity", "Background opacity", 0.5f, 0f, 1f));

    public static final Module MEM_HUD = reg(new Module("hud_memory", "Memory Usage",
            "Shows used / allocated heap.", Category.HUD, false));
    public static final Setting MEM_HUD_X = MEM_HUD.add(Setting.integer("x", "X", 6, 0, 4000));
    public static final Setting MEM_HUD_Y = MEM_HUD.add(Setting.integer("y", "Y", 26, 0, 4000));
    public static final Setting MEM_HUD_SCALE = MEM_HUD.add(Setting.number("scale", "Scale", 1.0f, 0.5f, 3.0f));
    public static final Setting MEM_HUD_BG = MEM_HUD.add(Setting.number("bg_opacity", "Background opacity", 0.5f, 0f, 1f));

    public static final Module COORDS_HUD = reg(new Module("hud_coords", "Coordinates",
            "Shows your block position and facing.", Category.HUD, false));
    public static final Setting COORDS_X = COORDS_HUD.add(Setting.integer("x", "X", 6, 0, 4000));
    public static final Setting COORDS_Y = COORDS_HUD.add(Setting.integer("y", "Y", 46, 0, 4000));
    public static final Setting COORDS_SCALE = COORDS_HUD.add(Setting.number("scale", "Scale", 1.0f, 0.5f, 3.0f));
    public static final Setting COORDS_BG = COORDS_HUD.add(Setting.number("bg_opacity", "Background opacity", 0.5f, 0f, 1f));

    // ---- Visuals / Utility (Vortex-only features) ----
    public static final Module ZOOM = reg(new Module("zoom", "Zoom",
            "Hold C (rebindable in Controls) to zoom in. Purely a client-side FOV change.",
            Category.VISUALS, true));
    public static final Setting ZOOM_FACTOR = ZOOM.add(Setting.number("factor", "Zoom factor", 4.0f, 1.5f, 10.0f));

    public static final Module TOGGLE_SPRINT_DISPLAY = reg(new Module("hud_sprint", "Sprint/Sneak Status",
            "Small HUD label showing whether you are sprinting or sneaking.", Category.UTILITY, false));

    private static Module reg(Module m) {
        MODULES.put(m.id, m);
        return m;
    }

    public static List<Module> all() { return new ArrayList<>(MODULES.values()); }

    public static Module get(String id) { return MODULES.get(id); }

    public static List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES.values()) if (m.category == c) out.add(m);
        return out;
    }

    private ModuleManager() {}
}
