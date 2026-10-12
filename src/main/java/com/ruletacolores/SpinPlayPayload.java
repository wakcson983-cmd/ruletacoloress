package com.ruletacolores;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Servidor -> todos los clientes: "reproduce la ruleta". No lleva el nombre de quien la giro. */
public record SpinPlayPayload(int color, String title, String text, boolean effects) implements CustomPayload {
    public static final CustomPayload.Id<SpinPlayPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RuletaMod.MOD_ID, "spin_play"));

    public static final PacketCodec<RegistryByteBuf, SpinPlayPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SpinPlayPayload::color,
            PacketCodecs.STRING, SpinPlayPayload::title,
            PacketCodecs.STRING, SpinPlayPayload::text,
            PacketCodecs.BOOL, SpinPlayPayload::effects,
            SpinPlayPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
