package com.lombinaxmasson.gt6crops;

import gregtech.api.addon.GregTechAddon;
import gregtech.api.addon.platform.AddonFluids;
import gregtech.api.addon.platform.AddonItems;
import gregtech.api.addon.platform.AddonRecipes;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.card.ItemRef;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Small, conflict-aware addon recipe layer.
 *
 * <p>GT6CE already supplies most food processing rows. This class adds
 * tiny-dust processing for plant products and squeezing for oil berries.</p>
 */
public final class GtRecipeIntegration {
    private GtRecipeIntegration() {}

    public static void register(GregTechAddon.Context context) {
        SeedScanRecipes.register();
        AddonRecipes.RecipeMap mortar = AddonRecipes.map("gt.recipe.mortar");
        AddonRecipes.RecipeMap shredder = AddonRecipes.map("gt.recipe.shredder");
        int added = 0;
        for (CropCard card : CropCards.cards()) {
            ItemRef drop = card.drop();
            if (!drop.isCropProduct()) {
                continue;
            }
            ItemStack input = drop.resolve().orElse(ItemStack.EMPTY);
            ItemStack output = drop.gtMaterial()
                    .map(material -> AddonItems.stack("dustTiny", material.name()))
                    .orElse(ItemStack.EMPTY);
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
                "gt6crops: registered processing for {} crop products ({} {})",
                added, context.platform(), context.minecraftVersion());
        registerOilBerries();
    }

    private static void registerOilBerries() {
        if (CropCards.find("oil_berries").isEmpty()) {
            return;
        }
        FluidStack oil = AddonFluids.still("Oil_Normal", 100);
        if (oil == null || oil.isEmpty()) {
            Gt6Crops.LOGGER.warn("gt6crops: skipped the oil berry squeezer recipe, GT6CE has no Oil_Normal fluid");
            return;
        }
        ItemStack berries = new ItemStack(Gt6Crops.OIL_BERRIES.get());
        AddonRecipes.RecipeMap squeezer = AddonRecipes.map("gt.recipe.squeezer");
        if (!squeezer.hasItemInput(berries)) {
            requireAccepted(squeezer, AddonRecipes.Line.builder()
                    .itemInputs(berries)
                    .fluidOutputs(oil)
                    .duration(20)
                    .energyPerTick(16)
                    .build(), "oil berries");
        }
    }

    static void requireAccepted(AddonRecipes.RecipeMap map, AddonRecipes.Line line, String what) {
        if (map.add(line) == null) {
            throw new IllegalStateException(
                    "gt6crops: rejected " + map.name() + " recipe for " + what);
        }
    }

    private static AddonRecipes.Line row(ItemStack input, ItemStack output) {
        return AddonRecipes.Line.builder()
                .itemInputs(input)
                .itemOutputs(output)
                .duration(16)
                .energyPerTick(16)
                .build();
    }
}
