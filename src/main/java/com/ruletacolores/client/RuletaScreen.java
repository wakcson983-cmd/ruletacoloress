package com.ruletacolores.client;

import com.ruletacolores.BubbleRequestPayload;
import com.ruletacolores.RuletaColor;
import com.ruletacolores.SpinRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Menu de la ruleta (fondo transparente): color, titulo y texto del chat. */
public class RuletaScreen extends Screen {
    private static final int[] NAT = new int[8];
    private static final int[] NAT_PREV = new int[8];
    private static final float[] ONE = new float[8];

    static {
        for (int k = 0; k < 8; k++) {
            NAT[k] = NAT_PREV[k] = (k + 2) % 8;
            ONE[k] = 1f;
        }
    }

    private static final class Box {
        final boolean multi;
        final int max;
        final String hint;
        String text = "";
        int x, y, w, h;

        Box(boolean multi, int max, String hint) {
            this.multi = multi;
            this.max = max;
            this.hint = hint;
        }

        void set(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private int selected = 0;
    private float previewAngle = -22.5f - 45f * RuletaHud.sectorOf(0);
    private long lastNano = 0L;
    private final boolean bubble;
    private boolean effectsOn = RuletaConfig.effects;
    private final Box titleBox;
    private final Box textBox;
    private Box active;
    private ButtonWidget effectsBtn;

    private int wcx, wcy, rx, rw, swY;
    private final float wR = 88f;

    /** bubble = false: menu de la ruleta; bubble = true: menu del globo de texto. */
    public RuletaScreen(boolean bubble) {
        super(Text.literal(bubble ? "Globo de texto" : "Ruleta"));
        this.bubble = bubble;
        this.titleBox = new Box(false, 40, bubble ? "TITULO (OPCIONAL)" : "TITULO DEL TEXTO");
        this.textBox = new Box(true, bubble ? 400 : 240, bubble ? "MENSAJE PARA EL CHAT" : "TEXTO EN EL CHAT");
        this.active = titleBox;
    }

    @Override
    protected void init() {
        int total = 440;
        int left = (this.width - total) / 2;
        int top = Math.max(8, (this.height - 236) / 2);
        wcx = left + 95;
        wcy = top + 112;
        rx = left + 206;
        rw = 234;
        swY = top + 24;
        titleBox.set(rx, top + 56, rw, 22);
        textBox.set(rx, top + 86, rw, 84);
        this.addDrawableChild(ButtonWidget.builder(Text.literal(bubble ? "ENVIAR MENSAJE" : "GIRAR RULETA"), b -> spin())
                .dimensions(rx, top + 178, rw, 22).build());
        int half = (rw - 4) / 2;
        if (!bubble) {
            effectsBtn = ButtonWidget.builder(effectsLabel(), b -> {
                effectsOn = !effectsOn;
                b.setMessage(effectsLabel());
            }).dimensions(rx, top + 204, half, 20).build();
            this.addDrawableChild(effectsBtn);
            this.addDrawableChild(ButtonWidget.builder(Text.literal("CONFIGURACION"), b -> this.client.setScreen(new ConfigScreen(this)))
                    .dimensions(rx + half + 4, top + 204, half, 20).build());
        } else {
            this.addDrawableChild(ButtonWidget.builder(Text.literal("CONFIGURACION"), b -> this.client.setScreen(new ConfigScreen(this)))
                    .dimensions(rx, top + 204, rw, 20).build());
        }
    }

    private Text effectsLabel() {
        return Text.literal("Efectos: " + (effectsOn ? "SI" : "NO"));
    }

    private void spin() {
        if (bubble) {
            if (textBox.text.isBlank()) return;
            ClientPlayNetworking.send(new BubbleRequestPayload(selected, titleBox.text.trim(), textBox.text.trim()));
        } else {
            ClientPlayNetworking.send(new SpinRequestPayload(selected, titleBox.text.trim(), textBox.text.trim(), effectsOn));
        }
        Sfx.play("select", 1.2f, 0.8f);
        this.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Fondo transparente: no se dibuja nada (sin oscurecer ni difuminar). */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0.016f : Math.min(0.1f, (now - lastNano) / 1e9f);
        lastNano = now;

        float tgt = -(45f * RuletaHud.sectorOf(selected) + 22.5f);
        float diff = (((tgt - previewAngle) % 360f) + 540f) % 360f - 180f;
        previewAngle += diff * (1f - (float) Math.exp(-dt * 9f));

        // --- ruleta + flecha (arriba, como en la imagen del menu)
        MatrixStack ms = ctx.getMatrices();
        Gfx.begin();
        ms.push();
        ms.translate(wcx, wcy, 0f);
        if (bubble) {
            Matrix4f bm = ms.peek().getPositionMatrix();
            float bob = (float) Math.sin(System.currentTimeMillis() / 400.0) * 3f;
            Gfx.quad(bm, Gfx.GLOBO, -wR, -wR + bob, wR, wR + bob, 1f, 1f, 1f, 1f);
        } else {
            float R = wR / Gfx.TEX_RATIO;
            Gfx.wheel(ms, R, previewAngle, NAT_PREV, NAT, ONE, 1f, 0f, 0f, 0f);
            ms.push();
            ms.translate(0f, -wR * 0.74f, 0f);
            float ph = wR * 0.46f, pw = ph * 0.8f;
            Matrix4f pm = ms.peek().getPositionMatrix();
            Gfx.quad(pm, Gfx.POINTER, -pw / 2f + 2f, -ph + 3f, pw / 2f + 2f, 3f, 0f, 0f, 0f, 0.3f);
            Gfx.quad(pm, Gfx.POINTER, -pw / 2f, -ph, pw / 2f, 0f, 1f, 1f, 1f, 1f);
            ms.pop();
        }
        ms.pop();
        Gfx.end();

        // --- titulo
        ms.push();
        ms.translate(rx, swY - 20f, 0f);
        ms.scale(1.4f, 1.4f, 1f);
        ctx.drawText(this.textRenderer, Text.literal(bubble ? "COLOR DEL MENSAJE" : "COLOR DE LA RULETA").formatted(Formatting.BOLD), 0, 0, 0xFFFFFFFF, true);
        ms.pop();

        // --- cuadros de color
        for (int i = 0; i < 8; i++) {
            int x = rx + i * 29;
            boolean sel = i == selected;
            ctx.fill(x - 2, swY - 2, x + 22, swY + 22, sel ? 0xFFFFFFFF : 0xFF000000);
            ctx.fill(x, swY, x + 20, swY + 20, 0xFF000000 | RuletaColor.byId(i).rgb);
        }

        drawBox(ctx, titleBox);
        drawBox(ctx, textBox);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawBox(DrawContext ctx, Box b) {
        boolean on = active == b;
        int border = on ? 0xFFFFFFFF : 0xFF555555;
        ctx.fill(b.x, b.y, b.x + b.w, b.y + b.h, 0xFF000000);
        ctx.fill(b.x, b.y, b.x + b.w, b.y + 1, border);
        ctx.fill(b.x, b.y + b.h - 1, b.x + b.w, b.y + b.h, border);
        ctx.fill(b.x, b.y, b.x + 1, b.y + b.h, border);
        ctx.fill(b.x + b.w - 1, b.y, b.x + b.w, b.y + b.h, border);

        boolean blink = on && (System.currentTimeMillis() / 500L) % 2L == 0L;
        if (b.text.isEmpty() && !on) {
            ctx.drawText(this.textRenderer, Text.literal(b.hint), b.x + 4, b.y + 7, 0xFFAAAAAA, false);
            return;
        }
        if (b.text.isEmpty() && on) {
            ctx.drawText(this.textRenderer, Text.literal(b.hint), b.x + 4, b.y + (b.multi ? 4 : 7), 0xFF666666, false);
            if (blink) ctx.fill(b.x + 4, b.y + (b.multi ? 3 : 6), b.x + 5, b.y + (b.multi ? 13 : 16), 0xFFFFFFFF);
            return;
        }
        String shownText = b.text + (blink ? "_" : "");
        if (!b.multi) {
            // una sola linea: muestra el final si no cabe
            String s = this.textRenderer.trimToWidth(shownText, b.w - 8);
            if (!s.equals(shownText)) {
                s = this.textRenderer.trimToWidth(shownText, b.w - 8, true);
            }
            ctx.drawText(this.textRenderer, Text.literal(s), b.x + 4, b.y + 7, 0xFFFFFFFF, true);
        } else {
            List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(shownText), b.w - 8);
            int visible = Math.max(1, (b.h - 6) / 10);
            int start = Math.max(0, lines.size() - visible);
            for (int i = start; i < lines.size(); i++) {
                ctx.drawText(this.textRenderer, lines.get(i), b.x + 4, b.y + 4 + (i - start) * 10, 0xFFFFFFFF, true);
            }
        }
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < 8; i++) {
                int x = rx + i * 29;
                if (mouseX >= x && mouseX < x + 20 && mouseY >= swY && mouseY < swY + 20) {
                    selected = i;
                    Sfx.play("select", 0.8f + i * 0.08f, 0.6f);
                    return true;
                }
            }
            if (titleBox.contains(mouseX, mouseY)) {
                active = titleBox;
                return true;
            }
            if (textBox.contains(mouseX, mouseY)) {
                active = textBox;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (active != null && chr >= 32 && chr != 127) {
            if (active.text.length() < active.max) active.text += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && active != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!active.text.isEmpty()) active.text = active.text.substring(0, active.text.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (active.multi) {
                    long nl = active.text.chars().filter(c -> c == '\n').count();
                    if (active.text.length() < active.max && nl < 5) active.text += "\n";
                } else {
                    active = textBox;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                active = active == titleBox ? textBox : titleBox;
                return true;
            }
            if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_V && this.client != null) {
                String clip = this.client.keyboard.getClipboard();
                if (clip != null) {
                    StringBuilder sb = new StringBuilder(active.text);
                    for (char c : clip.toCharArray()) {
                        if (sb.length() >= active.max) break;
                        if (c >= 32 && c != 127) sb.append(c);
                        else if (c == '\n' && active.multi) sb.append(c);
                    }
                    active.text = sb.toString();
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
