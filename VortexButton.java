package dev.vortex.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** Rounded button with an eased hover glow. */
public class VortexButton extends ClickableWidget {
    private final Runnable action;
    private float hover;

    public VortexButton(int x, int y, int w, int h, String label, Runnable action) {
        super(x, y, w, h, Text.literal(label));
        this.action = action;
    }

    @Override
    protected void renderWidget(DrawContext c, int mx, int my, float delta) {
        Theme.Palette p = Theme.current();
        hover += ((isHovered() ? 1f : 0f) - hover) * 0.25f;
        Draw.shadow(c, getX(), getY(), getWidth(), getHeight(), 8);
        if (hover > 0.02f) Draw.outline(c, getX(), getY(), getWidth(), getHeight(), 8, Theme.alpha(p.accent(), hover * 0.9f));
        Draw.rounded(c, getX(), getY(), getWidth(), getHeight(), 8, Theme.mix(p.panel(), p.panelHi(), hover));
        var tr = MinecraftClient.getInstance().textRenderer;
        int col = active ? Theme.mix(p.text(), 0xFFFFFFFF, hover) : p.textDim();
        c.drawCenteredTextWithShadow(tr, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, col);
    }

    @Override
    public void onClick(double mx, double my) {
        action.run();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder b) {
        appendDefaultNarrations(b);
    }
}
