package com.lombinaxmasson.gt6crops.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import gregtech.api.addon.platform.MachineBehavior;
import gregtech.api.addon.platform.MachineBehaviorContext;
import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.Gt6CropsTags;
import com.lombinaxmasson.gt6crops.block.CropTile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * CropsNH's Crop Manager on GregTech's behavior shell. Every cycle it waters, sprays and
 * fertilizes the crop sticks around it, as its switches allow, then harvests every ripe crop.
 * The shell keeps no power: a tick needs a packet, and a tick without one starts the cycle over.
 *
 * <p>Like CropsNH's, it sprays GT's strong poison as Weed-EX, and its fertilizer tank
 * takes enriched fertilizer as ten times its volume of liquid fertilizer.</p>
 */
public final class CropManagerBehavior implements MachineBehavior {
    public static final int WEED_EX_SLOTS = 2;
    public static final int FERTILIZER_SLOTS = 4;
    public static final int OUTPUT_SLOTS = 18;
    public static final int FERTILIZER_START = WEED_EX_SLOTS;
    public static final int OUTPUT_START = FERTILIZER_START + FERTILIZER_SLOTS;
    public static final int SLOTS = OUTPUT_START + OUTPUT_SLOTS;

    public static final int HARVEST = 0;
    public static final int WATER = 1;
    public static final int FERTILIZER = 2;
    public static final int WEED_EX = 3;

    public static final int WATER_TANK = 0;
    public static final int FERTILIZER_TANK = 1;
    public static final int WEED_EX_TANK = 2;

    /** GT6CE's strong poison, the fluid in CropsNH's Weed-EX cans. */
    private static final ResourceLocation STRONG_POISON =
            ResourceLocation.fromNamespaceAndPath("gt6ce", "potion_poison_strong");

    /** CropsNH's global update rate. */
    private static final int CYCLE_TICKS = 50;
    public static final int VERTICAL_RADIUS = 2;
    private static final double HARVEST_BONUS_PER_TIER = 0.05;
    public static final int WATER_PER_TIER = 32_000;
    /** Four stacks of 144 mB per tier. */
    public static final int FERTILIZER_PER_TIER = 36_864;
    private static final int ENRICHED_STRENGTH = 10;
    /** CropsNH holds two cans of 750 mB per tier. */
    public static final int WEED_EX_PER_TIER = 1500;

    // CropsNH's thresholds and doses on its 200-point stores, scaled to our 32 points.
    /** CropsNH spends 1 mB of water or liquid fertilizer per point; one of our points is 6.25 of its. */
    private static final int MB_PER_POINT = 6;
    /** CropsNH tops water and liquid fertilizer up from 180 of 200. */
    private static final int TOP_UP_THRESHOLD = 28;
    /** One fertilizer item, as by hand. CropsNH applies an item once at most half of it would be wasted. */
    private static final int FERTILIZER_DOSE = 8;
    private static final int FERTILIZER_THRESHOLD =
            Math.max(CropTile.MAX_STORAGE / 2, CropTile.MAX_STORAGE - FERTILIZER_DOSE / 2);
    /** The liquid fertilizer that does what one fertilizer item does. */
    public static final int FERTILIZER_ITEM_MB = FERTILIZER_DOSE * MB_PER_POINT;
    private static final int WEED_EX_THRESHOLD = 12;
    private static final int WEED_EX_DOSE = 12;
    private static final int WEED_EX_CAP = 24;
    private static final int WEED_EX_COST = 10;
    /** Provisional: the strong poison one Weed-EX item puts in the tank. */
    private static final int WEED_EX_PER_ITEM = 100;

    /** Not saved. The old machine did not write its cycle, so a reload starts over. */
    private int cycleTicks;

    public static int horizontalRadius(MachineTier tier) {
        return 3 + 2 * tier.index();
    }

    public static int harvestBonusPercent(MachineTier tier) {
        return (int) Math.round(HARVEST_BONUS_PER_TIER * 100 * tier.index());
    }

    public static int waterCapacity(MachineTier tier) {
        return WATER_PER_TIER * tier.index();
    }

    public static int fertilizerCapacity(MachineTier tier) {
        return FERTILIZER_PER_TIER * tier.index();
    }

    public static int weedExCapacity(MachineTier tier) {
        return WEED_EX_PER_TIER * tier.index();
    }

