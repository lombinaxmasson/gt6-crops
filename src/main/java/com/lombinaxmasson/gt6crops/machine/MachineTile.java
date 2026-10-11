package com.lombinaxmasson.gt6crops.machine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.gregtech.gregtech.api.energy.EnergyBlockDefaults;
import com.gregtech.gregtech.api.energy.IEnergyBlock;
import com.gregtech.gregtech.data.GregTechTags;
import com.lombinaxmasson.gt6crops.Gt6CropsConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared GT6 electric machine. It takes EU from GT cables on every side but the
 * front, as GT6CE's basic machines do, holds items and single-fluid tanks, and
 * lights its front while it works.
 */
public abstract class MachineTile extends BlockEntity implements IEnergyBlock, MenuProvider {
    /** Synced to the open menu: last tick's energy, progress in thousandths, the switches, then each tank's amount. */
    static final int DATA_ENERGY = 0;
    static final int DATA_PROGRESS = 1;
    static final int DATA_TOGGLES = 2;
    static final int DATA_TANKS = 3;

    private final MachineTier tier;
    private final MachineEnergy energy;
    private final ItemStackHandler items;
    private final List<Tank> tanks = new ArrayList<>();
    private long lastEnergy;
    private int toggles;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_ENERGY) {
                return (int) Math.min(Integer.MAX_VALUE, lastEnergy);
            }
            if (index == DATA_PROGRESS) {
                return Math.round(Math.max(0.0F, Math.min(1.0F, progress())) * 1000);
            }
            if (index == DATA_TOGGLES) {
                return toggles;
            }
            return tanks.get(index - DATA_TANKS).getFluidAmount();
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_TANKS + tanks.size();
        }
    };

    protected MachineTile(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineTier tier, int slots) {
        super(type, pos, state);
        this.tier = tier;
        this.energy = new MachineEnergy(tier);
        this.items = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return canHold(slot, stack);
            }
        };
    }

    /**
     * A tank that only ever holds one fluid, so the menu can show it from its amount alone.
     * It may also take a concentrate of that fluid, stored as {@code strength} times its volume.
     */
    public final class Tank extends FluidTank {
        private final Supplier<? extends Fluid> fluid;
        @Nullable
        private final Supplier<? extends Fluid> concentrate;
        private final int strength;

        private Tank(int capacity, Supplier<? extends Fluid> fluid,
                @Nullable Supplier<? extends Fluid> concentrate, int strength) {
            super(capacity, stack -> stack.is(fluid.get()));
            this.fluid = fluid;
            this.concentrate = concentrate;
            this.strength = strength;
        }

        public Fluid fluid() {
            return fluid.get();
        }

        private boolean isConcentrate(FluidStack stack) {
            return concentrate != null && !stack.isEmpty() && stack.is(concentrate.get());
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return super.isFluidValid(stack) || isConcentrate(stack);
        }

        /** A concentrate is taken whole millibuckets at a time, so none of it is lost to rounding. */
        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isConcentrate(resource)) {
                return super.fill(resource, action);
            }
            int taken = Math.min(resource.getAmount(), (getCapacity() - getFluidAmount()) / strength);
            if (taken > 0) {
                super.fill(new FluidStack(fluid(), taken * strength), action);
            }
            return taken;
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    }

    protected final Tank addTank(int capacity, Supplier<? extends Fluid> fluid) {
        return addTank(capacity, fluid, null, 1);
    }

    protected final Tank addTank(int capacity, Supplier<? extends Fluid> fluid,
            @Nullable Supplier<? extends Fluid> concentrate, int strength) {
        Tank tank = new Tank(capacity, fluid, concentrate, strength);
        tanks.add(tank);
        return tank;
    }

    public MachineTier tier() {
        return tier;
    }

    public ItemStackHandler items() {
        return items;
    }

    public List<Tank> tanks() {
        return tanks;
    }

    ContainerData menuData() {
        return menuData;
    }

    /** Runs one tick with {@code available} EU and returns whether the machine did any work. */
    protected abstract boolean work(Level level, long available);

    /** Runs on each tick with less EU than the tier needs, instead of {@link #work}. */
    protected void unpowered() {}

    /** How far the current job is, from 0 to 1. Machines without jobs leave it at 0. */
    protected float progress() {
        return 0.0F;
    }

    /** Which items a slot takes, from players and pipes alike. */
    protected boolean canHold(int slot, ItemStack stack) {
        return true;
    }

    /** Slots that pipes and hoppers may fill. */
    protected boolean isInputSlot(int slot) {
        return true;
    }

    /** Slots that pipes and hoppers may empty, and that players can only take from. */
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    /** How many on/off switches the machine's screen shows. */
    protected int toggleCount() {
        return 0;
    }

    protected final boolean isOn(int toggle) {
        return (toggles & 1 << toggle) != 0;
    }

    protected final void setOn(int toggle, boolean on) {
        toggles = on ? toggles | 1 << toggle : toggles & ~(1 << toggle);
        setChanged();
    }

    boolean flip(int toggle) {
        if (toggle < 0 || toggle >= toggleCount()) {
            return false;
        }
        setOn(toggle, !isOn(toggle));
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineTile machine) {
        long available = machine.energy.drain();
        machine.lastEnergy = available;
        boolean powered = available >= machine.tier.inputMin();
        if (!powered) {
            machine.unpowered();
        }
        boolean worked = powered && machine.work(level, available);
        if (state.getValue(MachineBlock.ACTIVE) != worked) {
            level.setBlock(pos, state.setValue(MachineBlock.ACTIVE, worked), Block.UPDATE_CLIENTS);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(slot));
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // GT energy, following GT6CE's BasicMachineBlockEntity.

    private Direction front() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    public boolean isEnergyType(GregTechTags.Tag energyType, @Nullable Direction side, boolean emitting) {
        return !emitting && energyType == GregTechTags.Energy.EU;
    }

    @Override
    public Collection<GregTechTags.Tag> getEnergyTypes(@Nullable Direction side) {
        return List.of(GregTechTags.Energy.EU);
    }

    @Override
    public boolean isEnergyAcceptingFrom(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return isEnergyType(energyType, side, false) && side != front();
    }

    @Override
    public boolean isEnergyEmittingTo(GregTechTags.Tag energyType, @Nullable Direction side, boolean theoretical) {
        return false;
    }

    @Override
    public synchronized long doEnergyInjection(
            GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        return EnergyBlockDefaults.doEnergyInjection(this, energyType, side, size, amount, doInject);
    }

    @Override
    public long doInject(
            GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doInject) {
        if (energy.isOvervoltage(size)) {
            if (doInject) {
                overvoltage(size);
            }
            return amount;
        }
        long accepted = energy.accept(size, amount, doInject);
        if (doInject && accepted > 0) {
            setChanged();
        }
        return accepted;
    }

    /** GT6CE blows the machine up, unless its explosions are turned off; then the packet is lost. */
    private void overvoltage(long size) {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!Gt6CropsConfig.overvoltageExplosions()) {
            energy.clear();
            return;
        }
        BlockPos pos = worldPosition;
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                energy.explosionPower(size), Level.ExplosionInteraction.BLOCK);
    }

    @Override
    public long getEnergyDemanded(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        if (!isEnergyAcceptingFrom(energyType, side, false)) {
            return 0;
        }
        return energy.demanded(size);
    }

    @Override
    public synchronized long doEnergyExtraction(
            GregTechTags.Tag energyType, @Nullable Direction side, long size, long amount, boolean doExtract) {
        return 0;
    }

    @Override
    public long getEnergyOffered(GregTechTags.Tag energyType, @Nullable Direction side, long size) {
        return 0;
    }

    @Override
    public long getEnergySizeInputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return tier.inputMin();
    }

    @Override
    public long getEnergySizeInputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return tier.voltage();
    }

    @Override
    public long getEnergySizeInputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return tier.inputMax();
    }

    @Override
    public long getEnergySizeOutputMin(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 0;
    }

    @Override
    public long getEnergySizeOutputRecommended(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 0;
    }

    @Override
    public long getEnergySizeOutputMax(GregTechTags.Tag energyType, @Nullable Direction side) {
        return 0;
    }

    // Pipes, hoppers and cables.

    public static <T extends MachineTile> void registerCapabilities(
            RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, MachineTile::automation);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, MachineTile::fluidIntake);
    }

    private static IItemHandler automation(MachineTile machine, @Nullable Direction side) {
        return machine.new Automation();
    }

    @Nullable
    private static IFluidHandler fluidIntake(MachineTile machine, @Nullable Direction side) {
        return machine.tanks.isEmpty() ? null : machine.new FluidIntake();
    }

    /** Pipes fill input slots and empty output slots. */
    private final class Automation implements IItemHandler {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return isInputSlot(slot) ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isOutputSlot(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isInputSlot(slot) && items.isItemValid(slot, stack);
        }
    }

    /** Machines only ever take fluids in, each into the one tank that holds it. */
    private final class FluidIntake implements IFluidHandler {
        @Override
        public int getTanks() {
            return tanks.size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks.get(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tanks.get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tanks.get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            for (Tank tank : tanks) {
                if (tank.isFluidValid(resource)) {
                    return tank.fill(resource, action);
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Energy", energy.stored());
        tag.put("Items", items.serializeNBT(registries));
        ListTag tankTags = new ListTag();
        for (Tank tank : tanks) {
            tankTags.add(tank.writeToNBT(registries, new CompoundTag()));
        }
        tag.put("Tanks", tankTags);
        tag.putInt("Toggles", toggles);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.load(tag.getLong("Energy"));
        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
        ListTag tankTags = tag.getList("Tanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(tankTags.size(), tanks.size()); i++) {
            tanks.get(i).readFromNBT(registries, tankTags.getCompound(i));
        }
        if (tag.contains("Toggles")) {
            toggles = tag.getInt("Toggles");
        }
    }
}
