package com.lombinaxmasson.gt6crops.machine;

import net.minecraft.core.BlockPos;
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
}
