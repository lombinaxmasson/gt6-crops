package com.lombinaxmasson.gt6crops.client.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;

import net.minecraft.resources.ResourceLocation;

/** These pages describe the world. They are not a crafting grid and not a craft tree. */
abstract class CropEmiRecipe extends BasicEmiRecipe {
    CropEmiRecipe(EmiRecipeCategory category, ResourceLocation id, int width, int height) {
        super(category, id, width, height);
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public boolean hideCraftable() {
        return true;
    }
}
