package com.lombinaxmasson.gt6crops.client;

import com.lombinaxmasson.gt6crops.machine.SeedGeneratorMenu;
import com.lombinaxmasson.gt6crops.machine.SeedGeneratorTile;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SeedGeneratorScreen extends MachineScreen<SeedGeneratorMenu> {
    public SeedGeneratorScreen(SeedGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 0);
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        drawEnergy(graphics, 8, 17);
        drawTank(graphics, SeedGeneratorTile.FERTILIZER_TANK, 22, 17);
        drawProgress(graphics, 103, 35);
    }
}
