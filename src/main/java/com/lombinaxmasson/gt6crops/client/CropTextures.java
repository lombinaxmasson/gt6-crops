package com.lombinaxmasson.gt6crops.client;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;

import net.minecraft.resources.ResourceLocation;

/** Sprite paths for a planted crop. Stage art lives on the block atlas. */
public final class CropTextures {
    private CropTextures() {}

    public static ResourceLocation of(CropCard card, int size) {
        if (card.isWeed()) {
            int stage = Math.max(1, Math.min(4, size));
            return ResourceLocation.fromNamespaceAndPath(
                    Gt6Crops.MODID, "block/crop/weed/" + stage);
        }
        if (isVanilla(card.id())) {
            return vanilla(card.id(), size);
        }
        int stage = Math.max(1, Math.min(card.maxSize(), size));
        return ResourceLocation.fromNamespaceAndPath(
                Gt6Crops.MODID, "block/crop/" + card.texture() + "/" + stage);
    }

    private static ResourceLocation vanilla(String id, int size) {
        return switch (id) {
            case "wheat" -> ResourceLocation.withDefaultNamespace(
                    "block/wheat_stage" + Math.min(7, size + 1));
            case "carrot", "potato", "beetroot" -> ResourceLocation.withDefaultNamespace(
                    "block/" + id + "_stage" + Math.min(3, Math.max(0, size - 1)));
            case "pumpkin", "melon" -> ResourceLocation.withDefaultNamespace(
                    "block/" + id + "_stem_stage" + Math.min(7, size + 1));
            case "sugar_cane", "nether_wart" -> ResourceLocation.withDefaultNamespace(
                    "block/" + id);
            case "cocoa" -> ResourceLocation.withDefaultNamespace("block/cocoa_stage2");
            default -> ResourceLocation.withDefaultNamespace("block/" + id);
        };
    }

    private static boolean isVanilla(String id) {
        return switch (id) {
            case "wheat", "carrot", "potato", "beetroot", "pumpkin", "melon",
                    "sugar_cane", "nether_wart", "cocoa", "dandelion", "poppy",
                    "blue_orchid", "allium", "azure_bluet", "red_tulip",
                    "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy",
                    "cornflower", "lily_of_the_valley" -> true;
            default -> false;
        };
    }
}
