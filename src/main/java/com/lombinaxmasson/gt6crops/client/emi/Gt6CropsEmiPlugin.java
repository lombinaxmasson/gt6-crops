package com.lombinaxmasson.gt6crops.client.emi;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.card.CropMutations;
import com.lombinaxmasson.gt6crops.rules.CropBreeding;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiIngredient;

import net.minecraft.resources.ResourceLocation;

/**
 * Planting, fixed crosses, and mutation pools. EMI loads this through
 * {@link EmiEntrypoint}; without EMI the class is never initialized.
 */
@EmiEntrypoint
public final class Gt6CropsEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        CropCards.initialize();
        if (CropCards.cards().isEmpty()) {
            Gt6Crops.LOGGER.warn("gt6crops: EMI reload found no crop cards");
            return;
        }
        registry.setDefaultComparison(
                Gt6Crops.CROP_SEED.get(), Comparison.compareData(CropEmiStacks::cardId));
        for (CropCard card : CropCards.cards()) {
            if (!card.isWeed()) {
                registry.addEmiStack(CropEmiStacks.seed(card));
            }
        }

        EmiRecipeCategory planting = category("planting");
        EmiRecipeCategory crossbreeding = category("crossbreeding");
        EmiRecipeCategory pools = category("mutation_pool");
        registry.addCategory(planting);
        registry.addCategory(crossbreeding);
        registry.addCategory(pools);
        EmiIngredient stick = CropEmiStacks.stick();
        registry.addWorkstation(planting, stick);
        registry.addWorkstation(crossbreeding, stick);
        registry.addWorkstation(pools, stick);

        int plantingPages = 0;
        int baseSeedPages = 0;
        for (CropCard card : CropCards.cards()) {
            if (card.isWeed()) {
                continue;
            }
            registry.addRecipe(new PlantingEmiRecipe(planting, card));
            plantingPages++;
            if (card.hasPlantableBaseSeed()) {
                registry.addRecipe(new AlternateSeedEmiRecipe(planting, card));
                baseSeedPages++;
            }
        }
        int crossPages = 0;
        for (CropBreeding.Mutation mutation : CropMutations.book().mutations()) {
            CrossbreedEmiRecipe recipe = CrossbreedEmiRecipe.create(crossbreeding, mutation);
            if (recipe != null) {
                registry.addRecipe(recipe);
                crossPages++;
            }
        }
        int poolPages = 0;
        for (CropBreeding.Pool pool : CropMutations.book().pools()) {
            MutationPoolEmiRecipe recipe = MutationPoolEmiRecipe.create(pools, pool);
            if (recipe != null) {
                registry.addRecipe(recipe);
                poolPages++;
            }
        }
        Gt6Crops.LOGGER.info("gt6crops: EMI pages: {} planting, {} base seeds, {} crosses, {} pools",
                plantingPages, baseSeedPages, crossPages, poolPages);
    }

    private static EmiRecipeCategory category(String path) {
        return new EmiRecipeCategory(
                ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, path),
                CropEmiIcons.get(path));
    }
}
