package com.ruletacolores.client;

import com.ruletacolores.RuletaMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;

final class Sfx {
    private Sfx() {}

    /** Sonido no posicional (se oye igual para todos). */
    static void play(String name, float pitch, float volume) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        float vol = volume * RuletaConfig.volume / 100f;
        if (vol <= 0.001f) return;
        SoundEvent ev = SoundEvent.of(RuletaMod.id(name));
        mc.getSoundManager().play(PositionedSoundInstance.master(ev, pitch, vol));
    }
}
