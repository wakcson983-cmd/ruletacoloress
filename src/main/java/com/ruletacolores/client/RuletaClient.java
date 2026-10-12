package com.ruletacolores.client;

import com.ruletacolores.BubbleItem;
import com.ruletacolores.BubblePlayPayload;
import com.ruletacolores.RuletaItem;
import com.ruletacolores.SpinPlayPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class RuletaClient implements ClientModInitializer {
    /** Tecla para ocultar la notificacion (cambiala en Controles; por defecto K porque L es de Logros). */
    public static KeyBinding hideKey;
    /** Tecla para leer el mensaje completo de la notificacion (N). */
    public static KeyBinding readKey;
    public static KeyBinding configKey;

    @Override
    public void onInitializeClient() {
        RuletaConfig.load();
        RuletaItem.clientOpenMenu = () -> MinecraftClient.getInstance().setScreen(new RuletaScreen(false));
        BubbleItem.clientOpenMenu = () -> MinecraftClient.getInstance().setScreen(new RuletaScreen(true));

        hideKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ruleta_colores.hide", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.ruleta_colores"));

        readKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ruleta_colores.read", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "key.categories.ruleta_colores"));
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ruleta_colores.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.ruleta_colores"));

        ClientPlayNetworking.registerGlobalReceiver(SpinPlayPayload.ID, (payload, context) ->
                RuletaHud.INSTANCE.enqueue(payload.color(), payload.title(), payload.text(), payload.effects()));
        ClientPlayNetworking.registerGlobalReceiver(BubblePlayPayload.ID, (payload, context) ->
                RuletaHud.INSTANCE.showBubble(payload.color(), payload.title(), payload.text()));

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> RuletaHud.INSTANCE.render(drawContext));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hideKey.wasPressed()) {
                RuletaHud.INSTANCE.hideNotification();
            }
            while (readKey.wasPressed()) {
                RuletaHud.INSTANCE.openFullText();
            }
            while (configKey.wasPressed()) {
                client.setScreen(new ConfigScreen(null));
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> RuletaHud.INSTANCE.reset());
    }
}
