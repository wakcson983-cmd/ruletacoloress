package com.ruletacolores.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Configuracion de la ruleta: cada boton cambia una opcion al hacer click. */
public class ConfigScreen extends Screen {
    private final Screen parent;
    private final List<ButtonWidget> buttons = new ArrayList<>();
    private final List<Supplier<Text>> labels = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Text.literal("Configuracion"));
        this.parent = parent;
    }

    private static String yn(boolean b) {
        return b ? "SI" : "NO";
    }

    private void option(int row, Supplier<Text> label, Runnable change) {
        int w = 240;
        int x = this.width / 2 - w / 2;
        int y = 44 + row * 24;
        ButtonWidget b = ButtonWidget.builder(label.get(), btn -> {
            change.run();
            RuletaConfig.save();
            refresh();
        }).dimensions(x, y, w, 20).build();
        buttons.add(b);
        labels.add(label);
        this.addDrawableChild(b);
    }

    private void refresh() {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setMessage(labels.get(i).get());
        }
    }

    @Override
    protected void init() {
        buttons.clear();
        labels.clear();
        int r = 0;
        option(r++, () -> Text.literal("Efectos de pantalla: " + yn(RuletaConfig.effects)), () -> RuletaConfig.effects = !RuletaConfig.effects);
        option(r++, () -> Text.literal("Motion blur al girar: " + yn(RuletaConfig.blur)), () -> RuletaConfig.blur = !RuletaConfig.blur);
        option(r++, () -> Text.literal("Destello al caer: " + yn(RuletaConfig.flash)), () -> RuletaConfig.flash = !RuletaConfig.flash);
        option(r++, () -> Text.literal("Mensaje en el chat: " + yn(RuletaConfig.chat)), () -> RuletaConfig.chat = !RuletaConfig.chat);
        option(r++, () -> Text.literal("Notificacion: " + yn(RuletaConfig.notification)), () -> RuletaConfig.notification = !RuletaConfig.notification);
        option(r++, () -> Text.literal("Duracion notificacion: " + RuletaConfig.noteSeconds + " s"),
                () -> RuletaConfig.noteSeconds = RuletaConfig.nextInt(RuletaConfig.NOTE_SECONDS, RuletaConfig.noteSeconds));
        option(r++, () -> Text.literal("Volumen de sonidos: " + RuletaConfig.volume + "%"),
                () -> RuletaConfig.volume = RuletaConfig.nextInt(RuletaConfig.VOLUMES, RuletaConfig.volume));
        option(r++, () -> Text.literal("Tiempo en pantalla tras caer: " + RuletaConfig.holdSeconds + " s"),
                () -> RuletaConfig.holdSeconds = RuletaConfig.nextFloat(RuletaConfig.HOLD, RuletaConfig.holdSeconds));
        option(r++, () -> Text.literal("Tamano de la ruleta: " + RuletaConfig.wheelSize + "%"),
                () -> RuletaConfig.wheelSize = RuletaConfig.nextInt(RuletaConfig.SIZES, RuletaConfig.wheelSize));
        option(r++, () -> Text.literal("Posicion de la flecha: " + (RuletaConfig.pointerTop ? "ARRIBA" : "IZQUIERDA")),
                () -> RuletaConfig.pointerTop = !RuletaConfig.pointerTop);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("LISTO"), b -> this.close())
                .dimensions(this.width / 2 - 120, 44 + r * 24 + 8, 240, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("CONFIGURACION - RULETA").formatted(Formatting.BOLD),
                this.width / 2, 22, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        if (this.client != null) this.client.setScreen(parent);
    }
}
