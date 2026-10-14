package com.lombinaxmasson.gt6crops.machine;

import com.lombinaxmasson.gt6crops.Gt6Crops;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class SeedGeneratorMenu extends MachineMenu {
    public SeedGeneratorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, clientMachine(inventory, buffer, SeedGeneratorTile.class));
    }

    public SeedGeneratorMenu(int id, Inventory inventory, SeedGeneratorTile generator) {
        super(Gt6Crops.SEED_GENERATOR_MENU.get(), id, inventory, generator);
        for (int slot = 0; slot < SeedGeneratorTile.INPUT_SLOTS; slot++) {
            addMachineSlot(slot, 62 + slot * 18, 35);
        }
        addMachineSlot(SeedGeneratorTile.OUTPUT_SLOT, 134, 35);
        addPlayerInventory(inventory, 8, 84);
    }
}
