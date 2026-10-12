package com.ruletacolores;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Globo de texto: click derecho abre un menu para escribir un mensaje que sale en el chat de todos. */
public class BubbleItem extends Item {
    public static Runnable clientOpenMenu = null;

    public BubbleItem(Settings settings) {
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
