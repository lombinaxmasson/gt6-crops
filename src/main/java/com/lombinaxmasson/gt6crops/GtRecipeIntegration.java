package com.lombinaxmasson.gt6crops;

import java.util.Arrays;
import java.util.Collection;

import com.gregtech.gregtech.api.addon.GregTechAddon;
import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.api.recipe.RecipeMap;
import com.gregtech.gregtech.data.MaterialPrefix;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.gregtech.gregtech.registry.GTItems;
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
        int added = 0;
        for (CropCard card : CropCards.cards()) {
            if (!card.drop().kind().equals("gt_form")
                    || !card.drop().prefix().startsWith("plantGt")) {
                continue;
            }
            ItemStack input = card.drop().resolve().orElse(ItemStack.EMPTY);
            var material = card.drop().gtMaterial();
            ItemStack output = material.isValid()
                    ? GTItems.getStack(MaterialPrefix.dustTiny, material)
                    : ItemStack.EMPTY;
            if (input.isEmpty() || output.isEmpty()
                    || alreadyHasInput(MachineRecipeMaps.Mortar, input)) {
                continue;
            }
            Recipe recipe = new Recipe(
                    new ItemStack[] {input},
                    new ItemStack[] {output},
                    null, null, null, null, 16, 16, 0);
            requireAccepted(MachineRecipeMaps.Mortar, recipe, card.id());
            if (!alreadyHasInput(MachineRecipeMaps.Shredder, input)) {
                Recipe shredderRecipe = new Recipe(
                        new ItemStack[] {input},
                        new ItemStack[] {output},
                        null, null, null, null, 16, 16, 0);
                requireAccepted(MachineRecipeMaps.Shredder, shredderRecipe, card.id());
            }
            added++;
        }
        Gt6Crops.LOGGER.info(
                "gt6crops: added {} plant-form processing rows for {} {}",
                added, context.platform(), context.minecraftVersion());
    }

    private static void requireAccepted(RecipeMap map, Recipe recipe, String cardId) {
        if (map.addRecipe(recipe) == null) {
            throw new IllegalStateException(
                    "gt6crops: rejected " + map.mNameInternal + " recipe for " + cardId);
        }
    }

    private static boolean alreadyHasInput(RecipeMap map, ItemStack input) {
        Collection<Recipe> recipes = map.mRecipeItemMap.get(input.getItem());
        return recipes != null && recipes.stream()
                .anyMatch(recipe -> Arrays.stream(recipe.mInputs)
                        .anyMatch(candidate -> candidate.getItem() == input.getItem()));
    }
}
