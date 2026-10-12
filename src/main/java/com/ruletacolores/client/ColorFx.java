package com.ruletacolores.client;

import com.ruletacolores.RuletaColor;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Animacion propia de cada color (particulas, rayos, vinetas...). Coordenadas en pixeles de GUI. */
final class ColorFx {
    private static final class P {
        float x, y, vx, vy, ax, ay, age, life, size, rot, vrot, grow;
        float r, g, b, a0 = 1f, aux, aux2, ox, oy;
        int kind; // 0 normal, 1 vaiven, 2 orbita, 3 lluvia (linea), 4 anillo
        boolean add;
        Identifier tex;
    }

    private static final class Bolt {
        float[] xs, ys;
        float age, life;
    }

    private final Random rnd = new Random();
    private final List<P> parts = new ArrayList<>();
    private final List<Bolt> bolts = new ArrayList<>();
    private int color = -1;
    private float time;
    private float acc, acc2, boltTimer, ringTimer;
    private float cr, cg, cb;
    private int rgb;

    void clear() {
        parts.clear();
        bolts.clear();
        color = -1;
    }

    void start(int c) {
        parts.clear();
        bolts.clear();
        color = c;
        time = 0f;
        acc = acc2 = boltTimer = ringTimer = 0f;
        rgb = RuletaColor.byId(c).rgb;
        cr = ((rgb >> 16) & 255) / 255f;
        cg = ((rgb >> 8) & 255) / 255f;
        cb = (rgb & 255) / 255f;
    }

    private float rr(float a, float b) {
        return a + rnd.nextFloat() * (b - a);
    }

    private P mk(Identifier tex, float x, float y, float vx, float vy, float size, float life, float r, float g, float b) {
        P p = new P();
        p.tex = tex;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.size = size;
        p.life = life;
        p.r = r;
        p.g = g;
        p.b = b;
        p.rot = rnd.nextFloat() * 6.28f;
        if (parts.size() < 600) parts.add(p);
        return p;
    }

    private int count(float rate, float dt) {
        acc += rate * dt;
        int n = (int) acc;
        acc -= n;
        return n;
    }

    private int count2(float rate, float dt) {
        acc2 += rate * dt;
        int n = (int) acc2;
        acc2 -= n;
        return n;
    }

    // ------------------------------------------------------------ explosion al caer

