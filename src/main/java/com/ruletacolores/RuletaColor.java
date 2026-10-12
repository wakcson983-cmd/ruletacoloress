package com.ruletacolores;

public enum RuletaColor {
    ROJO("Rojo", 0xFF0000),
    ROSA("Rosa", 0xFF00C4),
    MORADO("Morado", 0x7C00FF),
    AZUL("Azul", 0x0047FF),
    CELESTE("Celeste", 0x00B8FF),
    VERDE("Verde", 0x03FF00),
    AMARILLO("Amarillo", 0xFFF900),
    NARANJA("Naranja", 0xFF5B00);

    public final String label;
    public final int rgb;

    RuletaColor(String label, int rgb) {
        this.label = label;
        this.rgb = rgb;
    }

    public static RuletaColor byId(int id) {
        RuletaColor[] v = values();
        return v[Math.floorMod(id, v.length)];
    }
}
