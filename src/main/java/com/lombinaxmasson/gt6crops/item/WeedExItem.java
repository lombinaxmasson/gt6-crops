package com.lombinaxmasson.gt6crops.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Consumable crop-care item that suppresses weeds around one crop tile. */
public final class WeedExItem extends Item {
    public WeedExItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gt6crops.weed_ex.tooltip"));
    }
}
