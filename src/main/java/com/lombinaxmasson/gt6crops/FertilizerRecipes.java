package com.lombinaxmasson.gt6crops;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import gregtech.api.addon.GregTechAddon;
import gregtech.api.addon.platform.AddonFluids;
import gregtech.api.addon.platform.AddonRecipes;
import com.lombinaxmasson.gt6crops.card.ItemRef;
import com.lombinaxmasson.gt6crops.machine.CropManagerBehavior;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * GT6CE recipes for the Crop Manager's liquid and enriched fertilizer, after
 * CropsNH's {@code FertilizerRecipes}.
 *
 * <p>CropsNH makes fertilizer items and extracts them at 144 mB each. This
 * addon has no fertilizer item, so its Mixer rows make the liquid directly.
 * A row is skipped, with a warning, when GT6CE lacks an ingredient or already
 * has a recipe those inputs would run.</p>
 */
final class FertilizerRecipes {
    /** CropsNH's liquid fertilizer per fertilizer item. */
    private static final int MB_PER_FERTILIZER = 144;
    /** CropsNH spends five seconds per fertilizer item. */
    private static final int TICKS_PER_FERTILIZER = 100;
    private static final int EU_PER_TICK = 16;
    private static final int WATER_MB = 1000;
    /** The waters GT6CE's own Mixer rows accept; plain water is vanilla's. */
    private static final List<String> GT_WATERS = List.of("MnWtr", "DistW", "SpDew");
    /** CropsNH's second ingredients, Phosphorite standing in for its tricalcium phosphate. */
    private static final List<ItemRef> ADDITIVES = List.of(
            dust("Sulfur", 1), dust("Phosphorite", 1), dust("Phosphate", 1),
            dust("Ashes", 3), dust("DarkAshes", 1));
    private static final int SQUEEZE_TICKS = 16;
    private static final int ENRICHING_FERTILIZER_ITEMS = 4;
    private static final int ENRICHED_MB = 1000;
    private static final int ENRICHING_TICKS = 20;

    private final List<Fluid> waters = new ArrayList<>();
    private int added;
    private int skipped;

    private FertilizerRecipes() {
        waters.add(Fluids.WATER);
        for (String name : GT_WATERS) {
            FluidStack water = AddonFluids.still(name, WATER_MB);
            if (water != null && !water.isEmpty()) {
                waters.add(water.getFluid());
            } else {
                Gt6Crops.LOGGER.warn("gt6crops: GT6CE has no {} fluid for fertilizer recipes", name);
            }
        }
    }

    static void register(GregTechAddon.Context context) {
        FertilizerRecipes recipes = new FertilizerRecipes();
        recipes.mineral("Calcite", 2, 3, 2, 1, 1);
        recipes.mineral("Calcium", 3, 4, 3, 2, 2);
        recipes.mineral("Apatite", 3, 4, 3, 2, 2);
        recipes.mineral("Glauconite", 3, 4, 3, 2, 2);
        recipes.fromWater("organic fertilizer", 4, TICKS_PER_FERTILIZER,
                item("minecraft:dirt", 1), dust("Wood", 2), item("minecraft:sand", 4));
        recipes.boneMeal();
        recipes.enriched();
        Gt6Crops.LOGGER.info("gt6crops: added {} fertilizer recipes, skipped {} ({} {})",
                recipes.added, recipes.skipped, context.platform(), context.minecraftVersion());
    }

    /** {@code fertilizer[i]} is how many fertilizer items CropsNH makes from {@code base} and {@code ADDITIVES[i]}. */
    private void mineral(String base, int... fertilizer) {
        for (int i = 0; i < ADDITIVES.size(); i++) {
            ItemRef additive = ADDITIVES.get(i);
            fromWater(base + " + " + additive.material() + " fertilizer", fertilizer[i],
                    fertilizer[i] * TICKS_PER_FERTILIZER, dust(base, 1), additive);
        }
    }

    private void fromWater(String what, int fertilizer, int ticks, ItemRef... inputs) {
        for (Fluid water : waters) {
            Optional<ItemStack[]> items = resolve(inputs);
            if (items.isEmpty()) {
                Gt6Crops.LOGGER.warn("gt6crops: skipped the {} recipe, GT6CE lacks one of {}",
                        what, Arrays.toString(inputs));
                skipped++;
                return;
            }
            add(AddonRecipes.map("gt.recipe.mixer"), what, new AddonRecipes.Line(
                    items.get(), null,
                    new FluidStack[] {new FluidStack(water, WATER_MB)},
                    new FluidStack[] {fertilizer(fertilizer * MB_PER_FERTILIZER)},
                    ticks, EU_PER_TICK, 0));
        }
    }

    private void boneMeal() {
        add(AddonRecipes.map("gt.recipe.squeezer"), "bone meal", new AddonRecipes.Line(
                new ItemStack[] {boneMeal(1)}, null, null,
                new FluidStack[] {fertilizer(CropManagerBehavior.FERTILIZER_ITEM_MB)},
                SQUEEZE_TICKS, EU_PER_TICK, 0));
    }

    private void enriched() {
        add(AddonRecipes.map("gt.recipe.mixer"), "enriched fertilizer", new AddonRecipes.Line(
                new ItemStack[] {boneMeal(ENRICHING_FERTILIZER_ITEMS)}, null,
                new FluidStack[] {fertilizer(ENRICHED_MB)},
                new FluidStack[] {new FluidStack(Gt6Crops.ENRICHED_FERTILIZER.source(), ENRICHED_MB)},
                ENRICHING_TICKS, EU_PER_TICK, 0));
    }

    private void add(AddonRecipes.Map map, String what, AddonRecipes.Line line) {
        AddonRecipes.Line existing = map.collision(line);
        if (existing != null) {
            Gt6Crops.LOGGER.warn("gt6crops: skipped the {} {} recipe, its inputs already run {} + {}",
                    map.name(), what, Arrays.toString(existing.itemInputs()),
                    Arrays.toString(existing.fluidInputs()));
            skipped++;
            return;
        }
        GtRecipeIntegration.requireAccepted(map, line, what);
        added++;
    }

    private static Optional<ItemStack[]> resolve(ItemRef... refs) {
        ItemStack[] stacks = new ItemStack[refs.length];
        for (int i = 0; i < refs.length; i++) {
            Optional<ItemStack> stack = refs[i].resolve();
            if (stack.isEmpty()) {
                return Optional.empty();
            }
            stacks[i] = stack.get();
        }
        return Optional.of(stacks);
    }

    private static FluidStack fertilizer(int amount) {
        return new FluidStack(Gt6Crops.FERTILIZER.source(), amount);
    }

    private static ItemStack boneMeal(int count) {
        return new ItemStack(Items.BONE_MEAL, count);
    }

    private static ItemRef dust(String material, int count) {
        return new ItemRef("gt_form", null, "dust", material, count);
    }

    private static ItemRef item(String id, int count) {
        return new ItemRef("vanilla", id, null, null, count);
    }
}
