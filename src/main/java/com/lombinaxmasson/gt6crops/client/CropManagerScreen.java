package com.lombinaxmasson.gt6crops.client;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.machine.CropManagerMenu;
import com.lombinaxmasson.gt6crops.machine.CropManagerTile;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CropManagerScreen extends MachineScreen<CropManagerMenu> {
    private final ItemStack harvestIcon = new ItemStack(Items.WHEAT);
    private final ItemStack waterIcon = new ItemStack(Items.WATER_BUCKET);
    private final ItemStack fertilizerIcon = new ItemStack(Items.BONE_MEAL);
    private final ItemStack weedExIcon = new ItemStack(Gt6Crops.WEED_EX.get());

    public CropManagerScreen(CropManagerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CropManagerMenu.EXTRA_HEIGHT);
    }

    @Override
    protected void drawGauges(GuiGraphics graphics) {
        drawEnergy(graphics, 48, 17);
        drawTank(graphics, CropManagerTile.WATER_TANK, 62, 17);
        drawTank(graphics, CropManagerTile.FERTILIZER_TANK, 82, 17);
        drawTank(graphics, CropManagerTile.WEED_EX_TANK, 102, 17);
        drawToggle(graphics, CropManagerTile.HARVEST, 126, 17, harvestIcon,
                Component.translatable("gui.gt6crops.crop_manager.harvest"));
        drawToggle(graphics, CropManagerTile.WATER, 146, 17, waterIcon,
                Component.translatable("gui.gt6crops.crop_manager.water"));
        drawToggle(graphics, CropManagerTile.FERTILIZER, 126, 37, fertilizerIcon,
                Component.translatable("gui.gt6crops.crop_manager.fertilizer"));
        drawToggle(graphics, CropManagerTile.WEED_EX, 146, 37, weedExIcon,
                Component.translatable("gui.gt6crops.crop_manager.weed_ex"));
        drawProgress(graphics, 133, 57);
    }
}
