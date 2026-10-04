package com.lombinaxmasson.gt6crops.client;

import com.lombinaxmasson.gt6crops.Gt6Crops;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only renderer registration. */
@Mod(value = Gt6Crops.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Gt6Crops.MODID, value = Dist.CLIENT)
public final class Gt6CropsClient {
    public Gt6CropsClient(ModContainer container) {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Gt6Crops.CROP_TILE.get(), CropTileRenderer::new);
    }
}
