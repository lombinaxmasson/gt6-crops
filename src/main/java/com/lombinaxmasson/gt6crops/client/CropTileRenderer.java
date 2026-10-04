package com.lombinaxmasson.gt6crops.client;

import org.joml.Matrix4f;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** Renders the species-specific crop cross from the tile's card id. */
public final class CropTileRenderer implements BlockEntityRenderer<CropTile> {
    private static final ResourceLocation WEED_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/dead_bush");

    public CropTileRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            CropTile tile,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        CropCard card = tile.card().orElse(null);
        if (card == null) {
            return;
        }
        ResourceLocation texture = texture(card, tile.size());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        Matrix4f matrix;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        float height = card.isWeed() ? 0.45f : 0.25f + tile.size() * 0.16f;
        matrix = poseStack.last().pose();
        drawCross(
                matrix,
                consumer,
                sprite,
                -0.38f,
                0.38f,
                0.06f,
                height,
                packedLight,
                packedOverlay);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(CropTile tile) {
        return true;
    }

    private static void drawCross(
            Matrix4f matrix,
            VertexConsumer consumer,
            TextureAtlasSprite sprite,
            float x0,
            float x1,
            float z0,
            float height,
            int light,
            int overlay) {
        quad(matrix, consumer, sprite, x0, 0, z0, x1, height, z0, light, overlay);
        quad(matrix, consumer, sprite, z0, 0, x0, z0, height, x1, light, overlay);
    }

    private static void quad(
            Matrix4f matrix,
            VertexConsumer consumer,
            TextureAtlasSprite sprite,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            int light,
            int overlay) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        consumer.addVertex(matrix, x1, y0, z1).setColor(255, 255, 255, 255)
                .setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x0, y0, z0).setColor(255, 255, 255, 255)
                .setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x0, y1, z0).setColor(255, 255, 255, 255)
                .setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
        consumer.addVertex(matrix, x1, y1, z1).setColor(255, 255, 255, 255)
                .setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
    }

    private static ResourceLocation texture(CropCard card, int size) {
        if (card.isWeed()) {
            return WEED_TEXTURE;
        }
        if (isVanilla(card.id())) {
            return vanillaTexture(card.id(), size);
        }
        int stage = Math.max(1, Math.min(card.maxSize(), size));
        return ResourceLocation.fromNamespaceAndPath(
                Gt6Crops.MODID,
                "block/crop/" + card.texture() + "/" + stage);
    }

    private static ResourceLocation vanillaTexture(String id, int size) {
        return switch (id) {
            case "wheat" -> ResourceLocation.withDefaultNamespace(
                    "block/wheat_stage" + Math.min(7, size + 1));
            case "carrot", "potato", "beetroot" -> ResourceLocation.withDefaultNamespace(
                    "block/" + id + "_stage" + Math.min(3, size - 1));
            case "pumpkin", "melon" -> ResourceLocation.withDefaultNamespace(
                    "block/" + id + "_stem_stage" + Math.min(7, size + 1));
            case "sugar_cane", "nether_wart" -> ResourceLocation.withDefaultNamespace(
                    "block/" + id);
            case "cocoa" -> ResourceLocation.withDefaultNamespace("block/cocoa_stage2");
            default -> ResourceLocation.withDefaultNamespace("block/" + id);
        };
    }

    private static boolean isVanilla(String id) {
        return switch (id) {
            case "wheat", "carrot", "potato", "beetroot", "pumpkin", "melon",
                    "sugar_cane", "nether_wart", "cocoa", "dandelion", "poppy",
                    "blue_orchid", "allium", "azure_bluet", "red_tulip",
                    "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy",
                    "cornflower", "lily_of_the_valley" -> true;
            default -> false;
        };
    }
}
