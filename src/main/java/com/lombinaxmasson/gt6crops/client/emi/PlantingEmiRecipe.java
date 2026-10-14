package com.lombinaxmasson.gt6crops.client.emi;

import java.util.ArrayList;
import java.util.List;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.block.GrowthCheck;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropConditions;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * CropsNH's crop page. The left column is the crop seed, its soil, and its subsoil.
 * The right grid is the harvest. The arrow is painted on {@code crop_list.png}.
 */
final class PlantingEmiRecipe extends CropEmiRecipe {
    /** CropsNH prints 100% as 10000 hundredths of a percent. */
    private static final int MAIN_CHANCE = 10_000;
    /** A special drop lands on one roll in four. */
    private static final int SPECIAL_CHANCE = 2_500;

    private final EmiStack crop;
    private final EmiIngredient soil;
    private final EmiIngredient subSoil;
    private final List<EmiStack> harvests;
    private final boolean mainDrop;
    private final List<Component> lines;

    PlantingEmiRecipe(EmiRecipeCategory category, CropCard card) {
        super(category, id(card), EmiPages.CROP_WIDTH, 1);
        crop = CropEmiStacks.seed(card);
        inputs.add(crop);

        CropConditions conditions = card.conditions();
        soil = CropEmiStacks.blocks(conditions.soil());
        subSoil = conditions.subSoil().isEmpty()
                ? EmiStack.EMPTY
                : CropEmiStacks.blocks(conditions.subSoil());
        if (!soil.isEmpty()) {
            catalysts.add(soil);
        }
        if (!subSoil.isEmpty()) {
            catalysts.add(subSoil);
        }

        harvests = CropEmiStacks.harvest(card);
        mainDrop = card.harvestDrop().isPresent() && !harvests.isEmpty();
        outputs.addAll(harvests);

        lines = lines(card, conditions, soil, subSoil);
        height = EmiPages.CROP_TEXT_Y + lines.size() * EmiPages.LINE;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        EmiPages.image(widgets, EmiPages.CROP_LIST, EmiPages.CROP_ART_W, EmiPages.CROP_ART_H);
        EmiPages.item(widgets, crop, EmiPages.CROP_X_SEED, EmiPages.CROP_Y_SEED);
        if (!soil.isEmpty()) {
            EmiPages.item(widgets, soil, EmiPages.CROP_X_SEED, EmiPages.CROP_Y_SOIL).catalyst(true);
        }
        if (!subSoil.isEmpty()) {
            EmiPages.item(widgets, subSoil, EmiPages.CROP_X_SEED, EmiPages.CROP_Y_BASE).catalyst(true);
        }
        for (int index = 0; index < harvests.size(); index++) {
            int x = EmiPages.CROP_X_PRODUCT + 18 * (index % EmiPages.CROP_COLUMNS);
            int y = EmiPages.CROP_Y_PRODUCT + 18 * (index / EmiPages.CROP_COLUMNS);
            EmiPages.item(widgets, harvests.get(index), x, y).recipeContext(this);
            EmiPages.chance(widgets, x, y, mainDrop && index == 0 ? MAIN_CHANCE : SPECIAL_CHANCE);
        }
        for (int index = 0; index < lines.size(); index++) {
            EmiPages.line(
                    widgets,
                    lines.get(index),
                    0,
                    EmiPages.CROP_TEXT_Y + EmiPages.LINE * index,
                    EmiPages.CROP_WIDTH);
        }
    }

    private static List<Component> lines(
            CropCard card, CropConditions conditions, EmiIngredient soil, EmiIngredient subSoil) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("emi.gt6crops.planting.tier", card.tier()));
        lines.add(Component.translatable(
                "emi.gt6crops.planting.growth", CropRules.growthThreshold(card)));
        if (!conditions.likedBiomes().isEmpty()) {
            lines.add(Component.translatable(
                    "emi.gt6crops.planting.biomes", GrowthCheck.join(conditions.likedBiomes())));
            lines.add(Component.translatable("emi.gt6crops.planting.biome_bonus"));
        }
        if (conditions.minLight() > 0 || conditions.maxLight() < CropConditions.MAX_LIGHT) {
            lines.add(Component.translatable(
                    "emi.gt6crops.planting.light", conditions.minLight(), conditions.maxLight()));
        }
        if (!conditions.soil().isEmpty() && soil.isEmpty()) {
            lines.add(Component.translatable(
                    "emi.gt6crops.planting.missing", GrowthCheck.join(conditions.soil())));
        }
        if (!conditions.subSoil().isEmpty() && subSoil.isEmpty()) {
            lines.add(Component.translatable(
                    "emi.gt6crops.planting.missing", GrowthCheck.join(conditions.subSoil())));
        }
        if (card.hasPlantableBaseSeed()) {
            lines.add(Component.translatable("emi.gt6crops.planting.base"));
            lines.add(Component.translatable("emi.gt6crops.planting.base_cross"));
        } else {
            lines.add(Component.translatable("emi.gt6crops.planting.bred"));
        }
        return List.copyOf(lines);
    }

    private static ResourceLocation id(CropCard card) {
        return ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "planting/" + CropEmiStacks.safe(card.id()));
    }
}
