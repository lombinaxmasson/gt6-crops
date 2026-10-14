package com.lombinaxmasson.gt6crops.machine;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CropManagerBlock extends MachineBlock {
    public CropManagerBlock(MachineTier tier) {
        super(tier);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CropManagerTile(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.gt6crops.crop_manager.tooltip.range",
                CropManagerTile.horizontalRadius(tier()), CropManagerTile.VERTICAL_RADIUS)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("block.gt6crops.crop_manager.tooltip.bonus",
                CropManagerTile.harvestBonusPercent(tier())).withStyle(ChatFormatting.GRAY));
    }
}
