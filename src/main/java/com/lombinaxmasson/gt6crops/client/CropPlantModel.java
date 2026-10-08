package com.lombinaxmasson.gt6crops.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * Stick model plus four cached hash sheets. The sheets are baked once per
 * sprite, then reused by chunk meshing instead of a block-entity renderer.
 */
public final class CropPlantModel implements IDynamicBakedModel {
    private final BakedModel sticks;
    private final Map<ResourceLocation, List<BakedQuad>> plantQuads = new ConcurrentHashMap<>();

    public CropPlantModel(BakedModel sticks) {
        this.sticks = sticks;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData data,
            @Nullable net.minecraft.client.renderer.RenderType renderType) {
        List<BakedQuad> base = sticks.getQuads(state, side, rand);
        if (side != null) {
            return base;
        }
        String cropId = data.get(CropTile.CROP);
        Integer size = data.get(CropTile.SIZE);
        if (cropId == null || cropId.isEmpty() || size == null || size <= 0) {
            return base;
        }
        CropCard card = CropCards.find(cropId).orElse(null);
        if (card == null) {
            return base;
        }
        List<BakedQuad> plants = plantQuads.computeIfAbsent(
                CropTextures.of(card, size), this::bake);
        if (plants.isEmpty()) {
            return base;
        }
        List<BakedQuad> combined = new ArrayList<>(base.size() + plants.size());
        combined.addAll(base);
        combined.addAll(plants);
        return combined;
    }

    private List<BakedQuad> bake(ResourceLocation texture) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        if (sprite.contents().name().equals(ResourceLocation.withDefaultNamespace("missingno"))) {
            return List.of();
        }
        float y0 = -1.0f / 16.0f;
        float y1 = 1.0f;
        List<BakedQuad> quads = new ArrayList<>(8);
        sheetX(quads, sprite, 4.0f / 16.0f, y0, y1);
        sheetX(quads, sprite, 12.0f / 16.0f, y0, y1);
        sheetZ(quads, sprite, 4.0f / 16.0f, y0, y1);
        sheetZ(quads, sprite, 12.0f / 16.0f, y0, y1);
        return List.copyOf(quads);
    }

    private static void sheetX(
            List<BakedQuad> quads, TextureAtlasSprite sprite, float x, float y0, float y1) {
        quads.add(quad(sprite, x, y0, 0, x, y0, 1, x, y1, 1, x, y1, 0));
        quads.add(quad(sprite, x, y0, 0, x, y1, 0, x, y1, 1, x, y0, 1));
    }

    private static void sheetZ(
            List<BakedQuad> quads, TextureAtlasSprite sprite, float z, float y0, float y1) {
        quads.add(quad(sprite, 0, y0, z, 1, y0, z, 1, y1, z, 0, y1, z));
        quads.add(quad(sprite, 0, y0, z, 0, y1, z, 1, y1, z, 1, y0, z));
    }

    private static BakedQuad quad(
            TextureAtlasSprite sprite,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3) {
        QuadBakingVertexConsumer consumer = new QuadBakingVertexConsumer();
        consumer.setSprite(sprite);
        consumer.setDirection(Direction.UP);
        consumer.setTintIndex(-1);
        consumer.setShade(false);
        vertex(consumer, sprite, x0, y0, z0, sprite.getU0(), sprite.getV1());
        vertex(consumer, sprite, x1, y1, z1, sprite.getU1(), sprite.getV1());
        vertex(consumer, sprite, x2, y2, z2, sprite.getU1(), sprite.getV0());
        vertex(consumer, sprite, x3, y3, z3, sprite.getU0(), sprite.getV0());
        return consumer.bakeQuad();
    }

    private static void vertex(
            QuadBakingVertexConsumer consumer,
            TextureAtlasSprite sprite,
            float x,
            float y,
            float z,
            float u,
            float v) {
        consumer.addVertex(x, y, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u, v).setNormal(0, 1, 0);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return sticks.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return sticks.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return sticks.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return sticks.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return sticks.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return sticks.getOverrides();
    }
}
