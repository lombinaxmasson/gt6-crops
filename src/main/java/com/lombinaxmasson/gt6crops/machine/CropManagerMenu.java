package com.lombinaxmasson.gt6crops.machine;

import com.lombinaxmasson.gt6crops.Gt6Crops;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class CropManagerMenu extends MachineMenu {
    /** Room for two rows of output under the gauges. */
    public static final int EXTRA_HEIGHT = 42;

    public CropManagerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, clientMachine(inventory, buffer, CropManagerTile.class));
    }

    public CropManagerMenu(int id, Inventory inventory, CropManagerTile manager) {
        super(Gt6Crops.CROP_MANAGER_MENU.get(), id, inventory, manager);
        for (int slot = 0; slot < CropManagerTile.OUTPUT_START; slot++) {
            addMachineSlot(slot, 8 + slot % 2 * 18, 18 + slot / 2 * 18);
        }
        for (int i = 0; i < CropManagerTile.OUTPUT_SLOTS; i++) {
            addMachineSlot(CropManagerTile.OUTPUT_START + i, 8 + i % 9 * 18, 77 + i / 9 * 18);
        }
        addPlayerInventory(inventory, 8, 84 + EXTRA_HEIGHT);
    }
}
