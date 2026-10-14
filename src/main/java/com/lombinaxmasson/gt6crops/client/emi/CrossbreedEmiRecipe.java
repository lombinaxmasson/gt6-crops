package com.lombinaxmasson.gt6crops.client.emi;

import java.util.ArrayList;
import java.util.List;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.Gt6CropsConfig;
import com.lombinaxmasson.gt6crops.block.GrowthCheck;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.card.CropConditions;
import com.lombinaxmasson.gt6crops.rules.CropBreeding;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * CropsNH's crop-stick breeding page. Two parents use {@code breeding2.png};
 * three or four use {@code breeding4.png}. The child is the center slot, and
 * the soil and subsoil under it are the child's. Parents stay planted.
 */
final class CrossbreedEmiRecipe extends CropEmiRecipe {
    private final List<EmiStack> parents;
    private final EmiStack child;
    private final EmiIngredient soil;
    private final EmiIngredient subSoil;
    private final List<Component> lines;

    static CrossbreedEmiRecipe create(EmiRecipeCategory category, CropBreeding.Mutation mutation) {
        CropCard child = CropCards.find(mutation.child()).orElse(null);
        if (child == null) {
            return null;
        }
        List<EmiStack> parents = new ArrayList<>();
        for (String parentId : mutation.parents()) {
            CropCard parent = CropCards.find(parentId).orElse(null);
            if (parent == null) {
                return null;
            }
            parents.add(CropEmiStacks.seed(parent));
        }
        return new CrossbreedEmiRecipe(category, mutation, parents, child);
    }

    private CrossbreedEmiRecipe(
            EmiRecipeCategory category,
            CropBreeding.Mutation mutation,
            List<EmiStack> parents,
            CropCard childCard) {
        super(category, id(mutation), EmiPages.BREEDING_WIDTH, 1);
        this.parents = List.copyOf(parents);
        this.child = CropEmiStacks.seed(childCard);
        catalysts.addAll(this.parents);
        outputs.add(this.child);

        CropConditions conditions = childCard.conditions();
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

        lines = lines(mutation, conditions, soil, subSoil);
        height = EmiPages.BREED_TEXT_Y + lines.size() * EmiPages.LINE;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        boolean wide = parents.size() > 2;
        int[] parentX = wide ? EmiPages.BREED4_PARENT_X : EmiPages.BREED2_PARENT_X;
        int centerX = wide ? EmiPages.BREED4_CENTER_X : EmiPages.BREED2_CENTER_X;
        EmiPages.image(
                widgets,
                wide ? EmiPages.BREEDING_4 : EmiPages.BREEDING_2,
                wide ? EmiPages.BREEDING_4_W : EmiPages.BREEDING_2_W,
                EmiPages.BREED_ART_H);
        int shown = Math.min(parents.size(), parentX.length);
        for (int index = 0; index < shown; index++) {
            EmiPages.item(widgets, parents.get(index), parentX[index], EmiPages.BREED_Y_SEED).catalyst(true);
        }
        EmiPages.item(widgets, child, centerX, EmiPages.BREED_Y_SEED).recipeContext(this);
        if (!soil.isEmpty()) {
            EmiPages.item(widgets, soil, centerX, EmiPages.BREED_Y_SOIL).catalyst(true);
        }
        if (!subSoil.isEmpty()) {
            EmiPages.item(widgets, subSoil, centerX, EmiPages.BREED_Y_BASE).catalyst(true);
        }
        for (int index = 0; index < lines.size(); index++) {
            EmiPages.line(
                    widgets,
                    lines.get(index),
                    0,
                    EmiPages.BREED_TEXT_Y + EmiPages.LINE * index,
                    EmiPages.BREEDING_WIDTH);
        }
    }

    private static List<Component> lines(
            CropBreeding.Mutation mutation,
            CropConditions conditions,
            EmiIngredient soil,
            EmiIngredient subSoil) {
        List<Component> lines = new ArrayList<>();
        if (mutation.machineOnly()) {
            lines.add(Component.translatable("emi.gt6crops.cross.machine"));
        } else {
            CropBreeding.Variation variation = Gt6CropsConfig.breedingVariation();
            lines.add(Component.translatable("emi.gt6crops.cross.where"));
            lines.add(Component.translatable("emi.gt6crops.cross.when", Gt6CropsConfig.breedingChance()));
            lines.add(Component.translatable("emi.gt6crops.cross.order"));
            lines.add(Component.translatable(
                    "emi.gt6crops.cross.stats", variation.low(), variation.high()));
            lines.add(Component.translatable("emi.gt6crops.cross.fertilizer"));
            lines.add(Component.translatable("emi.gt6crops.cross.unscanned"));
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
        if (mutation.parents().size() > EmiPages.BREED4_PARENT_X.length) {
            lines.add(Component.translatable(
                    "emi.gt6crops.cross.too_many", EmiPages.BREED4_PARENT_X.length));
        }
        return List.copyOf(lines);
    }

    private static ResourceLocation id(CropBreeding.Mutation mutation) {
        String parents = mutation.parents().stream()
                .map(CropEmiStacks::safe)
                .reduce((left, right) -> left + "__" + right)
                .orElse("_");
        return ResourceLocation.fromNamespaceAndPath(
                Gt6Crops.MODID,
                "crossbreeding/" + CropEmiStacks.safe(mutation.child()) + "/" + parents);
    }
}
