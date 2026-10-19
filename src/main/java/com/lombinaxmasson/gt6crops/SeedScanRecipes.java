package com.lombinaxmasson.gt6crops;

import java.util.List;

import com.gregtech.gregtech.api.addon.platform.AddonRecipes;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Plantalyzer rows for the one crop-seed item. Species and stats live on the stack, so a static
 * row cannot name them. An unscanned seed is scanned in place; an already scanned seed is handed
 * back unchanged.
 */
public final class SeedScanRecipes {
    /** Same duration and power as the Bumblelyzer's scan. The LV Plantalyzer's minimum is 16 EU/t. */
    public static final long SCAN_TICKS = 64;
    public static final long SCAN_EU = 16;
    /** An already scanned seed leaves on the next tick, still at the machine's minimum power. */
    public static final long PASS_TICKS = 1;
    public static final long PASS_EU = 16;

    private static boolean registered;
    private static boolean displayAccepted;

    private SeedScanRecipes() {}

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        AddonRecipes.Map plantalyzer = AddonRecipes.map("gt.recipe.plantalyzer");
        plantalyzer.addDynamic(SeedScanRecipes::find);
        registerDisplay(plantalyzer);
    }

    /** True when the recipe viewer accepted the one example row. Machines do not craft that row. */
    public static boolean displayAccepted() {
        return displayAccepted;
    }

    /**
     * The first crop seed in the input slots. Anything else is left for a later provider.
     * The recipe consumes one item and returns one, so a stack is scanned one seed at a time.
     */
    public static AddonRecipes.Line find(List<ItemStack> items, List<FluidStack> fluids) {
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            var data = CropSeedItem.data(stack);
            if (data.isEmpty()) {
                continue;
            }
            var card = CropCards.find(data.get().cardId());
            if (card.isEmpty()) {
                continue;
            }
            ItemStack one = stack.copyWithCount(1);
            if (data.get().scanned()) {
                return row(one, one.copy(), PASS_TICKS, PASS_EU);
            }
            ItemStack scanned = CropSeedItem.create(card.get(), data.get().stats(), true);
            return row(one, scanned, SCAN_TICKS, SCAN_EU);
        }
        return null;
    }

    private static void registerDisplay(AddonRecipes.Map plantalyzer) {
        for (CropCard card : CropCards.cards()) {
            if (card.isWeed()) {
                continue;
            }
            CropRules.Stats example = new CropRules.Stats(1, 1, 1);
            ItemStack input = CropSeedItem.create(card, example, false);
            ItemStack output = CropSeedItem.create(card, example, true);
            AddonRecipes.Line shown = plantalyzer.addDisplay(row(input, output, SCAN_TICKS, SCAN_EU));
            displayAccepted = shown != null;
            if (!displayAccepted) {
                Gt6Crops.LOGGER.warn("gt6crops: Plantalyzer rejected the seed-scan display row");
            }
            return;
        }
    }

    private static AddonRecipes.Line row(ItemStack input, ItemStack output, long ticks, long eut) {
        return new AddonRecipes.Line(
                new ItemStack[] {input},
                new ItemStack[] {output},
                null, null, ticks, eut, 0);
    }
}
