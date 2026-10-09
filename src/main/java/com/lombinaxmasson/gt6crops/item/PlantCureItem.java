package com.lombinaxmasson.gt6crops.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Reusable spray that heals a sick crop; each cure costs one use. */
public final class PlantCureItem extends Item {
    public PlantCureItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gt6crops.plant_cure.tooltip"));
    }
}
