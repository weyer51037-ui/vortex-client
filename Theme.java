package dev.vortex.ui;

import dev.vortex.config.ConfigManager;

import java.util.List;

/** Colour palettes. All colours are ARGB ints. */
public final class Theme {
    public record Palette(String name, int bg, int panel, int panelHi, int accent, int accent2, int text, int textDim) {}

    public static final List<Palette> ALL = List.of(
            new Palette("Vortex", 0xFF0E0E14, 0xFF16161F, 0xFF1F1F2C, 0xFF8B5CF6, 0xFF3B82F6, 0xFFF1F1F7, 0xFF8A8AA0),
            new Palette("Ember", 0xFF120D0D, 0xFF1C1414, 0xFF281C1C, 0xFFF97316, 0xFFEF4444, 0xFFF7F1EF, 0xFFA09088),
            new Palette("Mint", 0xFF0B1210, 0xFF121C19, 0xFF1A2924, 0xFF10B981, 0xFF06B6D4, 0xFFEFF7F4, 0xFF84A096),
            new Palette("Mono", 0xFF0C0C0C, 0xFF161616, 0xFF222222, 0xFFE5E5E5, 0xFF9CA3AF, 0xFFFFFFFF, 0xFF8A8A8A));

    public static Palette current() {
        for (Palette p : ALL) if (p.name.equals(ConfigManager.theme)) return p;
        return ALL.get(0);
    }

    public static int alpha(int argb, float a) {
        int na = Math.max(0, Math.min(255, (int) (((argb >>> 24) & 0xFF) * a)));
        return (na << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private Theme() {}
}
