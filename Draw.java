package dev.vortex.ui;

import net.minecraft.client.gui.DrawContext;

/** Small drawing helpers: rounded rectangles, soft shadows, switches. */
public final class Draw {

    /** Filled rounded rect built from scanlines (no textures, resolution independent). */
    public static void rounded(DrawContext c, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { c.fill(x, y, x + w, y + h, color); return; }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    public static void shadow(DrawContext c, int x, int y, int w, int h, int r) {
        for (int i = 4; i >= 1; i--) rounded(c, x - i, y - i + 2, w + i * 2, h + i * 2, r + i, 0x0A000000);
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int r, int color) {
        rounded(c, x - 1, y - 1, w + 2, h + 2, r + 1, color);
    }

    public static void toggle(DrawContext c, int x, int y, boolean on, float anim, Theme.Palette p) {
        int w = 28, h = 14;
        rounded(c, x, y, w, h, 7, Theme.mix(0xFF3A3A4A, p.accent(), anim));
        int kx = x + 2 + (int) (anim * (w - h));
        rounded(c, kx, y + 2, 10, 10, 5, 0xFFFFFFFF);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private Draw() {}
}
