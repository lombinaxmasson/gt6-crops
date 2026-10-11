package com.lombinaxmasson.gt6crops;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tags exposed by the addon for crop-care integrations. */
public final class Gt6CropsTags {
    public static final TagKey<Item> FERTILIZER = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "fertilizer"));
    /** Soils that keep the crops on them from being trampled. */
    public static final TagKey<Block> TRAMPLE_PROOF_SOIL = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "trample_proof_soil"));

    private Gt6CropsTags() {}
}
