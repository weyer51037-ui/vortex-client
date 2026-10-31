package dev.vortex.feature;

import dev.vortex.module.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Hold-to-zoom with a smooth ease. Client-side FOV only; fully legal on servers. */
public final class ZoomFeature {
    public static KeyBinding key;
    private static double current = 1.0;
    private static double scrollBonus = 0;

    public static void init() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vortex.zoom",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "category.vortex"));
    }

    public static boolean active() {
        return ModuleManager.ZOOM.enabled && key != null && key.isPressed();
    }

    /** Called by the scroll hook while zoom is held. */
    public static void onScroll(double dy) {
        scrollBonus = Math.max(-2, Math.min(6, scrollBonus + dy * 0.5));
    }

    public static double fovDivisor() {
        double target = active() ? Math.max(1.1, ModuleManager.ZOOM_FACTOR.getFloat() + scrollBonus) : 1.0;
        if (!active()) scrollBonus = 0;
        current += (target - current) * 0.25; // ease
        if (Math.abs(target - current) < 0.001) current = target;
        return current;
    }

    private ZoomFeature() {}
}