    void burst(float cx, float cy, float R) {
        if (color < 0) return;
        // onda expansiva comun
        P ring = mk(Gfx.RING, cx, cy, 0, 0, R * 1.2f, 1.0f, cr, cg, cb);
        ring.kind = 4;
        ring.grow = R * 3.2f;
        ring.add = true;
        P ring2 = mk(Gfx.RING, cx, cy, 0, 0, R * 0.8f, 1.3f, 1f, 1f, 1f);
        ring2.kind = 4;
        ring2.grow = R * 2.4f;
        ring2.add = true;
        int n = 34;
        for (int i = 0; i < n; i++) {
            double a = rnd.nextFloat() * Math.PI * 2;
            float sp = rr(60f, 220f);
            float vx = (float) Math.cos(a) * sp;
            float vy = (float) Math.sin(a) * sp;
            switch (color) {
                case 0 -> { // rojo: chispas
                    P p = mk(Gfx.SPARK, cx, cy, vx, vy, rr(8, 16), rr(0.6f, 1.2f), 1f, rr(0.1f, 0.4f), 0.1f);
                    p.add = true;
                    p.ay = 120f;
                }
                case 1 -> { // rosa: corazones
                    P p = mk(Gfx.HEART, cx, cy, vx * 0.6f, vy * 0.6f - 30f, rr(10, 20), rr(1.2f, 2.2f), 1f, rr(0.3f, 0.6f), 0.85f);
                    p.ay = -25f;
                    p.vrot = rr(-1f, 1f);
                }
                case 2 -> { // morado: destellos que se alejan en espiral
                    P p = mk(Gfx.SPARK, cx, cy, rr(2f, 4f), rr(60f, 200f), rr(8, 18), rr(1.0f, 2.0f), 0.75f, 0.35f, 1f);
                    p.kind = 2;
                    p.aux = (float) a;
                    p.aux2 = R * 0.9f;
                    p.ox = cx;
                    p.oy = cy;
                    p.vrot = 3f;
                    p.add = true;
                }
                case 3 -> { // azul: gotas que saltan
                    P p = mk(Gfx.GLOW, cx, cy, vx * 0.7f, vy * 0.7f - 60f, rr(6, 12), rr(0.8f, 1.5f), 0.3f, 0.6f, 1f);
                    p.ay = 260f;
                    p.add = true;
                }
                case 4 -> { // celeste: copos
                    P p = mk(Gfx.FLAKE, cx, cy, vx * 0.4f, vy * 0.4f, rr(10, 20), rr(1.6f, 2.6f), 0.8f, 0.95f, 1f);
                    p.vrot = rr(-2f, 2f);
                    p.ay = 20f;
                }
                case 5 -> { // verde: hojas
                    P p = mk(Gfx.LEAF, cx, cy, vx * 0.5f, vy * 0.5f - 20f, rr(12, 22), rr(1.6f, 2.6f), 0.2f, rr(0.8f, 1f), 0.15f);
                    p.vrot = rr(-4f, 4f);
                    p.ay = 40f;
                }
                case 6 -> { // amarillo: estrellas doradas
                    P p = mk(Gfx.SPARK, cx, cy, vx * 1.3f, vy * 1.3f, rr(10, 22), rr(0.9f, 1.8f), 1f, rr(0.85f, 1f), 0.2f);
                    p.ay = 140f;
                    p.vrot = rr(-3f, 3f);
                    p.add = true;
                }
                default -> { // naranja: brasas
                    P p = mk(Gfx.SPARK, cx, cy, vx * 0.8f, vy * 0.8f - 40f, rr(8, 16), rr(1.0f, 2.0f), 1f, rr(0.3f, 0.55f), 0.05f);
                    p.ay = -60f;
                    p.add = true;
                }
            }
        }
        if (color == 0) {
            for (int i = 0; i < 4; i++) addBolt(cx, cy, R, cx * 2f, cy * 2f);
        }
    }

    private void addBolt(float cx, float cy, float R, float sw, float sh) {
        Bolt b = new Bolt();
        int seg = 9;
        b.xs = new float[seg + 1];
        b.ys = new float[seg + 1];
        float sx = rr(0f, sw);
        float sy = -10f;
        double ang = rnd.nextFloat() * Math.PI * 2;
        float ex = cx + (float) Math.cos(ang) * R * rr(0.9f, 1.3f);
        float ey = cy + (float) Math.sin(ang) * R * rr(0.9f, 1.3f);
        if (rnd.nextBoolean()) { // tambien desde los lados
            sx = rnd.nextBoolean() ? -10f : sw + 10f;
            sy = rr(0f, sh * 0.6f);
        }
        for (int i = 0; i <= seg; i++) {
            float t = i / (float) seg;
            float jit = (i == 0 || i == seg) ? 0f : rr(-26f, 26f);
            b.xs[i] = sx + (ex - sx) * t + jit;
            b.ys[i] = sy + (ey - sy) * t + (i == 0 || i == seg ? 0f : rr(-12f, 12f));
        }
        b.life = rr(0.14f, 0.28f);
        if (bolts.size() < 14) bolts.add(b);
    }

    // ------------------------------------------------------------ actualizar

