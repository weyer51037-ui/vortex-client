package dev.vortex.ui;

import dev.vortex.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Configurable animated backgrounds. Presets are drawn procedurally (no assets, near-zero cost).
 * "Custom" loads a PNG from a path you choose; if it fails it falls back to Aurora.
 */
public final class BackgroundRenderer {
    public static final List<String> PRESETS = List.of("Aurora", "Nebula", "Grid", "Spotlight", "Solid", "Custom");
    private static final Identifier CUSTOM_ID = Identifier.of("vortex", "custom_background");
    private static String loadedPath = "";
    private static int imgW, imgH;
    private static boolean customOk;

    public static void render(DrawContext c, int w, int h) {
        Theme.Palette p = Theme.current();
        double t = (System.currentTimeMillis() % 3_600_000L) / 1000.0 * ConfigManager.backgroundSpeed;
        String mode = ConfigManager.background;
        if (mode.equals("Custom") && ensureCustom()) {
            c.drawTexture(CUSTOM_ID, 0, 0, w, h, 0, 0, imgW, imgH, imgW, imgH);
            c.fill(0, 0, w, h, 0x66000000);
            return;
        }
        c.fillGradient(0, 0, w, h, p.bg(), Theme.mix(p.bg(), p.panel(), 0.8f));
        switch (mode) {
            case "Nebula" -> blobs(c, w, h, t * 0.5, p, 7);
            case "Grid" -> grid(c, w, h, t, p);
            case "Spotlight" -> spotlight(c, w, h, t, p);
            case "Solid" -> { }
            default -> blobs(c, w, h, t, p, 4); // Aurora (also fallback for Custom)
        }
    }

    private static void blobs(DrawContext c, int w, int h, double t, Theme.Palette p, int n) {
        for (int i = 0; i < n; i++) {
            double ph = i * 1.7;
            int cx = (int) (w * (0.5 + 0.42 * Math.sin(t * 0.21 + ph)));
            int cy = (int) (h * (0.5 + 0.38 * Math.cos(t * 0.17 + ph * 1.3)));
            int col = (i % 2 == 0) ? p.accent() : p.accent2();
            int rad = (int) (Math.min(w, h) * (0.28 + 0.06 * Math.sin(t * 0.4 + ph)));
            for (int r = rad; r > 0; r -= Math.max(6, rad / 14)) {
                float a = (1f - (float) r / rad);
                c.fill(cx - r, cy - r, cx + r, cy + r, Theme.alpha(col, a * a * 0.045f));
            }
        }
    }

    private static void grid(DrawContext c, int w, int h, double t, Theme.Palette p) {
        int step = 28;
        int off = (int) ((t * 12) % step);
        for (int x = -step + off; x < w; x += step) c.fill(x, 0, x + 1, h, Theme.alpha(p.accent(), 0.10f));
        for (int y = -step + off; y < h; y += step) c.fill(0, y, w, y + 1, Theme.alpha(p.accent2(), 0.08f));
        blobs(c, w, h, t * 0.3, p, 2);
    }

    private static void spotlight(DrawContext c, int w, int h, double t, Theme.Palette p) {
        int cx = (int) (w * (0.5 + 0.25 * Math.sin(t * 0.3)));
        for (int r = h; r > 0; r -= 12) {
            float a = 1f - (float) r / h;
            c.fill(cx - r, h / 2 - r, cx + r, h / 2 + r, Theme.alpha(p.accent(), a * 0.03f));
        }
    }

    private static boolean ensureCustom() {
        String path = ConfigManager.customBackgroundPath;
        if (path == null || path.isBlank()) return false;
        if (path.equals(loadedPath)) return customOk;
        loadedPath = path;
        customOk = false;
        try {
            Path f = Path.of(path.trim());
            if (!Files.isRegularFile(f) || Files.size(f) > 20_000_000) return false;
            try (InputStream in = Files.newInputStream(f)) {
                NativeImage img = NativeImage.read(in); // PNG only in vanilla
                imgW = img.getWidth();
                imgH = img.getHeight();
                MinecraftClient.getInstance().getTextureManager().registerTexture(CUSTOM_ID, new NativeImageBackedTexture(img));
                customOk = true;
            }
        } catch (Exception | LinkageError e) {
            customOk = false;
        }
        return customOk;
    }

    public static boolean customLoaded() { return customOk; }

    /** Call after the path changes so it is re-read. */
    public static void invalidate() { loadedPath = ""; }

    private BackgroundRenderer() {}
}
