package dev.vortex.mixin;

import dev.vortex.config.ConfigManager;
import dev.vortex.module.ModuleManager;
import dev.vortex.ui.VortexTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    /**
     * Real optimization: vanilla runs the render loop at the full limit even when the window is
     * in the background. Lowering it there frees CPU/GPU for whatever you're actually using.
     * Also caps FPS on the title screen / menus (no world loaded).
     */
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void vortex$limit(CallbackInfoReturnable<Integer> cir) {
        try {
            if (!ModuleManager.FPS_LIMITER.enabled) return;
            MinecraftClient self = (MinecraftClient) (Object) this;
            int limit = cir.getReturnValue();
            if (!self.isWindowFocused()) {
                limit = Math.min(limit, ModuleManager.UNFOCUSED_FPS.getInt());
            } else if (self.world == null) {
                limit = Math.min(limit, ModuleManager.MENU_FPS.getInt());
            }
            cir.setReturnValue(limit);
        } catch (Throwable ignored) {
            // never crash the render loop
        }
    }

    /** Swap the vanilla title screen (exact class only, so other mods' subclasses are untouched). */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen vortex$title(Screen screen) {
        if (ConfigManager.customTitleScreen && screen != null && screen.getClass() == TitleScreen.class) {
            return new VortexTitleScreen();
        }
        return screen;
    }
}
