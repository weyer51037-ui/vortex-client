package dev.vortex.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * Original Vortex logo drawn in code: a rotating dual-arm spiral in accent colours
 * with a bright core, plus a wordmark. No external art, no third-party branding.
 */
public final class VortexLogo {

    /** Draws the spiral mark centred on (cx, cy). */
    public static void mark(DrawContext c, int cx, int cy, int size, double time, Theme.Palette p) {
        int points = 46;
        for (int arm = 0; arm < 2; arm++) {
            int col = arm == 0 ? p.accent() : p.accent2();
            for (int i = 0; i < points; i++) {
                double f = i / (double) points;
                double ang = f * 4.6 + arm * Math.PI + time * 0.9;
                double rad = f * size * 0.5;
                int px = cx + (int) Math.round(Math.cos(ang) * rad);
                int py = cy + (int) Math.round(Math.sin(ang) * rad);
                int s = Math.max(1, (int) Math.round(1 + f * size / 14.0));
                float a = 0.25f + 0.75f * (float) f;
                c.fill(px - s / 2, py - s / 2, px - s / 2 + s, py - s / 2 + s, Theme.alpha(col, a));
            }
        }
        int g = Math.max(3, size / 9);
        c.fill(cx - g - 2, cy - g - 2, cx + g + 2, cy + g + 2, Theme.alpha(p.accent(), 0.18f));
        c.fill(cx - g / 2, cy - g / 2, cx + g / 2 + 1, cy + g / 2 + 1, 0xFFFFFFFF);
    }

    /** Wordmark "VORTEX" with letter spacing and an accent underline. */
    public static void wordmark(DrawContext c, net.minecraft.client.font.TextRenderer tr, int cx, int y, float scale, Theme.Palette p) {
        String word = "V O R T E X";
        var m = c.getMatrices();
        m.push();
        m.translate(cx, y, 0);
        m.scale(scale, scale, 1);
        int w = tr.getWidth(word);
        c.drawText(tr, word, -w / 2, 0, p.text(), true);
        c.fillGradient(-w / 2, 11, w / 2, 12, p.accent(), p.accent2());
        m.pop();
    }

    private VortexLogo() {}
}
