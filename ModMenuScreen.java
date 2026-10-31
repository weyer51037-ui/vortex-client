package dev.vortex.ui;

import dev.vortex.config.ConfigManager;
import dev.vortex.module.Category;
import dev.vortex.module.Module;
import dev.vortex.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Right-Shift menu. Sidebar: Favorites / categories / Appearance. Search filters by name+description.
 * Cards: toggle, favorite star, Settings. Beta: Appearance page configures theme, background, speed, custom image.
 */
public class ModMenuScreen extends Screen {
    private static final String FAV = "★ Favorites", ALL = "All Mods", APPEARANCE = "Appearance";
    private final Screen parent;
    private String tab = ALL;
    private TextFieldWidget search, bgPath;
    private int scroll;
    private final Map<Module, Float> anim = new HashMap<>();
    private int px, py, pw, ph; // panel
    private static final int SIDE = 110, CARD_H = 52, GAP = 6;

    public ModMenuScreen(Screen parent) {
        super(Text.literal("Vortex"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 40, 560);
        ph = Math.min(height - 40, 340);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        search = new TextFieldWidget(textRenderer, px + SIDE + 16, py + 12, pw - SIDE - 32, 18, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search mods..."));
        search.setDrawsBackground(false);
        search.setChangedListener(s -> scroll = 0);
        addDrawableChild(search);
        bgPath = new TextFieldWidget(textRenderer, px + SIDE + 16, py + 130, pw - SIDE - 32, 18, Text.literal("Image path"));
        bgPath.setMaxLength(500);
        bgPath.setText(ConfigManager.customBackgroundPath);
        bgPath.setPlaceholder(Text.literal("Full path to a .png file"));
        bgPath.setChangedListener(s -> {
            ConfigManager.customBackgroundPath = s;
            BackgroundRenderer.invalidate();
            ConfigManager.markDirty();
        });
        addDrawableChild(bgPath);
    }

    private List<String> tabs() {
        List<String> t = new ArrayList<>();
        t.add(ALL);
        t.add(FAV);
        for (Category c : Category.values()) t.add(c.label);
        t.add(APPEARANCE);
        return t;
    }

    private List<Module> visible() {
        String q = search.getText().toLowerCase().trim();
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            boolean ok;
            if (tab.equals(ALL)) ok = true;
            else if (tab.equals(FAV)) ok = m.favorite;
            else ok = m.category.label.equals(tab);
            if (!q.isEmpty() && !(m.name.toLowerCase().contains(q) || m.description.toLowerCase().contains(q))) ok = false;
            if (ok) out.add(m);
        }
        return out;
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0xA0000000);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        Theme.Palette p = Theme.current();
        renderBackground(c, mx, my, delta);
        Draw.shadow(c, px, py, pw, ph, 12);
        Draw.rounded(c, px, py, pw, ph, 12, p.bg());
        Draw.rounded(c, px, py, SIDE, ph, 12, p.panel());
        c.fill(px + SIDE - 12, py, px + SIDE, py + ph, p.panel());
        VortexLogo.mark(c, px + 22, py + 24, 26, System.currentTimeMillis() / 1000.0, p);
        c.drawText(textRenderer, "VORTEX", px + 42, py + 20, p.text(), true);

        int ty = py + 50;
        for (String t : tabs()) {
            boolean sel = t.equals(tab);
            boolean hov = Draw.inside(mx, my, px + 6, ty, SIDE - 12, 20);
            if (sel) Draw.rounded(c, px + 6, ty, SIDE - 12, 20, 6, Theme.alpha(p.accent(), 0.25f));
            else if (hov) Draw.rounded(c, px + 6, ty, SIDE - 12, 20, 6, p.panelHi());
            if (sel) c.fill(px + 6, ty + 4, px + 8, ty + 16, p.accent());
            c.drawText(textRenderer, t, px + 14, ty + 6, sel ? p.text() : p.textDim(), false);
            ty += 22;
        }

        boolean appearance = tab.equals(APPEARANCE);
        search.visible = !appearance;
        bgPath.visible = appearance;
        if (!appearance) {
            Draw.rounded(c, px + SIDE + 10, py + 8, pw - SIDE - 20, 24, 8, p.panel());
            super.render(c, mx, my, delta);
            renderCards(c, mx, my, p);
        } else {
            renderAppearance(c, mx, my, p);
            super.render(c, mx, my, delta);
        }
    }

