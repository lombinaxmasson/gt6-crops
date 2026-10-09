package com.lombinaxmasson.gt6crops.block;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCards;

import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A crop stick has no collision, so a click aimed at the sticks can land on
 * the soil underneath. Plant from that click as well.
 */
@EventBusSubscriber(modid = Gt6Crops.MODID)
public final class CropStickEvents {
    private CropStickEvents() {}

    @SubscribeEvent
    public static void plantThroughSoil(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!CropCards.isSoil(level.getBlockState(pos))) {
            return;
        }
        BlockPos above = pos.above();
        if (!(level.getBlockEntity(above) instanceof CropTile crop)) {
            return;
        }
        ItemInteractionResult result = CropStickBlock.offerSeed(
                level, crop, event.getItemStack(), event.getEntity());
        if (!result.consumesAction()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(result.result());
    }
}
