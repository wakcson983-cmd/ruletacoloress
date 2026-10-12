package com.ruletacolores.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuracion local del jugador (se guarda en config/ruleta_colores.properties). */
final class RuletaConfig {
    static boolean effects = true;       // efectos de pantalla por color
    static boolean blur = true;          // motion blur al girar
    static boolean flash = true;         // destello al caer el color
    static boolean chat = true;          // mensaje en el chat
    static boolean notification = true;  // cuadro arriba a la izquierda
    static boolean pointerTop = false;   // flecha arriba (true) o a la izquierda (false)
    static int volume = 100;             // 0-100
    static int noteSeconds = 120;        // duracion de la notificacion
    static float holdSeconds = 4.5f;     // tiempo que se queda la ruleta tras caer el color
    static int wheelSize = 100;          // tamano de la ruleta en %

    static final int[] VOLUMES = {0, 25, 50, 75, 100};
    static final int[] NOTE_SECONDS = {15, 30, 60, 120};
    static final float[] HOLD = {3f, 4.5f, 6f, 8f};
    static final int[] SIZES = {70, 85, 100, 120};

    private RuletaConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("ruleta_colores.properties");
    }

    static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(f)) {
            p.load(in);
        } catch (IOException e) {
            return;
        }
        effects = bool(p, "effects", effects);
        blur = bool(p, "blur", blur);
        flash = bool(p, "flash", flash);
        chat = bool(p, "chat", chat);
        notification = bool(p, "notification", notification);
        pointerTop = bool(p, "pointerTop", pointerTop);
        volume = num(p, "volume", volume);
        noteSeconds = num(p, "noteSeconds", noteSeconds);
        holdSeconds = num(p, "holdTenths", Math.round(holdSeconds * 10f)) / 10f;
        wheelSize = num(p, "wheelSize", wheelSize);
    }

    static void save() {
        Properties p = new Properties();
        p.setProperty("effects", String.valueOf(effects));
        p.setProperty("blur", String.valueOf(blur));
        p.setProperty("flash", String.valueOf(flash));
        p.setProperty("chat", String.valueOf(chat));
        p.setProperty("notification", String.valueOf(notification));
        p.setProperty("pointerTop", String.valueOf(pointerTop));
        p.setProperty("volume", String.valueOf(volume));
        p.setProperty("noteSeconds", String.valueOf(noteSeconds));
        p.setProperty("holdTenths", String.valueOf(Math.round(holdSeconds * 10f)));
        p.setProperty("wheelSize", String.valueOf(wheelSize));
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Ruleta de Colores");
        } catch (IOException ignored) {
        }
    }

    private static boolean bool(Properties p, String k, boolean d) {
        String v = p.getProperty(k);
        return v == null ? d : Boolean.parseBoolean(v.trim());
    }

    private static int num(Properties p, String k, int d) {
        try {
            return Integer.parseInt(p.getProperty(k, String.valueOf(d)).trim());
        } catch (NumberFormatException e) {
            return d;
        }
    }

    static int nextInt(int[] arr, int cur) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == cur) return arr[(i + 1) % arr.length];
        }
        return arr[0];
    }

    static float nextFloat(float[] arr, float cur) {
        for (int i = 0; i < arr.length; i++) {
            if (Math.abs(arr[i] - cur) < 0.01f) return arr[(i + 1) % arr.length];
        }
        return arr[0];
    }
}
