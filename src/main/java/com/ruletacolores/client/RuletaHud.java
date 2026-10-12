package com.ruletacolores.client;

import com.ruletacolores.RuletaColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Estado y dibujo de la ruleta en pantalla (la ven todos los jugadores) + notificacion. */
final class RuletaHud {
    static final RuletaHud INSTANCE = new RuletaHud();

    // duraciones (segundos)
    private static final double ENTER = 1.15;
    private static final double PAUSE = 0.35;
    private static final double SPIN = 2.6;
    private static final double EXIT = 0.9;

    private static final float[] LAND_PITCH = {0.65f, 1.0f, 1.2f, 0.8f, 1.4f, 1.1f, 1.25f, 0.9f};
    private static final int NOTE_MAX_LINES = 3;

    private record Req(int color, String title, String text, boolean effects) {}

    private enum Phase { IDLE, ENTER, PAUSE, SPIN, LANDED, EXIT }

    private final Random rnd = new Random();
    private final ArrayDeque<Req> queue = new ArrayDeque<>();
    private final ColorFx fx = new ColorFx();

    private Phase phase = Phase.IDLE;
    private double t;
    private Req cur;
    private boolean fxOn;
    private float pAng = 270f;
    private float theta, enterRot, drawAng, lastDraw, vel, spinFrom, spinTotal, kick, flash;
    private int flashRgb;
    private int winner, target, lastSec;
    private long lastTickMs;
    private long lastNano;
    private float gcx, gcy, gR, gsw, gsh;
    private float clock, frameDt;

    private final int[] prev = new int[8];
    private final int[] shown = new int[8];
    private final float[] fade = new float[8];
    private final boolean[] recolored = new boolean[8];

    // notificacion
    private boolean nActive, nHidden, nLong;
    private String nTitle = "", nText = "";
    private int nRgb;
    private long nAt;

    private RuletaHud() {
        for (int k = 0; k < 8; k++) {
            shown[k] = prev[k] = (k + 2) % 8;
            fade[k] = 1f;
        }
    }

    static int sectorOf(int colorId) {
        return Math.floorMod(colorId - 2, 8);
    }

    void enqueue(int color, String title, String text, boolean effects) {
        queue.add(new Req(Math.floorMod(color, 8), title == null ? "" : title, text == null ? "" : text, effects));
    }

    void reset() {
        queue.clear();
        phase = Phase.IDLE;
        cur = null;
        nActive = false;
        fx.clear();
    }

    void hideNotification() {
        if (nActive) nHidden = true;
    }

