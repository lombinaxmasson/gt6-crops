package com.lombinaxmasson.gt6crops.machine;

import java.util.Optional;

import com.gregtech.gregtech.api.recipe.MachineWorkCost;
import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * CropsNH's Seed Generator. It copies a scanned crop seed, keeping the original,
 * for enriched fertilizer by the seed's total stats and, for some crops, a catalyst item.
 *
 * <p>It works as GT6CE's basic machines do: the seed's fertilizer and catalyst are
 * spent when a copy starts, and the copy needs power on every tick; a tick without
 * enough starts it over, though nothing spent is lost.</p>
 */
public final class SeedGeneratorTile extends MachineTile {
    public static final int INPUT_SLOTS = 2;
    public static final int OUTPUT_SLOT = INPUT_SLOTS;
    public static final int SLOTS = OUTPUT_SLOT + 1;
    public static final int FERTILIZER_TANK = 0;

    /** CropsNH's LV recipe: 30 EU/t for 8 seconds. */
    private static final long RECIPE_EUT = 30;
    private static final long RECIPE_TICKS = 160;

    private final Tank fertilizer;
    private ItemStack pending = ItemStack.EMPTY;
    private long work;
    private long totalWork;
    private long minimumPower;

    public SeedGeneratorTile(BlockPos pos, BlockState state) {
        super(Gt6Crops.SEED_GENERATOR_TILE.get(), pos, state, ((MachineBlock) state.getBlock()).tier(), SLOTS);
        fertilizer = addTank(SeedGeneratorRules.capacity(tier()), Gt6Crops.ENRICHED_FERTILIZER::source);
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot < OUTPUT_SLOT;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    protected boolean work(Level level, long available) {
        if (pending.isEmpty() && !start()) {
            return false;
        }
        if (work < totalWork) {
            if (available < minimumPower) {
                work = 0;
                return false;
            }
            work = MachineWorkCost.advance(work, totalWork, Math.min(available, tier().inputMax()));
            setChanged();
            if (work < totalWork) {
                return true;
            }
        }
        ItemStack left = items().insertItem(OUTPUT_SLOT, pending, false);
        if (!left.isEmpty()) {
            return false;
        }
        pending = ItemStack.EMPTY;
        work = 0;
        totalWork = 0;
        setChanged();
        return true;
    }

    @Override
    protected void unpowered() {
        if (work < totalWork) {
            work = 0;
        }
    }

    @Override
    protected float progress() {
        return totalWork <= 0 ? 0.0F : work / (float) totalWork;
    }

    /** Spends what the copy needs and readies it, if any input seed can be copied now. */
    private boolean start() {
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack seed = items().getStackInSlot(slot);
            Optional<CropSeedItem.SeedData> data = CropSeedItem.data(seed).filter(CropSeedItem.SeedData::scanned);
            Optional<CropCard> card = data.flatMap(value -> CropCards.find(value.cardId()))
                    .filter(value -> !value.isWeed() && value.canDuplicate());
            if (card.isEmpty()) {
                continue;
            }
            int cost = SeedGeneratorRules.fertilizerCost(data.get().stats());
            ItemStack copy = seed.copyWithCount(1);
            if (fertilizer.getFluidAmount() < cost
                    || !items().insertItem(OUTPUT_SLOT, copy, true).isEmpty()
                    || !spendCatalyst(card.get(), true)) {
                continue;
            }
            spendCatalyst(card.get(), false);
            fertilizer.drain(cost, IFluidHandler.FluidAction.EXECUTE);
            MachineWorkCost.Cost recipe = MachineWorkCost.calculate(
                    RECIPE_EUT, RECIPE_TICKS, 1, true, 10_000, tier().inputMin(), tier().inputMax(), false, false);
            pending = copy;
            work = 0;
            totalWork = recipe.totalWork();
            minimumPower = recipe.minimumPower();
            setChanged();
            return true;
        }
        return false;
    }

    /** CropsNH takes the first of the card's catalysts the input slots hold enough of. */
    private boolean spendCatalyst(CropCard card, boolean simulate) {
        if (card.duplicationCatalysts().isEmpty()) {
            return true;
        }
        for (ItemStack catalyst : card.resolvedCatalysts()) {
            if (held(catalyst) < catalyst.getCount()) {
                continue;
            }
            if (!simulate) {
                int left = catalyst.getCount();
                for (int slot = 0; slot < INPUT_SLOTS && left > 0; slot++) {
                    if (ItemStack.isSameItem(items().getStackInSlot(slot), catalyst)) {
                        left -= items().extractItem(slot, left, false).getCount();
                    }
                }
            }
            return true;
        }
        return false;
    }

    private int held(ItemStack catalyst) {
        int count = 0;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = items().getStackInSlot(slot);
            if (ItemStack.isSameItem(stack, catalyst)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        super.dropContents(level, pos);
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), pending);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SeedGeneratorMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!pending.isEmpty()) {
            tag.put("Pending", pending.save(registries));
            tag.putLong("Work", work);
            tag.putLong("TotalWork", totalWork);
            tag.putLong("MinimumPower", minimumPower);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pending = tag.contains("Pending")
                ? ItemStack.parseOptional(registries, tag.getCompound("Pending"))
                : ItemStack.EMPTY;
        work = tag.getLong("Work");
        totalWork = tag.getLong("TotalWork");
        minimumPower = tag.getLong("MinimumPower");
    }
}
