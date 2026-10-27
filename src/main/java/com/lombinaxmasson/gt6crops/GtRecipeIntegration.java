package com.lombinaxmasson.gt6crops;

import gregtech.api.addon.GregTechAddon;
import gregtech.api.addon.platform.AddonItems;
import gregtech.api.addon.platform.AddonRecipes;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;

import net.minecraft.world.item.ItemStack;

/**
 * Small, conflict-aware addon recipe layer.
 *
 * <p>GT6CE already supplies most food processing rows. This class fills the
 * gap for GT plant-form crop drops by adding a tiny-dust mortar/shredder row
 * only when GT has not already registered an input row.</p>
 */
public final class GtRecipeIntegration {
    private GtRecipeIntegration() {}

    public static void register(GregTechAddon.Context context) {
        SeedScanRecipes.register();
        AddonRecipes.Map mortar = AddonRecipes.map("gt.recipe.mortar");
        AddonRecipes.Map shredder = AddonRecipes.map("gt.recipe.shredder");
        int added = 0;
        for (CropCard card : CropCards.cards()) {
            if (!card.drop().kind().equals("gt_form")
                    || !card.drop().prefix().startsWith("plantGt")) {
                continue;
            }
            ItemStack input = card.drop().resolve().orElse(ItemStack.EMPTY);
            var material = card.drop().gtMaterial();
            ItemStack output = material.valid()
                    ? AddonItems.stack("dustTiny", material.name())
                    : ItemStack.EMPTY;
            if (input.isEmpty() || output.isEmpty() || mortar.hasItemInput(input)) {
                continue;
            }
            requireAccepted(mortar, row(input, output), card.id());
            if (!shredder.hasItemInput(input)) {
                requireAccepted(shredder, row(input, output), card.id());
            }
            added++;
        }
        Gt6Crops.LOGGER.info(
                "gt6crops: added {} plant-form processing rows for {} {}",
                added, context.platform(), context.minecraftVersion());
    }

    static void requireAccepted(AddonRecipes.Map map, AddonRecipes.Line line, String what) {
        if (map.add(line) == null) {
            throw new IllegalStateException(
                    "gt6crops: rejected " + map.name() + " recipe for " + what);
        }
    }

    private static AddonRecipes.Line row(ItemStack input, ItemStack output) {
        return new AddonRecipes.Line(
                new ItemStack[] {input},
                new ItemStack[] {output},
                null, null, 16, 16, 0);
    }
}