    /** Tecla N: abre el texto completo de la notificacion actual. */
    void openFullText() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (nActive && !nHidden && System.currentTimeMillis() - nAt < noteMs()) {
            mc.setScreen(new FullTextScreen(nTitle, nText, nRgb));
        }
    }

    private static long noteMs() {
        return RuletaConfig.noteSeconds * 1000L;
    }

    /** Mensaje del globo de texto: chat + notificacion, sin ruleta. */
    void showBubble(int color, String title, String text) {
        Sfx.play("bubble", 1f, 0.9f);
        postMessage(Math.floorMod(color, 8), title == null ? "" : title, text == null ? "" : text);
    }

    // ------------------------------------------------------------------ logica

    private void begin(Req r) {
        cur = r;
        target = r.color();
        winner = sectorOf(target);
        fxOn = r.effects() && RuletaConfig.effects;
        pAng = RuletaConfig.pointerTop ? 0f : 270f;
        for (int k = 0; k < 8; k++) {
            shown[k] = prev[k] = (k + 2) % 8;
            fade[k] = 1f;
            recolored[k] = false;
        }
        theta = 0f;
        enterRot = 720f;
        drawAng = lastDraw = theta + enterRot;
        vel = 0f;
        kick = 0f;
        lastSec = Integer.MIN_VALUE;
        phase = Phase.ENTER;
        t = 0;
        Sfx.play("enter", 1f, 0.9f);
    }

    private void startSpin() {
        phase = Phase.SPIN;
        t = 0;
        float off = 22.5f + (rnd.nextFloat() - 0.5f) * 26f;
        float aw = 45f * winner + off;
        float need = (((pAng - aw - theta) % 360f) + 360f) % 360f;
        spinFrom = theta;
        spinTotal = need + 360f * (6 + rnd.nextInt(3));
        lastSec = (int) Math.floor((pAng - theta) / 45f);
        Sfx.play("spin_start", 1f, 0.9f);
    }

    private void land() {
        theta = spinFrom + spinTotal;
        vel = 0f;
        phase = Phase.LANDED;
        t = 0;
        Sfx.play("land", LAND_PITCH[target], 1f);
        if (RuletaConfig.flash) {
            flash = 1f;
            flashRgb = RuletaColor.byId(target).rgb;
        }
        if (fxOn) {
            fx.start(target);
            fx.burst(gcx, gcy, gR);
        }
        postMessage(cur.color(), cur.title(), cur.text());
    }

    private void postMessage(int colorId, String rawTitle, String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        RuletaColor c = RuletaColor.byId(colorId);
        int rgb = c.rgb;
        String title = rawTitle.toUpperCase(Locale.ROOT);

        if (RuletaConfig.chat && mc.inGameHud != null) {
            Style lineStyle = Style.EMPTY.withColor(rgb).withStrikethrough(true);
            Style textStyle = Style.EMPTY.withColor(rgb);
            Text line = Text.literal(" ".repeat(60)).setStyle(lineStyle);
            mc.inGameHud.getChatHud().addMessage(line);
            if (!title.isEmpty()) {
                mc.inGameHud.getChatHud().addMessage(Text.literal(title).setStyle(textStyle.withBold(true)));
            }
            if (!text.isEmpty()) {
                for (String s : text.split("\n")) {
                    if (!s.isBlank()) {
                        mc.inGameHud.getChatHud().addMessage(Text.literal(s).setStyle(textStyle));
                    }
                }
            }
            mc.inGameHud.getChatHud().addMessage(line);
        }

        if (RuletaConfig.notification) {
            nTitle = title.isEmpty() ? c.label.toUpperCase(Locale.ROOT) : title;
            nText = text;
            nRgb = rgb;
            nAt = System.currentTimeMillis();
            nHidden = false;
            nActive = true;
            nLong = false;
        }
    }

    private void update(float dt) {
        clock += dt;
        flash = Math.max(0f, flash - dt * 1.6f);
        kick *= (float) Math.exp(-dt * 14f);
        for (int k = 0; k < 8; k++) {
            if (fade[k] < 1f) fade[k] = Math.min(1f, fade[k] + dt / 0.25f);
        }
        enterRot = 0f;
        switch (phase) {
            case IDLE -> {
                Req r = queue.poll();
                if (r != null) begin(r);
            }
            case ENTER -> {
                t += dt;
                float u = (float) Math.min(1.0, t / ENTER);
                enterRot = 720f * (float) Math.pow(1f - u, 2.5);
                if (t >= ENTER) {
                    phase = Phase.PAUSE;
                    t = 0;
                    enterRot = 0f;
                }
            }
            case PAUSE -> {
                t += dt;
                if (t >= PAUSE) startSpin();
            }
            case SPIN -> {
                t += dt;
                float u = (float) Math.min(1.0, t / SPIN);
                float f = 1f - (float) Math.pow(1f - u, 4);
                theta = spinFrom + spinTotal * f;
                int sec = (int) Math.floor((pAng - theta) / 45f);
                if (sec != lastSec) {
                    lastSec = sec;
                    kick = 1f;
                    long now = System.currentTimeMillis();
                    if (now - lastTickMs >= 50) {
                        lastTickMs = now;
                        float sp = Math.min(1f, Math.abs(vel) / 2500f);
                        Sfx.play("tick", 0.8f + 0.5f * sp, 0.55f);
                    }
                }
                if (u >= 1f) land();
            }
            case LANDED -> {
                t += dt;
                for (int j = 1; j <= 7; j++) {
                    int k = (winner + j) % 8;
                    double at = 0.5 + (j - 1) * 0.25;
                    if (!recolored[k] && t >= at) {
                        recolored[k] = true;
                        prev[k] = shown[k];
                        shown[k] = target;
                        fade[k] = 0f;
                        Sfx.play("recolor", 0.75f + 0.08f * j, 0.8f);
                    }
                }
                if (t >= RuletaConfig.holdSeconds) {
                    phase = Phase.EXIT;
                    t = 0;
                    Sfx.play("exit", 1f, 0.9f);
                }
            }
            case EXIT -> {
                t += dt;
                if (t >= EXIT) {
                    phase = Phase.IDLE;
                    t = 0;
                    cur = null;
                    fx.clear();
                }
            }
        }
        drawAng = theta + enterRot;
        if (dt > 0f) {
            vel = vel * 0.5f + 0.5f * (drawAng - lastDraw) / dt;
        }
        lastDraw = drawAng;
    }

    // ------------------------------------------------------------------ dibujo

    void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        long now = System.nanoTime();
        float dt = lastNano == 0 ? 0f : Math.min(0.1f, (now - lastNano) / 1e9f);
        lastNano = now;
        frameDt = dt;

        gsw = mc.getWindow().getScaledWidth();
        gsh = mc.getWindow().getScaledHeight();
        gcx = gsw / 2f;
        gcy = gsh / 2f;
        float Rv = Math.min(gsh * 0.30f, gsw * 0.22f) * RuletaConfig.wheelSize / 100f;
        gR = Rv;

        update(dt);

        MatrixStack ms = ctx.getMatrices();
        Gfx.begin();
        if (phase != Phase.IDLE) {
            drawWheelScene(ms, Rv);
        }
        if (flash > 0.001f) {
            Gfx.fill(ms.peek().getPositionMatrix(), 0, 0, gsw, gsh, ((int) (flash * 0.35f * 255f) << 24) | (flashRgb & 0xFFFFFF));
        }
        drawNotification(ctx, mc);
        Gfx.end();
    }

    private static float easeOutCubic(float x) {
        float a = 1f - x;
        return 1f - a * a * a;
    }

    private void drawWheelScene(MatrixStack ms, float Rv) {
        float R = Rv / Gfx.TEX_RATIO;
        float alpha = 1f, offY = 0f, sx = 1f, sy = 1f, amp = 0f, shear = 0f;
        float wavePhase = clock * 16f;
        float fxK = 1f;

        switch (phase) {
            case ENTER -> {
                float u = (float) (t / ENTER);
                float start = -(gcy + Rv * 1.9f);
                float dip = Rv * 0.22f;
                final float split = 0.55f;
                if (u < split) {
                    // caida acelerada, estirada
                    float a = u / split;
                    offY = start + (dip - start) * a * a;
                    sy = 1f + 0.32f * a;
                    sx = 1f / (float) Math.sqrt(sy);
                } else {
                    // rebote: sube de nuevo hasta quedar en el medio, con squash elastico
                    float b = (u - split) / (1f - split);
                    offY = dip * (1f - easeOutCubic(b));
                    sy = 1f - 0.24f * (float) Math.exp(-b * 4.5f) * (float) Math.cos(b * 12f);
                    sx = 1f / (float) Math.sqrt(sy);
                }
                float s = 1f - u;
                amp = 0.09f * s;
                shear = 0.22f * (float) Math.sin(t * 15.0) * s;
                alpha = Math.min(1f, u * 5f);
            }
            case LANDED -> {
                float pun = Math.max(0f, 1f - (float) t * 3.5f);
                sx = sy = 1f + 0.07f * pun;
            }
            case EXIT -> {
                float u = (float) (t / EXIT);
                offY = u * u * (gsh - gcy + Rv * 1.9f);
                amp = 0.08f * u;
                sy = 1f + 0.32f * u * u;
                sx = 1f / (float) Math.sqrt(sy);
                shear = 0.2f * (float) Math.sin(t * 15.0) * u;
                fxK = Math.max(0f, 1f - u * 1.4f);
            }
            default -> {}
        }

        float cx = gcx, cy = gcy + offY;
        if (fxOn) fx.update(frameDt, gcx, gcy, Rv, gsw, gsh, fxK);

        Matrix4f scr = ms.peek().getPositionMatrix();

        // aura y rayos detras
        if (fxOn && (phase == Phase.LANDED || phase == Phase.EXIT)) {
            RuletaColor c = RuletaColor.byId(target);
            float r = ((c.rgb >> 16) & 255) / 255f, g = ((c.rgb >> 8) & 255) / 255f, b = (c.rgb & 255) / 255f;
            float pulse = 0.55f + 0.2f * (float) Math.sin(clock * 3f);
            Gfx.additive(true);
            Gfx.quad(scr, Gfx.GLOW, cx - Rv * 2.1f, cy - Rv * 2.1f, cx + Rv * 2.1f, cy + Rv * 2.1f, r, g, b, pulse * fxK);
            Gfx.additive(false);
        }
        if (fxOn) fx.drawBack(ms, cx, cy, Rv, fxK);

        // ruleta (con desenfoque de movimiento al girar / entrar)
        ms.push();
        ms.translate(cx, cy, 0f);
        ms.scale(sx, sy, 1f);
        float spd = Math.abs(vel);
        float spread = Math.min(40f, spd * 0.009f);
        int copies = 1;
        if (RuletaConfig.blur && (phase == Phase.SPIN || phase == Phase.ENTER)) {
            copies = 1 + Math.min(9, (int) (spd / 300f));
        }
        float dir = vel >= 0f ? 1f : -1f;
        for (int c = 0; c < copies; c++) {
            float ang = drawAng - dir * spread * c / copies;
            Gfx.wheel(ms, R, ang, prev, shown, fade, alpha / (c + 1), amp, wavePhase, shear);
        }
        ms.pop();

        // flecha dorada pegada a la ruleta (la punta entra en el anillo de gajos)
        ms.push();
        ms.translate(cx, cy, 0f);
        ms.scale(sx, sy, 1f);
        float ph = Rv * 0.46f, pw = ph * 0.8f;
        if (pAng == 0f) {
            ms.translate(0f, -Rv * 0.74f, 0f);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(kick * 20f));
        } else {
            ms.translate(-Rv * 0.74f, 0f, 0f);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90f - kick * 20f));
        }
        Matrix4f pm = ms.peek().getPositionMatrix();
        Gfx.quad(pm, Gfx.POINTER, -pw / 2f + 2f, -ph + 3f, pw / 2f + 2f, 3f, 0f, 0f, 0f, 0.30f * alpha);
        Gfx.quad(pm, Gfx.POINTER, -pw / 2f, -ph, pw / 2f, 0f, 1f, 1f, 1f, alpha);
        ms.pop();

        if (fxOn) fx.drawFront(ms, gsw, gsh, fxK);
    }

    private void drawNotification(DrawContext ctx, MinecraftClient mc) {
        if (!nActive) return;
        long age = System.currentTimeMillis() - nAt;
        if (age >= noteMs()) {
            nActive = false;
            return;
        }
        if (nHidden) return;

        TextRenderer tr = mc.textRenderer;
        int w = 236;
        List<OrderedText> lines = tr.wrapLines(Text.literal(nText), w - 16);
        nLong = lines.size() > NOTE_MAX_LINES;
        if (nLong) lines = lines.subList(0, NOTE_MAX_LINES);

        float slide = (float) Math.min(1.0, age / 350.0);
        slide = 1f - (float) Math.pow(1f - slide, 3);
        float x = 4f - (w + 8f) * (1f - slide);
        float y = 4f;
        int titleH = 24;
        int shownLines = Math.max(1, lines.size()) + (nLong ? 1 : 0);
        int bodyH = shownLines * 10 + 8;
        int footH = nLong ? 26 : 15;
        int total = titleH + bodyH + footH;
        int col = 0xFF000000 | nRgb;

        Matrix4f m = ctx.getMatrices().peek().getPositionMatrix();
        Gfx.fill(m, x, y, x + w, y + titleH, 0xE6000000);
        Gfx.fill(m, x, y + titleH, x + w, y + total, 0xD90F1420);
        Gfx.fill(m, x, y, x + 3, y + total, col);
        Gfx.quad(m, Gfx.ICON, x + w - 21, y + 4, x + w - 5, y + 20, 1f, 1f, 1f, 1f);

        Text title = Text.literal(nTitle).formatted(Formatting.BOLD);
        float s = Math.min(1.35f, (w - 40f) / Math.max(1, tr.getWidth(title)));
        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x + 9f, y + 12f - 4.5f * s, 0f);
        ms.scale(s, s, 1f);
        ctx.drawText(tr, title, 0, 0, col, false);
        ms.pop();

        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(tr, lines.get(i), (int) x + 9, (int) y + titleH + 5 + i * 10, col, true);
        }
        int fy = (int) y + titleH + bodyH + 3;
        if (nLong) {
            ctx.drawText(tr, Text.literal("..."), (int) x + 9, (int) y + titleH + 5 + lines.size() * 10, col, true);
            MutableText more = Text.literal("Presiona ");
            if (RuletaClient.readKey != null) more.append(RuletaClient.readKey.getBoundKeyLocalizedText());
            more.append(" para leer mas");
            ctx.drawText(tr, more, (int) x + 9, fy, 0xFFFFE066, false);
            fy += 11;
        }
        MutableText foot = Text.literal("Presiona ");
        if (RuletaClient.hideKey != null) foot.append(RuletaClient.hideKey.getBoundKeyLocalizedText());
        foot.append(" para ocultar este mensaje");
        ctx.drawText(tr, foot, (int) x + 9, fy, 0xFFFFFFFF, false);
    }
}
