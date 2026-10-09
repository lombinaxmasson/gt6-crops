package com.lombinaxmasson.gt6crops.client;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.item.CropSeedItem;
import com.lombinaxmasson.gt6crops.item.SeedColors;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

/** Client crop models, generated crop stages, crop tints, and wheat-seed tints. */
@Mod(value = Gt6Crops.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Gt6Crops.MODID, value = Dist.CLIENT)
public final class Gt6CropsClient {
    public Gt6CropsClient(ModContainer container) {}

    @SubscribeEvent
    public static void registerSeedColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            String cardId = CropSeedItem.data(stack)
                    .map(CropSeedItem.SeedData::cardId)
                    .orElse(null);
            return SeedColors.argb(cardId, tintIndex);
        }, Gt6Crops.CROP_SEED.get());
    }

    @SubscribeEvent
    public static void registerCropStages(RegisterSpriteSourceTypesEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "crop_stages"), CropStageSprites.TYPE);
    }

    @SubscribeEvent
    public static void registerCropTints(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != CropPlantModel.GRASS_TINT) {
                return -1;
            }
            return level != null && pos != null
                    ? BiomeColors.getAverageGrassColor(level, pos)
                    : GrassColor.getDefaultColor();
        }, Gt6Crops.CROP_STICK.get());
    }

    @SubscribeEvent
    public static void wrapCropModels(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) -> {
            if (isCropStick(location)) {
                return new CropPlantModel(model);
            }
            return model;
        });
    }

    private static boolean isCropStick(ModelResourceLocation location) {
        return Gt6Crops.MODID.equals(location.id().getNamespace())
                && (location.id().getPath().equals("crop_stick")
                        || location.id().getPath().equals("block/crop_stick")
                        || location.id().getPath().equals("block/cross_crop_stick"));
    }
}