    @Override
    public void tick(MachineBehaviorContext context) {
        MachineTier tier = MachineTier.byIndex(context.tier());
        if (context.energy() < tier.inputMin()) {
            cycleTicks = 0;
            context.progress(0);
            return;
        }
        context.working(true);
        if (++cycleTicks >= CYCLE_TICKS) {
            cycleTicks = 0;
            runCycle(context, tier);
        }
        context.progress(cycleTicks / (float) CYCLE_TICKS);
    }

    @Override
    public boolean canInsert(MachineBehaviorContext context, int slot, ItemStack stack) {
        if (slot < FERTILIZER_START) {
            return stack.is(Gt6Crops.WEED_EX.get());
        }
        if (slot < OUTPUT_START) {
            return stack.is(Gt6CropsTags.FERTILIZER);
        }
        return false;
    }

    @Override
    public boolean canFill(MachineBehaviorContext context, int tank, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (tank == WATER_TANK) {
            return stack.is(Fluids.WATER);
        }
        if (tank == FERTILIZER_TANK) {
            return stack.is(Gt6Crops.FERTILIZER.source()) || stack.is(Gt6Crops.ENRICHED_FERTILIZER.source());
        }
        if (tank == WEED_EX_TANK) {
            Fluid poison = strongPoison();
            return poison != Fluids.EMPTY && stack.is(poison);
        }
        return false;
    }

    @Override
    public int acceptFluid(MachineBehaviorContext context, int tank, FluidStack stack, boolean simulate) {
        if (tank == FERTILIZER_TANK && stack != null && stack.is(Gt6Crops.ENRICHED_FERTILIZER.source())) {
            return acceptEnriched(context, stack, simulate);
        }
        return MachineBehavior.super.acceptFluid(context, tank, stack, simulate);
    }

    /** A concentrate is taken whole millibuckets at a time and stored as ten times that of fertilizer. */
    private static int acceptEnriched(MachineBehaviorContext context, FluidStack stack, boolean simulate) {
        FluidStack held = context.fluid(FERTILIZER_TANK);
        if (!held.isEmpty() && !held.is(Gt6Crops.FERTILIZER.source())) {
            return 0;
        }
        int room = context.capacity(FERTILIZER_TANK) - held.getAmount();
        int taken = Math.min(stack.getAmount(), room / ENRICHED_STRENGTH);
        if (taken <= 0) {
            return 0;
        }
        int stored = context.fill(FERTILIZER_TANK,
                new FluidStack(Gt6Crops.FERTILIZER.source(), taken * ENRICHED_STRENGTH), simulate);
        return stored > 0 ? taken : 0;
    }

    /** CropsNH tends every crop stick in range, weeds and empty sticks too, before it harvests. */
    private void runCycle(MachineBehaviorContext context, MachineTier tier) {
        List<CropTile> crops = cropsInRange(context, tier);
        if (context.toggle(WEED_EX)) {
            refillWeedEx(context);
        }
        for (CropTile crop : crops) {
            if (context.toggle(WATER)) {
                topUp(context, WATER_TANK, crop.hydrationStorage(), crop::addWater);
            }
            if (context.toggle(WEED_EX)) {
                sprayWeedEx(context, crop);
            }
            if (context.toggle(FERTILIZER)) {
                fertilize(context, crop);
            }
        }
        if (context.toggle(HARVEST)) {
            harvest(context, tier, crops);
        }
    }

