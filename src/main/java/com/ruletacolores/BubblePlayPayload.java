package com.ruletacolores;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Mensaje del globo de texto (servidor a todos). */
public record BubblePlayPayload(int color, String title, String text) implements CustomPayload {
    public static final CustomPayload.Id<BubblePlayPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RuletaMod.MOD_ID, "bubble_play"));

    public static final PacketCodec<RegistryByteBuf, BubblePlayPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, BubblePlayPayload::color,
            PacketCodecs.STRING, BubblePlayPayload::title,
            PacketCodecs.STRING, BubblePlayPayload::text,
            BubblePlayPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
