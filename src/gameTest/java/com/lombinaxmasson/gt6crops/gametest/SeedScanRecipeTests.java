package com.lombinaxmasson.gt6crops.gametest;

import java.util.List;
import java.util.stream.StreamSupport;

import com.gregtech.gregtech.api.recipe.Recipe;
import com.gregtech.gregtech.block.machine.BasicMachineBlock;
import com.gregtech.gregtech.blockentity.machine.BasicMachineBlockEntity;
import com.gregtech.gregtech.data.GregTechTags;
import com.gregtech.gregtech.data.MachineRecipeMaps;
import com.lombinaxmasson.gt6crops.SeedScanRecipes;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    private static Recipe found(ItemStack seed) {
        return MachineRecipeMaps.Plantalyzer.findRecipe(List.of(seed), List.of(), false, 2, 2);
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void anUnscannedSeedIsScanned(GameTestHelper helper) {
        CropCard card = crop();
        ItemStack seed = CropSeedItem.create(card, new CropRules.Stats(7, 8, 9), false);
        seed.setCount(4);
        Recipe recipe = found(seed);
        helper.assertTrue(recipe != null, "the Plantalyzer accepts an unscanned seed");
        helper.assertTrue(recipe.mDuration == SeedScanRecipes.SCAN_TICKS
                        && recipe.mEUt == SeedScanRecipes.SCAN_EU,
                "an unscanned seed takes 64 ticks at 16 EU/t");
        helper.assertTrue(recipe.mInputs[0].getCount() == 1 && recipe.mOutputs[0].getCount() == 1,
                "one seed is scanned out of the stack");
        var output = CropSeedItem.data(recipe.mOutputs[0]).orElseThrow();
        helper.assertTrue(output.scanned() && output.cardId().equals(card.id())
                        && output.stats().growth() == 7
                        && output.stats().gain() == 8
                        && output.stats().resistance() == 9,
                "the output is the same seed, now scanned");
        helper.assertTrue(MachineRecipeMaps.Plantalyzer.mRecipeList.stream().anyMatch(row -> row.mFakeRecipe),
                "a display row is published for the recipe viewer");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 100)
    public static void aScannedSeedPassesThrough(GameTestHelper helper) {
        CropCard card = crop();
        ItemStack seed = CropSeedItem.create(card, new CropRules.Stats(2, 3, 4), true);
        Recipe recipe = found(seed);
        helper.assertTrue(recipe != null, "the Plantalyzer accepts a scanned seed");
        helper.assertTrue(recipe.mDuration == SeedScanRecipes.PASS_TICKS
                        && recipe.mEUt == SeedScanRecipes.PASS_EU,
                "a scanned seed leaves in 1 tick");
        var output = CropSeedItem.data(recipe.mOutputs[0]).orElseThrow();
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

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void aPoweredPlantalyzerScansOneSeed(GameTestHelper helper) {
        CropCard card = crop();
        ItemStack seed = CropSeedItem.create(card, new CropRules.Stats(7, 8, 9), false);
        BasicMachineBlockEntity machine = plantalyzer(helper);
        machine.inventory().setStackInSlot(0, seed);
        int outputSlot = machine.inputSlots();
        for (int tick = 1; tick <= 63; tick++) {
            feed(helper, machine);
            helper.assertTrue(machine.inventory().getStackInSlot(outputSlot).isEmpty(),
                    "the seed is still being scanned at tick " + tick);
        }
        helper.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(),
                "the unscanned seed was taken from the input");
        feed(helper, machine);
        ItemStack out = machine.inventory().getStackInSlot(outputSlot);
        helper.assertTrue(!out.isEmpty(), "the scanned seed is ready after 64 minimum packets");
        var data = CropSeedItem.data(out).orElseThrow();
        helper.assertTrue(data.scanned() && data.cardId().equals(card.id())
                        && data.stats().growth() == 7
                        && data.stats().gain() == 8
                        && data.stats().resistance() == 9
                        && out.getCount() == 1,
                "the powered Plantalyzer returns the same seed, scanned");
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 200)
    public static void aPoweredPlantalyzerReturnsAScannedSeed(GameTestHelper helper) {
        CropCard card = crop();
        ItemStack seed = CropSeedItem.create(card, new CropRules.Stats(2, 3, 4), true);
        BasicMachineBlockEntity machine = plantalyzer(helper);
        machine.inventory().setStackInSlot(0, seed);
        feed(helper, machine);
        helper.assertTrue(machine.inventory().getStackInSlot(0).isEmpty(),
                "the scanned seed left the input");
        ItemStack out = machine.inventory().getStackInSlot(machine.inputSlots());
        helper.assertTrue(!out.isEmpty(), "one minimum packet is enough to hand a scanned seed back");
        var data = CropSeedItem.data(out).orElseThrow();
        helper.assertTrue(data.scanned() && data.cardId().equals(card.id())
                        && data.stats().growth() == 2
                        && data.stats().gain() == 3
                        && data.stats().resistance() == 4
                        && out.getCount() == 1,
                "the powered Plantalyzer returns the scanned seed unchanged");
        helper.succeed();
    }

    /** LV Plantalyzer. Its minimum packet is 16 EU, so a 16 EU/t recipe advances one tick per packet. */
    private static BasicMachineBlockEntity plantalyzer(GameTestHelper helper) {
        BasicMachineBlock block = StreamSupport.stream(BuiltInRegistries.BLOCK.spliterator(), false)
                .filter(candidate -> candidate instanceof BasicMachineBlock machine
                        && machine.basicSpec().recipeMap() == MachineRecipeMaps.Plantalyzer
                        && machine.basicSpec().tier() == 1)
                .map(candidate -> (BasicMachineBlock) candidate)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no LV Plantalyzer"));
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        var level = helper.getLevel();
        level.setBlockAndUpdate(origin, block.defaultBlockState());
        // A free output face drops the finished stack into the world. Stone keeps it in the slot.
        for (Direction side : Direction.values()) {
            BlockPos neighbor = origin.relative(side);
            level.getChunk(neighbor);
            level.setBlockAndUpdate(neighbor, Blocks.STONE.defaultBlockState());
        }
        BlockEntity entity = level.getBlockEntity(origin);
        helper.assertTrue(entity instanceof BasicMachineBlockEntity, "the Plantalyzer was placed");
        BasicMachineBlockEntity machine = (BasicMachineBlockEntity) entity;
        helper.assertTrue(machine.spec().energyInMin() == SeedScanRecipes.SCAN_EU
                        && machine.spec().energyInMax() == 64,
                "the LV Plantalyzer accepts 16 to 64 EU per tick");
        return machine;
    }

    private static void feed(GameTestHelper helper, BasicMachineBlockEntity machine) {
        long accepted = machine.doEnergyInjection(
                GregTechTags.Energy.EU, null, machine.spec().energyInMin(), 1, true);
        helper.assertTrue(accepted == 1, "the LV Plantalyzer accepts one minimum packet");
        BasicMachineBlockEntity.serverTick(
                helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }
}
