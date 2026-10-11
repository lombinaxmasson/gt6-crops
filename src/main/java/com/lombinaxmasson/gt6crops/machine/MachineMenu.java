package com.lombinaxmasson.gt6crops.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Shared machine menu. Machine slots come first and the player's inventory after
 * them, so shift-clicking can tell the two apart.
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    private static final int PLAYER_SLOTS = 36;

    private final MachineTile machine;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private int machineSlots;

    protected MachineMenu(MenuType<?> type, int id, Inventory inventory, MachineTile machine) {
        super(type, id);
        this.machine = machine;
        this.data = inventory.player.level().isClientSide
                ? new SimpleContainerData(machine.menuData().getCount())
                : machine.menuData();
        this.access = ContainerLevelAccess.create(machine.getLevel(), machine.getBlockPos());
        addDataSlots(data);
    }

    /** The client opens the menu on its own copy of the block entity at the position the server sent. */
    protected static <T extends MachineTile> T clientMachine(
            Inventory inventory, RegistryFriendlyByteBuf buffer, Class<T> type) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity entity = inventory.player.level().getBlockEntity(pos);
        if (type.isInstance(entity)) {
            return type.cast(entity);
        }
        throw new IllegalStateException("No " + type.getSimpleName() + " at " + pos);
    }

    public MachineTile machine() {
        return machine;
    }

    public int machineSlotCount() {
        return machineSlots;
    }

    /** EU the machine received last tick. */
    public int energy() {
        return data.get(MachineTile.DATA_ENERGY);
    }

    public float progress() {
        return data.get(MachineTile.DATA_PROGRESS) / 1000.0F;
    }

    public int tankAmount(int tank) {
        return data.get(MachineTile.DATA_TANKS + tank);
    }

    public boolean isOn(int toggle) {
        return (data.get(MachineTile.DATA_TOGGLES) & 1 << toggle) != 0;
    }

    /** The screen's switches reach the server as menu button clicks. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        return machine.flip(id);
    }

    /** Must be called for every machine slot before {@link #addPlayerInventory}. */
    protected void addMachineSlot(int slot, int x, int y) {
        boolean output = machine.isOutputSlot(slot);
        addSlot(new SlotItemHandler(machine.items(), slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !output && super.mayPlace(stack);
            }
        });
        machineSlots++;
    }

    /** Vanilla places the inventory at y 84; a machine with a taller screen adds its extra height. */
    protected void addPlayerInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < machineSlots
                ? moveItemStackTo(stack, machineSlots, machineSlots + PLAYER_SLOTS, true)
                : moveItemStackTo(stack, 0, machineSlots, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, machine.getBlockState().getBlock());
    }
}