    private int listTop() { return py + 42; }
    private int listH() { return ph - 50; }
    private int cardW() { return (pw - SIDE - 24 - GAP) / 2; }

    private void renderCards(DrawContext c, int mx, int my, Theme.Palette p) {
        List<Module> mods = visible();
        c.enableScissor(px + SIDE, listTop(), px + pw, listTop() + listH());
        int cw = cardW();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int x = px + SIDE + 12 + (i % 2) * (cw + GAP);
            int y = listTop() + (i / 2) * (CARD_H + GAP) - scroll;
            if (y + CARD_H < listTop() || y > listTop() + listH()) continue;
            boolean hov = Draw.inside(mx, my, x, y, cw, CARD_H) && my >= listTop() && my < listTop() + listH();
            Draw.rounded(c, x, y, cw, CARD_H, 8, hov ? p.panelHi() : p.panel());
            c.drawText(textRenderer, m.name, x + 8, y + 7, p.text(), true);
            c.drawText(textRenderer, m.favorite ? "★" : "☆", x + cw - 62, y + 7, m.favorite ? 0xFFFFD166 : p.textDim(), false);
            float a = anim.getOrDefault(m, m.enabled ? 1f : 0f);
            a += ((m.enabled ? 1f : 0f) - a) * 0.3f;
            anim.put(m, a);
            Draw.toggle(c, x + cw - 38, y + 7, m.enabled, a, p);
            String d = textRenderer.trimToWidth(m.description, cw - 16);
            c.drawText(textRenderer, d, x + 8, y + 22, p.textDim(), false);
            boolean sh = Draw.inside(mx, my, x + 8, y + CARD_H - 17, 52, 13);
            Draw.rounded(c, x + 8, y + CARD_H - 17, 52, 13, 5, sh ? p.accent() : Theme.alpha(p.accent(), 0.45f));
            c.drawText(textRenderer, "Settings", x + 13, y + CARD_H - 14, 0xFFFFFFFF, false);
        }
        c.disableScissor();
        if (mods.isEmpty()) {
            c.drawCenteredTextWithShadow(textRenderer, tab.equals(FAV) ? "Star a mod to pin it here." : "No mods match.",
                    px + SIDE + (pw - SIDE) / 2, py + ph / 2, p.textDim());
        }
    }

    private void renderAppearance(DrawContext c, int mx, int my, Theme.Palette p) {
        int x = px + SIDE + 16, y = py + 14;
        c.drawText(textRenderer, "Theme", x, y, p.text(), true);
        int bx = x;
        for (Theme.Palette t : Theme.ALL) {
            boolean sel = t.name().equals(ConfigManager.theme);
            if (sel) Draw.outline(c, bx, y + 14, 54, 22, 6, p.accent());
            Draw.rounded(c, bx, y + 14, 54, 22, 6, t.panel());
            c.fill(bx + 6, y + 20, bx + 18, y + 30, t.accent());
            c.fill(bx + 20, y + 20, bx + 32, y + 30, t.accent2());
            c.drawText(textRenderer, t.name(), bx + 4, y + 38, p.textDim(), false);
            bx += 60;
        }
        int y2 = y + 62;
        c.drawText(textRenderer, "Background", x, y2, p.text(), true);
        bx = x;
        int by = y2 + 14;
        for (String b : BackgroundRenderer.PRESETS) {
            boolean sel = b.equals(ConfigManager.background);
            int w = textRenderer.getWidth(b) + 14;
            if (bx + w > px + pw - 12) { bx = x; by += 20; }
            Draw.rounded(c, bx, by, w, 16, 6, sel ? p.accent() : (Draw.inside(mx, my, bx, by, w, 16) ? p.panelHi() : p.panel()));
            c.drawText(textRenderer, b, bx + 7, by + 4, sel ? 0xFFFFFFFF : p.textDim(), false);
            bx += w + 5;
        }
        c.drawText(textRenderer, "Custom image (PNG) path:", x, py + 118, p.textDim(), false);
        String status = ConfigManager.background.equals("Custom")
                ? (BackgroundRenderer.customLoaded() ? "Loaded." : "Not loaded yet - showing Aurora.") : "Pick 'Custom' above to use it.";
        c.drawText(textRenderer, status, x, py + 152, p.textDim(), false);
        c.drawText(textRenderer, "Animation speed", x, py + 172, p.text(), true);
        int sw = pw - SIDE - 40;
        Draw.rounded(c, x, py + 188, sw, 6, 3, p.panel());
        float f = (ConfigManager.backgroundSpeed - 0.1f) / 3.9f;
        Draw.rounded(c, x, py + 188, Math.max(6, (int) (sw * f)), 6, 3, p.accent());
        c.drawText(textRenderer, String.format("%.1fx", ConfigManager.backgroundSpeed), x + sw - 20, py + 198, p.textDim(), false);
        c.drawText(textRenderer, (ConfigManager.customTitleScreen ? "[x]" : "[ ]") + " Use Vortex title screen", x, py + 222, p.text(), false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int ty = py + 50;
        for (String t : tabs()) {
            if (Draw.inside(mx, my, px + 6, ty, SIDE - 12, 20)) { tab = t; scroll = 0; return true; }
            ty += 22;
        }
        if (tab.equals(APPEARANCE)) return clickAppearance(mx, my);
        List<Module> mods = visible();
        int cw = cardW();
        if (my < listTop() || my > listTop() + listH()) return false;
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int x = px + SIDE + 12 + (i % 2) * (cw + GAP);
            int y = listTop() + (i / 2) * (CARD_H + GAP) - scroll;
            if (!Draw.inside(mx, my, x, y, cw, CARD_H)) continue;
            if (Draw.inside(mx, my, x + cw - 40, y + 4, 34, 20)) { m.toggle(); ConfigManager.markDirty(); }
            else if (Draw.inside(mx, my, x + cw - 66, y + 4, 22, 20)) { m.favorite = !m.favorite; ConfigManager.markDirty(); }
            else if (Draw.inside(mx, my, x + 8, y + CARD_H - 17, 52, 13)) client.setScreen(new ModSettingsScreen(this, m));
            return true;
        }
        return false;
    }

    private boolean clickAppearance(double mx, double my) {
        int x = px + SIDE + 16, y = py + 14, bx = x;
        for (Theme.Palette t : Theme.ALL) {
            if (Draw.inside(mx, my, bx, y + 14, 54, 22)) { ConfigManager.theme = t.name(); ConfigManager.markDirty(); return true; }
            bx += 60;
        }
        int y2 = y + 62;
        bx = x;
        int by = y2 + 14;
        for (String b : BackgroundRenderer.PRESETS) {
            int w = textRenderer.getWidth(b) + 14;
            if (bx + w > px + pw - 12) { bx = x; by += 20; }
            if (Draw.inside(mx, my, bx, by, w, 16)) { ConfigManager.background = b; ConfigManager.markDirty(); return true; }
            bx += w + 5;
        }
        int sw = pw - SIDE - 40;
        if (Draw.inside(mx, my, x, py + 182, sw, 18)) { setSpeed(mx, x, sw); return true; }
        if (Draw.inside(mx, my, x, py + 220, 200, 14)) { ConfigManager.customTitleScreen = !ConfigManager.customTitleScreen; ConfigManager.markDirty(); return true; }
        return false;
    }

    private void setSpeed(double mx, int x, int sw) {
        float f = (float) Math.max(0, Math.min(1, (mx - x) / sw));
        ConfigManager.backgroundSpeed = Math.round((0.1f + f * 3.9f) * 10f) / 10f;
        ConfigManager.markDirty();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (tab.equals(APPEARANCE)) {
            int x = px + SIDE + 16, sw = pw - SIDE - 40;
            if (Draw.inside(mx, my, x - 10, py + 182, sw + 20, 18)) { setSpeed(mx, x, sw); return true; }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int rows = (visible().size() + 1) / 2;
        int max = Math.max(0, rows * (CARD_H + GAP) - listH());
        scroll = (int) Math.max(0, Math.min(max, scroll - v * 20));
        return true;
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
