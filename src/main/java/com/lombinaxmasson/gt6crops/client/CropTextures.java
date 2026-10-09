package com.lombinaxmasson.gt6crops.client;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;

import net.minecraft.resources.ResourceLocation;

/**
 * Sprite paths for a planted crop. Stage art lives on the block atlas, under
 * {@code block/crop/<texture>/<stage>}, either shipped or drawn by {@link CropStageSprites}.
 */
public final class CropTextures {
    /** A stage sprite, and whether vanilla tints it with the biome's grass color. */
    public record PlantSprite(ResourceLocation texture, boolean grassTint) {}

    private CropTextures() {}

    public static PlantSprite of(CropCard card, int size) {
        ResourceLocation texture = switch (card.id()) {
            case "wheat" -> vanilla("block/wheat_stage" + stage(card, size, 7));
            case "carrot" -> vanilla("block/carrots_stage" + stage(card, size, 3));
            case "potato" -> vanilla("block/potatoes_stage" + stage(card, size, 3));
            case "beetroot" -> vanilla("block/beetroots_stage" + stage(card, size, 3));
            case "nether_wart" -> vanilla("block/nether_wart_stage" + stage(card, size, 2));
            default -> ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID,
                    "block/crop/" + card.texture() + "/" + Math.max(1, Math.min(card.maxSize(), size)));
        };
        return new PlantSprite(texture, "sugar_cane".equals(card.id()));
    }

    /** Spreads sizes 1..maxSize over vanilla stages 0..ripe, so a mature crop looks ripe. */
    private static int stage(CropCard card, int size, int ripe) {
        if (size >= card.maxSize()) {
            return ripe;
        }
        return Math.max(0, (size - 1) * ripe / (card.maxSize() - 1));
    }

    private static ResourceLocation vanilla(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
