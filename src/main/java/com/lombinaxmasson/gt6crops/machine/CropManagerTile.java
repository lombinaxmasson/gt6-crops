package com.lombinaxmasson.gt6crops.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.Gt6CropsTags;
import com.lombinaxmasson.gt6crops.block.CropTile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * CropsNH's Crop Manager. Every cycle it waters, sprays and fertilizes the crop sticks
 * around it, as its switches allow, then harvests every ripe crop into its output.
 * As a GT6 machine it stores no power: it needs power on every tick of the cycle,
 * and a tick without it starts the cycle over.
 *
 * <p>Like CropsNH's, it sprays GT's strong poison as Weed-EX, and its fertilizer tank
 * takes enriched fertilizer as ten times its volume of liquid fertilizer.</p>
 */
public final class CropManagerTile extends MachineTile {
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
    private static final int TOGGLES = 4;

    public static final int WATER_TANK = 0;
    public static final int FERTILIZER_TANK = 1;
    public static final int WEED_EX_TANK = 2;

    /** GT6CE's strong poison, the fluid in CropsNH's Weed-EX cans. */
    private static final ResourceLocation STRONG_POISON =
            ResourceLocation.fromNamespaceAndPath("gregtech", "potion_poison_strong");

    /** CropsNH's global update rate. */
    private static final int CYCLE_TICKS = 50;
    private static final int VERTICAL_RADIUS = 2;
    private static final double HARVEST_BONUS_PER_TIER = 0.05;
    private static final int WATER_PER_TIER = 32_000;
    /** Four stacks of 144 mB per tier. */
    private static final int FERTILIZER_PER_TIER = 36_864;
    private static final int ENRICHED_STRENGTH = 10;
    /** CropsNH holds two cans of 750 mB per tier. */
    private static final int WEED_EX_PER_TIER = 1500;

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

    private final Tank water;
    private final Tank fertilizer;
    private final Tank weedEx;
    private final List<ItemStack> overflow = new ArrayList<>();
    private int cycleTicks;

    public CropManagerTile(BlockPos pos, BlockState state) {
        super(Gt6Crops.CROP_MANAGER_TILE.get(), pos, state, ((MachineBlock) state.getBlock()).tier(), SLOTS);
        int tier = tier().index();
        water = addTank(WATER_PER_TIER * tier, () -> Fluids.WATER);
        fertilizer = addTank(FERTILIZER_PER_TIER * tier, Gt6Crops.FERTILIZER::source,
                Gt6Crops.ENRICHED_FERTILIZER::source, ENRICHED_STRENGTH);
        weedEx = addTank(WEED_EX_PER_TIER * tier, () -> BuiltInRegistries.FLUID.get(STRONG_POISON));
        setOn(HARVEST, true);
    }

    private int horizontalRadius() {
        return 3 + 2 * tier().index();
    }

    @Override
    protected int toggleCount() {
        return TOGGLES;
    }

    @Override
    protected boolean canHold(int slot, ItemStack stack) {
        if (slot < FERTILIZER_START) {
            return stack.is(Gt6Crops.WEED_EX.get());
        }
        if (slot < OUTPUT_START) {
            return stack.is(Gt6CropsTags.FERTILIZER);
        }
        return true;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot < OUTPUT_START;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot >= OUTPUT_START;
    }

    @Override
    protected boolean work(Level level, long available) {
        if (++cycleTicks >= CYCLE_TICKS) {
            cycleTicks = 0;
            runCycle(level);
        }
        return true;
    }

    @Override
    protected void unpowered() {
        cycleTicks = 0;
    }

    @Override
    protected float progress() {
        return cycleTicks / (float) CYCLE_TICKS;
    }

    /** CropsNH tends every crop stick in range, weeds and empty sticks too, before it harvests. */
    private void runCycle(Level level) {
        List<CropTile> crops = cropsInRange(level);
        if (isOn(WEED_EX)) {
            refillWeedEx();
        }
        for (CropTile crop : crops) {
            if (isOn(WATER)) {
                water(crop);
            }
            if (isOn(WEED_EX)) {
                sprayWeedEx(crop);
            }
            if (isOn(FERTILIZER)) {
                fertilize(crop);
            }
        }
        if (isOn(HARVEST)) {
            harvest(crops);
        }
    }

