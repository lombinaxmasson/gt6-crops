package com.lombinaxmasson.gt6crops.gametest;

import java.util.List;

import com.gregtech.gregtech.api.addon.platform.AddonRecipes;
import com.lombinaxmasson.gt6crops.SeedScanRecipes;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Plantalyzer scans one gt6crops seed, and hands an already scanned seed back. */
@GameTestHolder("gt6crops")
@PrefixGameTestTemplate(false)
public final class SeedScanRecipeTests {
    private SeedScanRecipeTests() {}

    private static CropCard crop() {
        for (CropCard card : CropCards.cards()) {
            if (!card.isWeed()) {
                return card;
            }
        }
        throw new IllegalStateException("no crop card");
    }

    private static AddonRecipes.Line found(ItemStack seed) {
        return AddonRecipes.map("gt.recipe.plantalyzer").find(List.of(seed), List.of(), false, 2, 2);
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void anUnscannedSeedIsScanned(GameTestHelper helper) {
        CropCard card = crop();
        ItemStack seed = CropSeedItem.create(card, new CropRules.Stats(7, 8, 9), false);
        seed.setCount(4);
        AddonRecipes.Line recipe = found(seed);
        helper.assertTrue(recipe != null, "the Plantalyzer accepts an unscanned seed");
        helper.assertTrue(recipe.duration() == SeedScanRecipes.SCAN_TICKS
                        && recipe.euPerTick() == SeedScanRecipes.SCAN_EU,
                "an unscanned seed takes 64 ticks at 16 EU/t");
        helper.assertTrue(recipe.itemInputs()[0].getCount() == 1 && recipe.itemOutputs()[0].getCount() == 1,
                "one seed is scanned out of the stack");
        var output = CropSeedItem.data(recipe.itemOutputs()[0]).orElseThrow();
        helper.assertTrue(output.scanned() && output.cardId().equals(card.id())
                        && output.stats().growth() == 7
                        && output.stats().gain() == 8
                        && output.stats().resistance() == 9,
                "the output is the same seed, now scanned");
        helper.assertTrue(SeedScanRecipes.displayAccepted(),
                "a display row is published for the recipe viewer");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aScannedSeedPassesThrough(GameTestHelper helper) {
        CropCard card = crop();
        ItemStack seed = CropSeedItem.create(card, new CropRules.Stats(2, 3, 4), true);
        AddonRecipes.Line recipe = found(seed);
        helper.assertTrue(recipe != null, "the Plantalyzer accepts a scanned seed");
        helper.assertTrue(recipe.duration() == SeedScanRecipes.PASS_TICKS
                        && recipe.euPerTick() == SeedScanRecipes.PASS_EU,
                "a scanned seed leaves in 1 tick");
        var output = CropSeedItem.data(recipe.itemOutputs()[0]).orElseThrow();
        helper.assertTrue(output.scanned() && output.cardId().equals(card.id())
                        && output.stats().growth() == 2
                        && output.stats().gain() == 3
                        && output.stats().resistance() == 4,
                "the scanned seed comes back unchanged");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void otherItemsAreLeftAlone(GameTestHelper helper) {
        helper.assertTrue(found(new ItemStack(Items.STICK)) == null,
                "a stick is not a seed scan");
        helper.succeed();
    }
}
