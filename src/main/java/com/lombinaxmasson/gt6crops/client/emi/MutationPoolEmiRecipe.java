package com.lombinaxmasson.gt6crops.client.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.Gt6CropsConfig;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.rules.CropBreeding;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * CropsNH's mutation-pool page. Nine columns, a frame tiled from
 * {@code mutation_pool.png}, and the pool name under the bottom cap.
 * Any member can be a parent or the result, so each seed is both.
 */
final class MutationPoolEmiRecipe extends CropEmiRecipe {
    private final List<EmiStack> members;
    private final int rows;
    private final List<Component> lines;

    static MutationPoolEmiRecipe create(EmiRecipeCategory category, CropBreeding.Pool pool) {
        List<EmiStack> members = new ArrayList<>();
        for (String cropId : pool.crops()) {
            CropCards.find(cropId).map(CropEmiStacks::seed).ifPresent(members::add);
        }
        if (members.size() < 2) {
            return null;
        }
        return new MutationPoolEmiRecipe(category, pool, members);
    }

    private MutationPoolEmiRecipe(
            EmiRecipeCategory category, CropBreeding.Pool pool, List<EmiStack> members) {
        super(category, id(pool), EmiPages.POOL_WIDTH, 1);
        this.members = List.copyOf(members);
        this.rows = EmiPages.poolRows(this.members.size());
        inputs.addAll(this.members);
        outputs.addAll(this.members);
        lines = List.of(
                Component.translatable("emi.gt6crops.pool.line", name(pool.id())),
                Component.translatable("emi.gt6crops.pool.where"),
                Component.translatable("emi.gt6crops.pool.when", Gt6CropsConfig.breedingChance()));
        int nameY = EmiPages.poolNameY(rows);
        height = Math.max(rows * 18 + 20, nameY + lines.size() * EmiPages.LINE);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        EmiPages.poolFrame(widgets, rows);
        for (int index = 0; index < members.size(); index++) {
            int x = EmiPages.POOL_X + 18 * (index % EmiPages.POOL_COLUMNS);
            int y = EmiPages.POOL_Y + 18 * (index / EmiPages.POOL_COLUMNS);
            EmiPages.item(widgets, members.get(index), x, y).recipeContext(this);
        }
        int nameY = EmiPages.poolNameY(rows);
        for (int index = 0; index < lines.size(); index++) {
            EmiPages.line(widgets, lines.get(index), 0, nameY + EmiPages.LINE * index, EmiPages.POOL_WIDTH);
        }
    }

    static Component name(String id) {
        return Component.translatableWithFallback("emi.gt6crops.pool." + id, words(id));
    }

    private static String words(String id) {
        String[] parts = id.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? id : builder.toString();
    }

    private static ResourceLocation id(CropBreeding.Pool pool) {
        return ResourceLocation.fromNamespaceAndPath(
                Gt6Crops.MODID, "mutation_pool/" + CropEmiStacks.safe(pool.id()));
    }
}
