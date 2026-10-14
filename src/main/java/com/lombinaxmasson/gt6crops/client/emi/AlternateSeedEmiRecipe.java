package com.lombinaxmasson.gt6crops.client.emi;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A base seed on the left, the crop seed on the right. The arrow is painted on
 * {@code alternate_seeds.png}.
 */
final class AlternateSeedEmiRecipe extends CropEmiRecipe {

    private final EmiStack base;
    private final EmiStack crop;

    AlternateSeedEmiRecipe(EmiRecipeCategory category, CropCard card) {
        super(category, id(card), EmiPages.ALTERNATE_WIDTH, EmiPages.ALTERNATE_HEIGHT);
        ItemStack stack = card.baseSeed().resolve().orElse(ItemStack.EMPTY).copy();
        stack.setCount(1);
        base = EmiStack.of(stack);
        crop = CropEmiStacks.seed(card);
        inputs.add(base);
        outputs.add(crop);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        EmiPages.image(widgets, EmiPages.ALTERNATE_SEEDS, EmiPages.ALTERNATE_WIDTH, EmiPages.ALTERNATE_HEIGHT);
        EmiPages.item(widgets, base, EmiPages.ALT_INPUT_X, EmiPages.ALT_Y);
        EmiPages.item(widgets, crop, EmiPages.ALT_OUTPUT_X, EmiPages.ALT_Y).recipeContext(this);
    }

    private static ResourceLocation id(CropCard card) {
        return ResourceLocation.fromNamespaceAndPath(
                Gt6Crops.MODID, "planting/base/" + CropEmiStacks.safe(card.id()));
    }
}
