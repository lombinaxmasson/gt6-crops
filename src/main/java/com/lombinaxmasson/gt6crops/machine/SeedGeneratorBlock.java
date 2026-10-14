package com.lombinaxmasson.gt6crops.machine;

import java.util.List;

import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SeedGeneratorBlock extends MachineBlock {
    public SeedGeneratorBlock(MachineTier tier) {
        super(tier);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SeedGeneratorTile(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("block.gt6crops.seed_generator.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("block.gt6crops.seed_generator.tooltip.cost",
                SeedGeneratorRules.FERTILIZER_PER_STAT, SeedGeneratorRules.capacity(tier()))
                .withStyle(ChatFormatting.GRAY));
        int maxStats = SeedGeneratorRules.maxTotalStats(tier());
        if (maxStats < 3 * CropRules.MAX_STAT) {
            tooltip.add(Component.translatable("block.gt6crops.seed_generator.tooltip.small", maxStats)
                    .withStyle(ChatFormatting.RED));
        }
    }
}
