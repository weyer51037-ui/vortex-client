package dev.vortex.ui;

import dev.vortex.config.ConfigManager;
import dev.vortex.module.Module;
import dev.vortex.module.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Per-mod configuration: toggles, choice cycling, drag sliders, reset to defaults. */
public class ModSettingsScreen extends Screen {
    private final Screen parent;
    private final Module mod;
    private Setting dragging;
    private int px, py, pw, ph;
    private static final int ROW = 26;

    public ModSettingsScreen(Screen parent, Module mod) {
        super(Text.literal(mod.name));
        this.parent = parent;
        this.mod = mod;
    }

    @Override
    protected void init() {
        pw = 320;
        ph = Math.min(height - 40, 90 + mod.settings.size() * ROW);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        addDrawableChild(new VortexButton(px + 12, py + ph - 30, 90, 20, "Back", this::close));
        addDrawableChild(new VortexButton(px + pw - 102, py + ph - 30, 90, 20, "Reset", () -> {
            mod.resetToDefaults();
            ConfigManager.markDirty();
        }));
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0xB0000000);
    }

    private int sliderX() { return px + pw - 160; }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        Theme.Palette p = Theme.current();
        renderBackground(c, mx, my, delta);
        Draw.shadow(c, px, py, pw, ph, 12);
        Draw.rounded(c, px, py, pw, ph, 12, p.bg());
        c.drawText(textRenderer, mod.name, px + 14, py + 12, p.text(), true);
        c.drawText(textRenderer, textRenderer.trimToWidth(mod.description, pw - 28), px + 14, py + 26, p.textDim(), false);
        Draw.rounded(c, px + 12, py + 40, pw - 24, ROW - 4, 6, p.panel());
        c.drawText(textRenderer, "Enabled", px + 20, py + 47, p.text(), false);
        Draw.toggle(c, px + pw - 52, py + 45, mod.enabled, mod.enabled ? 1f : 0f, p);
        int y = py + 40 + ROW;
        for (Setting s : mod.settings) {
            Draw.rounded(c, px + 12, y, pw - 24, ROW - 4, 6, p.panel());
            c.drawText(textRenderer, s.label, px + 20, y + 7, p.text(), false);
            switch (s.kind) {
                case BOOL -> Draw.toggle(c, px + pw - 52, y + 5, s.getBool(), s.getBool() ? 1f : 0f, p);
                case CHOICE -> c.drawText(textRenderer, s.getChoice() + " >", px + pw - 20 - textRenderer.getWidth(s.getChoice() + " >"), y + 7, p.accent(), false);
                default -> {
                    int sx = sliderX(), sw = 100;
                    double f = (value(s) - s.min) / (s.max - s.min);
                    Draw.rounded(c, sx, y + 9, sw, 4, 2, p.panelHi());
                    Draw.rounded(c, sx, y + 9, Math.max(4, (int) (sw * f)), 4, 2, p.accent());
                    Draw.rounded(c, sx + (int) (sw * f) - 3, y + 6, 6, 10, 3, 0xFFFFFFFF);
                    String v = s.kind == Setting.Kind.INT ? String.valueOf(s.getInt()) : String.format("%.1f", s.getFloat());
                    c.drawText(textRenderer, v, px + pw - 20 - textRenderer.getWidth(v), y + 7, p.textDim(), false);
                }
            }
            y += ROW;
        }
        super.render(c, mx, my, delta);
    }

    private static double value(Setting s) { return s.kind == Setting.Kind.INT ? s.getInt() : s.getFloat(); }

    private void applySlider(Setting s, double mx) {
        double f = Math.max(0, Math.min(1, (mx - sliderX()) / 100.0));
        double v = s.min + f * (s.max - s.min);
        if (s.kind == Setting.Kind.INT) s.setInt((int) Math.round(v)); else s.setFloat((float) v);
        ConfigManager.markDirty();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (Draw.inside(mx, my, px + 12, py + 40, pw - 24, ROW - 4)) { mod.toggle(); ConfigManager.markDirty(); return true; }
        int y = py + 40 + ROW;
        for (Setting s : mod.settings) {
            if (Draw.inside(mx, my, px + 12, y, pw - 24, ROW - 4)) {
                if (s.kind == Setting.Kind.BOOL || s.kind == Setting.Kind.CHOICE) { s.cycle(); ConfigManager.markDirty(); }
                else { dragging = s; applySlider(s, mx); }
                return true;
            }
            y += ROW;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) { applySlider(dragging, mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void close() {
        ConfigManager.save();
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
