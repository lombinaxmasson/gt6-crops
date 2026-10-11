package com.lombinaxmasson.gt6crops.block;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.card.CropCards;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A crop stick has no collision, so clicks and falls aimed at it can land on
 * the soil underneath. Plant and trample from those as well.
 */
@EventBusSubscriber(modid = Gt6Crops.MODID)
public final class CropStickEvents {
    /** Vanilla shrinks the entity's box by this much when it looks for blocks the entity is inside. */
    private static final double INSIDE_EPSILON = 1.0E-7;

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

    /** Every crop stick the landing entity stands in rolls for trampling, as in CropsNH. */
    @SubscribeEvent
    public static void trampleOnLanding(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide || event.getDistance() <= CropRules.MIN_TRAMPLE_FALL) {
            return;
        }
        AABB box = entity.getBoundingBox();
        BlockPos min = BlockPos.containing(
                box.minX + INSIDE_EPSILON, box.minY + INSIDE_EPSILON, box.minZ + INSIDE_EPSILON);
        BlockPos max = BlockPos.containing(
                box.maxX - INSIDE_EPSILON, box.maxY - INSIDE_EPSILON, box.maxZ - INSIDE_EPSILON);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(pos) instanceof CropTile crop) {
                crop.landedOn(entity, event.getDistance());
            }
        }
    }
}
