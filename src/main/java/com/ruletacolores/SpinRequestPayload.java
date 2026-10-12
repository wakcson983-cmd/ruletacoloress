package com.ruletacolores;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Cliente -> servidor: "quiero girar la ruleta y que caiga este color". */
public record SpinRequestPayload(int color, String title, String text, boolean effects) implements CustomPayload {
    public static final CustomPayload.Id<SpinRequestPayload> ID =
            new CustomPayload.Id<>(Identifier.of(RuletaMod.MOD_ID, "spin_request"));

    public static final PacketCodec<RegistryByteBuf, SpinRequestPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, SpinRequestPayload::color,
            PacketCodecs.STRING, SpinRequestPayload::title,
            PacketCodecs.STRING, SpinRequestPayload::text,
            PacketCodecs.BOOL, SpinRequestPayload::effects,
            SpinRequestPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