    private List<CropTile> cropsInRange(Level level) {
        int h = horizontalRadius();
        List<CropTile> crops = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                worldPosition.offset(-h, -VERTICAL_RADIUS, -h),
                worldPosition.offset(h, VERTICAL_RADIUS, h))) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CropTile crop) {
                crops.add(crop);
            }
        }
        return crops;
    }

    private void water(CropTile crop) {
        topUp(water, crop.hydrationStorage(), crop::addWater);
    }

    /** {@code add} stores up to the points it is given and returns how many it took. */
    private static void topUp(Tank tank, int stored, IntUnaryOperator add) {
        if (stored > TOP_UP_THRESHOLD) {
            return;
        }
        int points = Math.min(CropTile.MAX_STORAGE - stored, tank.getFluidAmount() / MB_PER_POINT);
        if (points > 0) {
            tank.drain(add.applyAsInt(points) * MB_PER_POINT, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void sprayWeedEx(CropTile crop) {
        if (crop.weedExStorage() > WEED_EX_THRESHOLD) {
            return;
        }
        if (weedEx.getFluidAmount() < WEED_EX_COST) {
            refillWeedEx();
        }
        if (weedEx.getFluidAmount() < WEED_EX_COST) {
            return;
        }
        weedEx.drain(WEED_EX_COST, IFluidHandler.FluidAction.EXECUTE);
        crop.addWeedEx(Math.min(WEED_EX_DOSE, WEED_EX_CAP - crop.weedExStorage()));
    }

    /** Pours Weed-EX items into the tank, one whole item at a time. */
    private void refillWeedEx() {
        FluidStack dose = new FluidStack(weedEx.fluid(), WEED_EX_PER_ITEM);
        for (int slot = 0; slot < FERTILIZER_START; slot++) {
            while (!items().getStackInSlot(slot).isEmpty()
                    && weedEx.fill(dose, IFluidHandler.FluidAction.SIMULATE) == WEED_EX_PER_ITEM) {
                items().extractItem(slot, 1, false);
                weedEx.fill(dose, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /** CropsNH uses liquid fertilizer while it has any, and items only once it runs dry. */
    private void fertilize(CropTile crop) {
        if (fertilizer.getFluidAmount() >= MB_PER_POINT) {
            topUp(fertilizer, crop.nutrientStorage(), crop::addFertilizer);
            return;
        }
        if (crop.nutrientStorage() > FERTILIZER_THRESHOLD) {
            return;
        }
        for (int slot = FERTILIZER_START; slot < OUTPUT_START; slot++) {
            if (!items().getStackInSlot(slot).isEmpty()) {
                items().extractItem(slot, 1, false);
                crop.addFertilizer(FERTILIZER_DOSE);
                return;
            }
        }
    }

    /**
     * CropsNH skips the harvest while the output is full or last harvest's leftovers
     * still do not fit, so at most one harvest waits outside the slots.
     */
    private void harvest(List<CropTile> crops) {
        if (!hasOutputRoom()) {
            return;
        }
        overflow.replaceAll(this::insertOutput);
        overflow.removeIf(ItemStack::isEmpty);
        if (!overflow.isEmpty()) {
            setChanged();
            return;
        }
        double multiplier = 1.0 + HARVEST_BONUS_PER_TIER * tier().index();
        for (CropTile crop : crops) {
            for (ItemStack drop : crop.machineHarvest(multiplier)) {
                ItemStack left = insertOutput(drop);
                if (!left.isEmpty()) {
                    overflow.add(left);
                }
            }
        }
        setChanged();
    }

    private boolean hasOutputRoom() {
        for (int slot = OUTPUT_START; slot < SLOTS; slot++) {
            ItemStack stack = items().getStackInSlot(slot);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private ItemStack insertOutput(ItemStack stack) {
        for (int slot = OUTPUT_START; slot < SLOTS && !stack.isEmpty(); slot++) {
            stack = items().insertItem(slot, stack, false);
        }
        return stack;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        for (ItemStack stack : overflow) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CropManagerMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag overflowTags = new ListTag();
        for (ItemStack stack : overflow) {
            overflowTags.add(stack.save(registries));
        }
        tag.put("Overflow", overflowTags);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        overflow.clear();
        ListTag overflowTags = tag.getList("Overflow", Tag.TAG_COMPOUND);
        for (int i = 0; i < overflowTags.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, overflowTags.getCompound(i));
            if (!stack.isEmpty()) {
                overflow.add(stack);
            }
        }
    }
}
