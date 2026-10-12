package com.ruletacolores.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Texto completo de la notificacion (se abre con N). */
public class FullTextScreen extends Screen {
    private final String title;
    private final String text;
    private final int rgb;
    private int scroll = 0;
    private List<OrderedText> lines = List.of();
    private int px, py, pw, ph, visible;

    public FullTextScreen(String title, String text, int rgb) {
        super(Text.literal(title));
        this.title = title;
        this.text = text;
        this.rgb = rgb;
    }

    @Override
    protected void init() {
        pw = Math.min(this.width - 40, 380);
        ph = this.height - 60;
        px = (this.width - pw) / 2;
        py = 30;
        lines = this.textRenderer.wrapLines(Text.literal(text), pw - 28);
        visible = Math.max(1, (ph - 64) / 11);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("CERRAR"), b -> this.close())
                .dimensions(this.width / 2 - 50, py + ph - 26, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        int col = 0xFF000000 | rgb;
        ctx.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, col);
        ctx.fill(px, py, px + pw, py + ph, 0xF0101420);
        ctx.fill(px, py, px + pw, py + 28, 0xFF000000);
        ctx.drawText(this.textRenderer, Text.literal(title).formatted(Formatting.BOLD), px + 10, py + 10, col, false);

        int maxScroll = Math.max(0, lines.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        for (int i = 0; i < visible && scroll + i < lines.size(); i++) {
            ctx.drawText(this.textRenderer, lines.get(scroll + i), px + 12, py + 36 + i * 11, col, true);
        }
        if (maxScroll > 0) {
            int trackH = ph - 64;
            int barH = Math.max(12, trackH * visible / lines.size());
            int barY = py + 36 + (trackH - barH) * scroll / maxScroll;
            ctx.fill(px + pw - 7, py + 36, px + pw - 4, py + 36 + trackH, 0x55FFFFFF);
            ctx.fill(px + pw - 7, barY, px + pw - 4, barY + barH, col);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) Math.signum(verticalAmount) * 2;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            scroll++;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            scroll--;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
