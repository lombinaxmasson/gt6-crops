package com.lombinaxmasson.gt6crops.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.lombinaxmasson.gt6crops.block.CropTile;
import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropCards;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

/**
 * Stick model plus cached plant sheets in the card's render shape. The sheets are baked
 * once per sprite, tint, shape, and soil depth, then reused by chunk meshing instead of
 * a block-entity renderer.
 */
public final class CropPlantModel implements IDynamicBakedModel {
    private static final float[] SHEET_OFFSETS = {4.0f / 16.0f, 12.0f / 16.0f};
    /** Vanilla cross models run corner to corner, 0.8 pixels in from each side. */
    private static final float CROSS_MIN = 0.8f / 16.0f;
    private static final float CROSS_MAX = 15.2f / 16.0f;
    /**
     * Pixels from the crop stick's floor down to the soil surface. Sheets are one texture
     * tall and start on that surface, as vanilla crops do on farmland.
     */
    private static final ModelProperty<Integer> SOIL_DEPTH = new ModelProperty<>();
    private static final int FARMLAND_DEPTH = 1;
    /** Tint index that {@link Gt6CropsClient} colors with the biome's grass color. */
    static final int GRASS_TINT = 0;

    private record Plant(CropTextures.PlantSprite sprite, CropCard.RenderShape shape, int soilDepth) {}

    private final BakedModel sticks;
    private final Map<Plant, List<BakedQuad>> plantQuads = new ConcurrentHashMap<>();

    public CropPlantModel(BakedModel sticks) {
        this.sticks = sticks;
    }

    /** Measures the soil from its outline on the client, so the crop tile need not sync it. */
    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        BlockPos soil = pos.below();
        double top = level.getBlockState(soil).getShape(level, soil).max(Direction.Axis.Y);
        int depth = top > 0 && top < 1 ? (int) Math.round((1 - top) * 16) : 0;
        return modelData.derive().with(SOIL_DEPTH, depth).build();
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData data,
            @Nullable RenderType renderType) {
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
        Integer depth = data.get(SOIL_DEPTH);
        Plant plant = new Plant(CropTextures.of(card, size), card.renderShape(),
                depth == null ? FARMLAND_DEPTH : depth);
        List<BakedQuad> plants = plantQuads.computeIfAbsent(plant, this::bake);
        if (plants.isEmpty()) {
            return base;
        }
        List<BakedQuad> combined = new ArrayList<>(base.size() + plants.size());
        combined.addAll(base);
        combined.addAll(plants);
        return combined;
    }

    private List<BakedQuad> bake(Plant plant) {
        CropTextures.PlantSprite tint = plant.sprite();
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(tint.texture());
        if (sprite.contents().name().equals(ResourceLocation.withDefaultNamespace("missingno"))) {
            Gt6Crops.LOGGER.warn("gt6crops: missing crop texture {}", tint.texture());
            return List.of();
        }
        float bottom = -plant.soilDepth() / 16.0f;
        List<BakedQuad> quads = new ArrayList<>(8);
        switch (plant.shape()) {
            case HASH -> {
                for (float offset : SHEET_OFFSETS) {
                    sheet(quads, sprite, tint, bottom, offset, 0, offset, 1);
                    sheet(quads, sprite, tint, bottom, 0, offset, 1, offset);
                }
            }
            case X -> {
                sheet(quads, sprite, tint, bottom, CROSS_MIN, CROSS_MIN, CROSS_MAX, CROSS_MAX);
                sheet(quads, sprite, tint, bottom, CROSS_MIN, CROSS_MAX, CROSS_MAX, CROSS_MIN);
            }
        }
        return List.copyOf(quads);
    }

    /**
     * One upright sheet from (x0, z0) to (x1, z1). Its front faces to the right of that walk
     * and shows the texture unmirrored. The back quad reuses the same corners in
     * reverse order, so each corner keeps its texture position and the image lines up
     * through the sheet, as in vanilla crop models.
     */
    private static void sheet(
            List<BakedQuad> quads,
            TextureAtlasSprite sprite,
            CropTextures.PlantSprite tint,
            float bottom,
            float x0,
            float z0,
            float x1,
            float z1) {
        float top = bottom + 1.0f;
        float[] bottomLeft = {x0, bottom, z0, sprite.getU0(), sprite.getV1()};
        float[] bottomRight = {x1, bottom, z1, sprite.getU1(), sprite.getV1()};
        float[] topRight = {x1, top, z1, sprite.getU1(), sprite.getV0()};
        float[] topLeft = {x0, top, z0, sprite.getU0(), sprite.getV0()};
        float length = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0));
        float normalX = (z0 - z1) / length;
        float normalZ = (x1 - x0) / length;
        quads.add(quad(sprite, tint, normalX, normalZ, bottomLeft, bottomRight, topRight, topLeft));
        quads.add(quad(sprite, tint, -normalX, -normalZ, bottomLeft, topLeft, topRight, bottomRight));
    }

    /** Corners are {x, y, z, u, v}, counter-clockwise as seen from the horizontal normal. */
    private static BakedQuad quad(
            TextureAtlasSprite sprite,
            CropTextures.PlantSprite tint,
            float normalX,
            float normalZ,
            float[]... corners) {
        QuadBakingVertexConsumer consumer = new QuadBakingVertexConsumer();
        consumer.setSprite(sprite);
        consumer.setDirection(Direction.getNearest(normalX, 0, normalZ));
        consumer.setTintIndex(tint.grassTint() ? GRASS_TINT : -1);
        consumer.setShade(false);
        for (float[] corner : corners) {
            consumer.addVertex(corner[0], corner[1], corner[2])
                    .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                    .setUv(corner[3], corner[4])
                    .setNormal(normalX, 0, normalZ);
        }
        return consumer.bakeQuad();
    }

    /** Without this NeoForge falls back to the solid layer and transparent pixels render black. */
    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return sticks.getRenderTypes(state, rand, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return sticks.getRenderTypes(itemStack, fabulous);
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
