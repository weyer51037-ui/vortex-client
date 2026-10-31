package dev.vortex.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

public class VortexTitleScreen extends Screen {
    private long opened;

    public VortexTitleScreen() {
        super(Text.literal("Vortex"));
    }

    @Override
    protected void init() {
        opened = System.currentTimeMillis();
        int bw = 220, bh = 24, x = width / 2 - bw / 2, y = height / 2 - 10, gap = 30;
        addDrawableChild(new VortexButton(x, y, bw, bh, "Singleplayer", () -> client.setScreen(new SelectWorldScreen(this))));
        addDrawableChild(new VortexButton(x, y + gap, bw, bh, "Multiplayer", () -> client.setScreen(new MultiplayerScreen(this))));
        addDrawableChild(new VortexButton(x, y + gap * 2, bw, bh, "Vortex Mods", () -> client.setScreen(new ModMenuScreen(this))));
        addDrawableChild(new VortexButton(x, y + gap * 3, bw, bh, "Cosmetics (coming soon)", () -> { }) {
            { active = false; }
        });
        int half = (bw - 6) / 2;
        addDrawableChild(new VortexButton(x, y + gap * 4, half, bh, "Settings", () -> client.setScreen(new OptionsScreen(this, client.options))));
        addDrawableChild(new VortexButton(x + half + 6, y + gap * 4, half, bh, "Quit", () -> client.scheduleStop()));
    }

    @Override
    public void renderBackground(DrawContext c, int mx, int my, float delta) {
        BackgroundRenderer.render(c, width, height);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        Theme.Palette p = Theme.current();
        double t = (System.currentTimeMillis() - opened) / 1000.0;
        float in = (float) Math.min(1.0, t / 0.6);
        in = 1 - (1 - in) * (1 - in);
        int logoY = height / 2 - 100 + (int) ((1 - in) * -14);
        VortexLogo.mark(c, width / 2, logoY, 70, t, p);
        VortexLogo.wordmark(c, textRenderer, width / 2, logoY + 46, 2.0f, p);
        c.drawCenteredTextWithShadow(textRenderer, "Performance Client  •  Beta", width / 2, logoY + 74, p.textDim());
        if (in < 1f) c.fill(0, 0, width, height, Theme.alpha(p.bg(), 1f - in));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
