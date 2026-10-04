package com.lombinaxmasson.gt6crops.client;

import org.joml.Matrix4f;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/** Renders the planted crop as four full-height sheets in a hash. */
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
        Matrix4f matrix = poseStack.last().pose();
        float y0 = -1.0f / 16.0f;
        float y1 = 1.0f;
        sheetX(matrix, consumer, sprite, 4.0f / 16.0f, y0, y1, packedLight, packedOverlay);
        sheetX(matrix, consumer, sprite, 12.0f / 16.0f, y0, y1, packedLight, packedOverlay);
        sheetZ(matrix, consumer, sprite, 4.0f / 16.0f, y0, y1, packedLight, packedOverlay);
        sheetZ(matrix, consumer, sprite, 12.0f / 16.0f, y0, y1, packedLight, packedOverlay);
    }

    @Override
    public boolean shouldRenderOffScreen(CropTile tile) {
        return true;
    }

    /** A plant sheet at constant x, spanning the block on z. Both sides are drawn. */
    private static void sheetX(
            Matrix4f matrix,
            VertexConsumer consumer,
            TextureAtlasSprite sprite,
            float x,
            float y0,
            float y1,
            int light,
            int overlay) {
        quad(matrix, consumer, sprite, x, y0, 0, x, y0, 1, x, y1, 1, x, y1, 0, light, overlay);
        quad(matrix, consumer, sprite, x, y0, 0, x, y1, 0, x, y1, 1, x, y0, 1, light, overlay);
    }

    /** A plant sheet at constant z, spanning the block on x. Both sides are drawn. */
    private static void sheetZ(
            Matrix4f matrix,
            VertexConsumer consumer,
            TextureAtlasSprite sprite,
            float z,
            float y0,
            float y1,
            int light,
            int overlay) {
        quad(matrix, consumer, sprite, 0, y0, z, 1, y0, z, 1, y1, z, 0, y1, z, light, overlay);
        quad(matrix, consumer, sprite, 0, y0, z, 0, y1, z, 1, y1, z, 1, y0, z, light, overlay);
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
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            int light,
            int overlay) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        vertex(matrix, consumer, x0, y0, z0, u0, v1, light, overlay);
        vertex(matrix, consumer, x1, y1, z1, u1, v1, light, overlay);
        vertex(matrix, consumer, x2, y2, z2, u1, v0, light, overlay);
        vertex(matrix, consumer, x3, y3, z3, u0, v0, light, overlay);
    }

    private static void vertex(
            Matrix4f matrix,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light,
            int overlay) {
        consumer.addVertex(matrix, x, y, z).setColor(255, 255, 255, 255)
                .setUv(u, v).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
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
