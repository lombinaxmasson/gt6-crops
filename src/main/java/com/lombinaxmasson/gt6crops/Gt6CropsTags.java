package com.lombinaxmasson.gt6crops;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Tags exposed by the addon for crop-care integrations. */
public final class Gt6CropsTags {
    public static final TagKey<Item> FERTILIZER = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "fertilizer"));

    private Gt6CropsTags() {}
}