    void update(float dt, float cx, float cy, float R, float sw, float sh, float k) {
        time += dt;
        for (Iterator<P> it = parts.iterator(); it.hasNext(); ) {
            P p = it.next();
            p.age += dt;
            if (p.age >= p.life) {
                it.remove();
                continue;
            }
            switch (p.kind) {
                case 2 -> {
                    p.aux += p.vx * dt;
                    p.aux2 += p.vy * dt;
                    p.x = p.ox + (float) Math.cos(p.aux) * p.aux2;
                    p.y = p.oy + (float) Math.sin(p.aux) * p.aux2;
                    p.rot += p.vrot * dt;
                }
                default -> {
                    if (p.kind == 1) p.x += (float) Math.sin(p.age * 3f + p.aux) * p.aux2 * dt;
                    p.vx += p.ax * dt;
                    p.vy += p.ay * dt;
                    p.x += p.vx * dt;
                    p.y += p.vy * dt;
                    p.rot += p.vrot * dt;
                    p.size += p.grow * dt;
                }
            }
        }
        for (Iterator<Bolt> it = bolts.iterator(); it.hasNext(); ) {
            Bolt b = it.next();
            b.age += dt;
            if (b.age >= b.life) it.remove();
        }
        if (color < 0 || k <= 0.02f) return;

        switch (color) {
            case 0 -> { // ROJO: rayos y brasas
                boltTimer -= dt;
                if (boltTimer <= 0f) {
                    boltTimer = rr(0.10f, 0.35f);
                    addBolt(cx, cy, R, sw, sh);
                }
                for (int i = count(26f * k, dt); i > 0; i--) {
                    double a = rnd.nextFloat() * Math.PI * 2;
                    float rad = R * rr(0.95f, 1.25f);
                    P p = mk(Gfx.SPARK, cx + (float) Math.cos(a) * rad, cy + (float) Math.sin(a) * rad,
                            (float) Math.cos(a) * rr(20f, 80f), (float) Math.sin(a) * rr(20f, 80f) - 30f,
                            rr(6f, 13f), rr(0.5f, 1.0f), 1f, rr(0.1f, 0.35f), 0.1f);
                    p.add = true;
                }
            }
            case 1 -> { // ROSA: corazones que suben
                for (int i = count(8f * k, dt); i > 0; i--) {
                    P p = mk(Gfx.HEART, rr(0f, sw), sh + 10f, 0f, rr(-70f, -40f), rr(10f, 20f), rr(3.5f, 5.5f), 1f, rr(0.25f, 0.55f), 0.85f);
                    p.kind = 1;
                    p.aux = rr(0f, 6f);
                    p.aux2 = rr(15f, 40f);
                    p.vrot = rr(-0.8f, 0.8f);
                    p.a0 = 0.9f;
                }
                for (int i = count2(10f * k, dt); i > 0; i--) {
                    double a = rnd.nextFloat() * Math.PI * 2;
                    P p = mk(Gfx.SPARK, cx + (float) Math.cos(a) * R * rr(0.8f, 1.2f), cy + (float) Math.sin(a) * R * rr(0.8f, 1.2f),
                            0f, rr(-25f, -10f), rr(5f, 10f), rr(0.8f, 1.4f), 1f, 0.6f, 0.9f);
                    p.add = true;
                }
            }
            case 2 -> { // MORADO: destellos orbitando
                for (int i = count(22f * k, dt); i > 0; i--) {
                    P p = mk(Gfx.SPARK, cx, cy, rr(1.2f, 2.6f), rr(8f, 28f), rr(7f, 15f), rr(1.6f, 2.8f), 0.75f, rr(0.25f, 0.5f), 1f);
                    p.kind = 2;
                    p.aux = rr(0f, 6.28f);
                    p.aux2 = R * rr(0.95f, 1.15f);
                    p.ox = cx;
                    p.oy = cy;
                    p.vrot = rr(-3f, 3f);
                    p.add = true;
                }
            }
            case 3 -> { // AZUL: lluvia y ondas
                for (int i = count(95f * k, dt); i > 0; i--) {
                    P p = mk(Gfx.GLOW, rr(-20f, sw + 20f), -10f, rr(-20f, -5f), rr(380f, 520f), rr(1.2f, 2.2f), rr(1.1f, 1.8f), 0.45f, 0.7f, 1f);
                    p.kind = 3;
                    p.a0 = 0.75f;
                }
                ringTimer -= dt;
                if (ringTimer <= 0f) {
                    ringTimer = 0.55f;
                    P p = mk(Gfx.RING, cx, cy, 0, 0, R * 1.0f, 1.6f, 0.3f, 0.6f, 1f);
                    p.kind = 4;
                    p.grow = R * 1.6f;
                    p.add = true;
                }
            }
            case 4 -> { // CELESTE: nieve
                for (int i = count(30f * k, dt); i > 0; i--) {
                    P p = mk(Gfx.FLAKE, rr(-10f, sw + 10f), -12f, rr(-10f, 10f), rr(30f, 70f), rr(6f, 15f), rr(5f, 9f), 0.85f, 0.97f, 1f);
                    p.kind = 1;
                    p.aux = rr(0f, 6f);
                    p.aux2 = rr(10f, 35f);
                    p.vrot = rr(-1.5f, 1.5f);
                    p.a0 = 0.9f;
                }
            }
            case 5 -> { // VERDE: hojas llevadas por el viento
                for (int i = count(15f * k, dt); i > 0; i--) {
                    boolean left = rnd.nextBoolean();
                    P p = mk(Gfx.LEAF, left ? -12f : sw + 12f, rr(sh * 0.2f, sh), left ? rr(60f, 130f) : rr(-130f, -60f), rr(-60f, -10f),
                            rr(10f, 20f), rr(3f, 5f), 0.2f, rr(0.75f, 1f), 0.15f);
                    p.kind = 1;
                    p.aux = rr(0f, 6f);
                    p.aux2 = rr(20f, 60f);
                    p.vrot = rr(-4f, 4f);
                }
                for (int i = count2(12f * k, dt); i > 0; i--) {
                    P p = mk(Gfx.SPARK, cx + rr(-R, R), cy + rr(-R, R), 0f, rr(-30f, -10f), rr(5f, 9f), rr(0.9f, 1.5f), 0.4f, 1f, 0.3f);
                    p.add = true;
                }
            }
            case 6 -> { // AMARILLO: estrellas doradas
                for (int i = count(20f * k, dt); i > 0; i--) {
                    double a = rnd.nextFloat() * Math.PI * 2;
                    float sp = rr(40f, 140f);
                    P p = mk(Gfx.SPARK, cx + (float) Math.cos(a) * R * 0.9f, cy + (float) Math.sin(a) * R * 0.9f,
                            (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, rr(8f, 20f), rr(0.9f, 1.7f), 1f, rr(0.85f, 1f), 0.25f);
                    p.ay = 60f;
                    p.vrot = rr(-3f, 3f);
                    p.add = true;
                }
            }
            default -> { // NARANJA: brasas subiendo desde abajo
                for (int i = count(44f * k, dt); i > 0; i--) {
                    P p = mk(Gfx.SPARK, rr(0f, sw), sh + 6f, rr(-15f, 15f), rr(-140f, -60f), rr(5f, 12f), rr(2.2f, 4.2f), 1f, rr(0.3f, 0.6f), 0.05f);
                    p.kind = 1;
                    p.aux = rr(0f, 6f);
                    p.aux2 = rr(10f, 30f);
                    p.add = true;
                }
            }
        }
    }

    // ------------------------------------------------------------ dibujar

    /** Cosas que van DETRAS de la ruleta (rayos de sol). */
    void drawBack(MatrixStack ms, float cx, float cy, float R, float k) {
        if (color != 6 || k <= 0.02f) return;
        Matrix4f m = ms.peek().getPositionMatrix();
        int rays = 14;
        float len = R * 3.2f;
        int cCenter = ((int) (0.55f * k * 255f) << 24) | 0xFFE040;
        int cEdge = 0x00FFE040;
        for (int i = 0; i < rays; i++) {
            double a = time * 0.35 + i * (Math.PI * 2 / rays);
            double w = Math.PI / rays * 0.55;
            Gfx.triGrad(m, cx, cy,
                    cx + (float) Math.cos(a - w) * len, cy + (float) Math.sin(a - w) * len,
                    cx + (float) Math.cos(a + w) * len, cy + (float) Math.sin(a + w) * len,
                    cCenter, cEdge);
        }
    }

    /** Cosas que van DELANTE (particulas, rayos, vineta). */
    void drawFront(MatrixStack ms, float sw, float sh, float k) {
        if (color < 0) return;
        Matrix4f m = ms.peek().getPositionMatrix();
        float thick = Math.min(sw, sh) * 0.22f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 5f);
        switch (color) {
            case 0 -> Gfx.vignette(m, sw, sh, 0xFF0000, (0.16f + 0.26f * pulse * pulse) * k, thick * 1.2f);
            case 1 -> Gfx.vignette(m, sw, sh, 0xFF40C8, 0.20f * k, thick);
            case 2 -> Gfx.vignette(m, sw, sh, 0x8030FF, (0.16f + 0.12f * pulse) * k, thick);
            case 3 -> Gfx.gradV(m, 0, 0, sw, sh * 0.45f, ((int) (0.28f * k * 255f) << 24) | 0x1040FF, 0x001040FF);
            case 4 -> Gfx.vignette(m, sw, sh, 0xBFEFFF, 0.28f * k, thick);
            case 5 -> Gfx.vignette(m, sw, sh, 0x20E020, 0.18f * k, thick);
            case 6 -> Gfx.vignette(m, sw, sh, 0xFFE020, 0.18f * k, thick);
            default -> Gfx.gradV(m, 0, sh * 0.5f, sw, sh, 0x00FF5000, ((int) ((0.30f + 0.12f * pulse) * k * 255f) << 24) | 0xFF5000);
        }

        for (Bolt b : bolts) {
            float fl = 0.55f + 0.45f * rnd.nextFloat();
            float fade = 1f - b.age / b.life;
            float a = fl * fade * k;
            int glow = ((int) (a * 0.45f * 255f) << 24) | 0xFF2020;
            int core = ((int) (a * 255f) << 24) | 0xFFD0D0;
            for (int i = 0; i + 1 < b.xs.length; i++) {
                Gfx.line(m, b.xs[i], b.ys[i], b.xs[i + 1], b.ys[i + 1], 7f, glow);
                Gfx.line(m, b.xs[i], b.ys[i], b.xs[i + 1], b.ys[i + 1], 2f, core);
            }
        }

        for (int pass = 0; pass < 2; pass++) {
            boolean wantAdd = pass == 1;
            Gfx.additive(wantAdd);
            for (P p : parts) {
                if (p.add != wantAdd) continue;
                float u = p.age / p.life;
                float al = p.a0 * Math.min(1f, u / 0.12f) * Math.min(1f, (1f - u) / 0.4f) * Math.min(1f, k * 1.5f);
                if (al <= 0.003f) continue;
                if (p.kind == 3) {
                    int c = ((int) (al * 255f) << 24)
                            | ((int) (p.r * 255f) << 16) | ((int) (p.g * 255f) << 8) | (int) (p.b * 255f);
                    Gfx.line(m, p.x, p.y, p.x - p.vx * 0.045f, p.y - p.vy * 0.045f, p.size, c);
                } else {
                    ms.push();
                    ms.translate(p.x, p.y, 0f);
                    ms.multiply(RotationAxis.POSITIVE_Z.rotation(p.rot));
                    float h = p.size * 0.5f;
                    Gfx.quad(ms.peek().getPositionMatrix(), p.tex, -h, -h, h, h, p.r, p.g, p.b, al);
                    ms.pop();
                }
            }
        }
        Gfx.additive(false);
    }
}
