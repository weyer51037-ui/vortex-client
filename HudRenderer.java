package dev.vortex.hud;

import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import dev.vortex.module.Setting;
import dev.vortex.ui.Draw;
import dev.vortex.ui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * HUD elements. Text is cached and rebuilt only every ~250 ms, so there is no per-frame
 * string formatting/allocation (this is the "HUD caching" optimization).
 */
public final class HudRenderer {
    private static String fpsText = "", memText = "", coordText = "";
    private static long lastBuild;

    public static void render(DrawContext c) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        FrameStats.frame();
        long now = System.currentTimeMillis();
        if (now - lastBuild > 250) { rebuild(mc); lastBuild = now; }
        TextRenderer tr = mc.textRenderer;
        box(c, tr, ModuleManager.FPS_HUD, fpsText, ModuleManager.FPS_HUD_X, ModuleManager.FPS_HUD_Y, ModuleManager.FPS_HUD_SCALE, ModuleManager.FPS_HUD_BG);
        box(c, tr, ModuleManager.MEM_HUD, memText, ModuleManager.MEM_HUD_X, ModuleManager.MEM_HUD_Y, ModuleManager.MEM_HUD_SCALE, ModuleManager.MEM_HUD_BG);
        box(c, tr, ModuleManager.COORDS_HUD, coordText, ModuleManager.COORDS_X, ModuleManager.COORDS_Y, ModuleManager.COORDS_SCALE, ModuleManager.COORDS_BG);
    }

    private static void rebuild(MinecraftClient mc) {
        fpsText = String.format("%d FPS  •  %.1f ms", Math.round(FrameStats.currentFps()), FrameStats.lastFrameMs());
        Runtime r = Runtime.getRuntime();
        long used = (r.totalMemory() - r.freeMemory()) >> 20, total = r.totalMemory() >> 20, max = r.maxMemory() >> 20;
        memText = "RAM " + used + " / " + total + " MB (max " + max + ")";
        if (mc.player != null) {
            coordText = String.format("XYZ %d %d %d  %s", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ(),
                    mc.player.getHorizontalFacing().asString());
        }
    }

    private static void box(DrawContext c, TextRenderer tr, Module m, String text, Setting x, Setting y, Setting scale, Setting bg) {
        if (!m.enabled || text.isEmpty()) return;
        Theme.Palette p = Theme.current();
        int w = tr.getWidth(text) + 10, h = 16;
        var ms = c.getMatrices();
        ms.push();
        ms.translate(x.getInt(), y.getInt(), 0);
        ms.scale(scale.getFloat(), scale.getFloat(), 1);
        Draw.rounded(c, 0, 0, w, h, 4, Theme.alpha(0xFF000000, bg.getFloat()));
        c.fill(0, 3, 2, h - 3, p.accent());
        c.drawText(tr, text, 6, 4, 0xFFFFFFFF, true);
        ms.pop();
    }

    private HudRenderer() {}
}
