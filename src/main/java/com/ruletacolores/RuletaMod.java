package com.ruletacolores;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RuletaMod implements ModInitializer {
    public static final String MOD_ID = "ruleta_colores";
    public static final String[] SOUNDS = {"tick", "enter", "exit", "spin_start", "land", "recolor", "select", "bubble"};

    /** Duracion aproximada de toda la animacion en el cliente (ms), para no solapar giros. */
    private static final long BUSY_MS = 11_000L;
    private static long busyUntil = 0L;

    public static Item RULETA;
    public static Item GLOBO;
    private static long lastBubble = 0L;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        RULETA = Registry.register(Registries.ITEM, id("ruleta"), new RuletaItem(new Item.Settings().maxCount(1)));
        GLOBO = Registry.register(Registries.ITEM, id("globo"), new BubbleItem(new Item.Settings().maxCount(1)));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(RULETA);
            entries.add(GLOBO);
        });

        for (String s : SOUNDS) {
            Identifier sid = id(s);
            Registry.register(Registries.SOUND_EVENT, sid, SoundEvent.of(sid));
        }

        PayloadTypeRegistry.playC2S().register(SpinRequestPayload.ID, SpinRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpinPlayPayload.ID, SpinPlayPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BubbleRequestPayload.ID, BubbleRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BubblePlayPayload.ID, BubblePlayPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SpinRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!player.getMainHandStack().isOf(RULETA) && !player.getOffHandStack().isOf(RULETA)) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now < busyUntil) {
                player.sendMessage(Text.literal("La ruleta ya esta girando, espera un momento."), true);
                return;
            }
            busyUntil = now + BUSY_MS;

            int color = Math.floorMod(payload.color(), 8);
            String title = clamp(payload.title(), 40);
            String text = clamp(payload.text(), 240);
            SpinPlayPayload out = new SpinPlayPayload(color, title, text, payload.effects());
            for (ServerPlayerEntity p : context.server().getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, out);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(BubbleRequestPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!player.getMainHandStack().isOf(GLOBO) && !player.getOffHandStack().isOf(GLOBO)) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - lastBubble < 1200L) {
                return;
            }
            lastBubble = now;
            String text = clamp(payload.text(), 400);
            if (text.isEmpty()) return;
            BubblePlayPayload out = new BubblePlayPayload(Math.floorMod(payload.color(), 8), clamp(payload.title(), 40), text);
            for (ServerPlayerEntity p : context.server().getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, out);
            }
        });
    }

    private static String clamp(String s, int max) {
        if (s == null) return "";
        s = s.strip();
        return s.length() > max ? s.substring(0, max) : s;
    }
}
