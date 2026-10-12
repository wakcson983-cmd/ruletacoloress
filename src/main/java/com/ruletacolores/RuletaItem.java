package com.ruletacolores;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class RuletaItem extends Item {
    /** Lo pone el cliente al iniciar para abrir el menu (asi el servidor no carga clases de cliente). */
    public static Runnable clientOpenMenu = null;

    public RuletaItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient() && clientOpenMenu != null) {
            clientOpenMenu.run();
        }
        return TypedActionResult.success(stack);
    }
}
