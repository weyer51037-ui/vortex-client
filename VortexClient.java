package dev.vortex;

import dev.vortex.config.ConfigManager;
import dev.vortex.feature.ZoomFeature;
import dev.vortex.hud.HudRenderer;
import dev.vortex.ui.ModMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class VortexClient implements ClientModInitializer {
    public static KeyBinding menuKey;

    @Override
    public void onInitializeClient() {
        try {
            ConfigManager.load();
        } catch (Throwable t) {
            System.err.println("[Vortex] Config load failed: " + t);
        }

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.vortex.menu",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.vortex"));
        ZoomFeature.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ModMenuScreen(null));
            }
            ConfigManager.tick();
        });
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            try {
                HudRenderer.render(ctx);
            } catch (Throwable t) {
                // a HUD bug must never crash the game
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> ConfigManager.save());
        System.out.println("[Vortex] Loaded.");
    }
}