    private static List<CropTile> cropsInRange(MachineBehaviorContext context, MachineTier tier) {
        Level level = context.level();
        int horizontal = horizontalRadius(tier);
        BlockPos origin = context.pos();
        List<CropTile> crops = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-horizontal, -VERTICAL_RADIUS, -horizontal),
                origin.offset(horizontal, VERTICAL_RADIUS, horizontal))) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CropTile crop) {
                crops.add(crop);
            }
        }
        return crops;
    }

    /** {@code add} stores up to the points it is given and returns how many it took. */
    private static void topUp(MachineBehaviorContext context, int tank, int stored, IntUnaryOperator add) {
        if (stored > TOP_UP_THRESHOLD) {
            return;
        }
        int points = Math.min(CropTile.MAX_STORAGE - stored, context.fluid(tank).getAmount() / MB_PER_POINT);
        if (points <= 0) {
            return;
        }
        context.drain(tank, add.applyAsInt(points) * MB_PER_POINT, false);
    }

    private void sprayWeedEx(MachineBehaviorContext context, CropTile crop) {
        if (crop.weedExStorage() > WEED_EX_THRESHOLD) {
            return;
        }
        if (context.fluid(WEED_EX_TANK).getAmount() < WEED_EX_COST) {
            refillWeedEx(context);
        }
        if (context.fluid(WEED_EX_TANK).getAmount() < WEED_EX_COST) {
            return;
        }
        context.drain(WEED_EX_TANK, WEED_EX_COST, false);
        crop.addWeedEx(Math.min(WEED_EX_DOSE, WEED_EX_CAP - crop.weedExStorage()));
    }

    /** Pours Weed-EX items into the tank, one whole item at a time. */
    private void refillWeedEx(MachineBehaviorContext context) {
        Fluid poison = strongPoison();
        if (poison == Fluids.EMPTY) {
            return;
        }
        FluidStack dose = new FluidStack(poison, WEED_EX_PER_ITEM);
        for (int slot = 0; slot < FERTILIZER_START; slot++) {
            while (!context.item(slot).isEmpty()
                    && context.fill(WEED_EX_TANK, dose, true) == WEED_EX_PER_ITEM) {
                shrink(context, slot, 1);
                context.fill(WEED_EX_TANK, dose, false);
            }
        }
    }

    /** CropsNH uses liquid fertilizer while it has any, and items only once it runs dry. */
    private static void fertilize(MachineBehaviorContext context, CropTile crop) {
        if (context.fluid(FERTILIZER_TANK).getAmount() >= MB_PER_POINT) {
            topUp(context, FERTILIZER_TANK, crop.nutrientStorage(), crop::addFertilizer);
            return;
        }
        if (crop.nutrientStorage() > FERTILIZER_THRESHOLD) {
            return;
        }
        for (int slot = FERTILIZER_START; slot < OUTPUT_START; slot++) {
            if (!context.item(slot).isEmpty()) {
                shrink(context, slot, 1);
                crop.addFertilizer(FERTILIZER_DOSE);
                return;
            }
        }
    }

    /**
     * CropsNH skips the harvest while the output is full or last harvest's leftovers
     * still do not fit, so at most one harvest waits outside the slots.
     */
    private static void harvest(MachineBehaviorContext context, MachineTier tier, List<CropTile> crops) {
        if (!hasOutputRoom(context)) {
            return;
        }
        List<ItemStack> overflow = readOverflow(context);
        List<ItemStack> stillWaiting = new ArrayList<>();
        for (ItemStack stack : overflow) {
            ItemStack left = context.insertOutput(stack);
            if (!left.isEmpty()) {
                stillWaiting.add(left);
            }
        }
        if (!stillWaiting.isEmpty()) {
            writeOverflow(context, stillWaiting);
            return;
        }
        double multiplier = 1.0 + HARVEST_BONUS_PER_TIER * tier.index();
        List<ItemStack> leftovers = new ArrayList<>();
        for (CropTile crop : crops) {
            for (ItemStack drop : crop.machineHarvest(multiplier)) {
                ItemStack left = context.insertOutput(drop);
                if (!left.isEmpty()) {
                    leftovers.add(left);
                }
            }
        }
        writeOverflow(context, leftovers);
    }

    private static boolean hasOutputRoom(MachineBehaviorContext context) {
        for (int slot = OUTPUT_START; slot < SLOTS; slot++) {
            ItemStack stack = context.item(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private static List<ItemStack> readOverflow(MachineBehaviorContext context) {
        int count = context.state().getInt("overflow");
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = context.state().item("overflow." + i);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static void writeOverflow(MachineBehaviorContext context, List<ItemStack> stacks) {
        int previous = context.state().getInt("overflow");
        for (int i = 0; i < previous; i++) {
            context.state().setItem("overflow." + i, ItemStack.EMPTY);
        }
        int kept = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                context.state().setItem("overflow." + kept, stack);
                kept++;
            }
        }
        context.state().setInt("overflow", kept);
    }

    private static void shrink(MachineBehaviorContext context, int slot, int count) {
        ItemStack stack = context.item(slot);
        int left = stack.getCount() - count;
        context.setItem(slot, left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left));
    }

    private static Fluid strongPoison() {
        Fluid fluid = BuiltInRegistries.FLUID.get(STRONG_POISON);
        return fluid == null ? Fluids.EMPTY : fluid;
    }
}
