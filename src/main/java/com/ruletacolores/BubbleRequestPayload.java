package com.ruletacolores;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Mensaje del globo de texto (cliente a servidor). */
public record BubbleRequestPayload(int color, String title, String text) implements CustomPayload {
    public static final CustomPayload.Id<BubbleRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RuletaMod.MOD_ID, "bubble_request"));

    public static final PacketCodec<RegistryByteBuf, BubbleRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, BubbleRequestPayload::color,
            PacketCodecs.STRING, BubbleRequestPayload::title,
            PacketCodecs.STRING, BubbleRequestPayload::text,
            BubbleRequestPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
