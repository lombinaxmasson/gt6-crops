package com.lombinaxmasson.gt6crops.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.lombinaxmasson.gt6crops.Gt6Crops;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;

/**
 * Block-atlas source for the stages of vanilla cards that have no shippable stage art:
 * flowers, sugar cane, and the ripe pumpkin and melon. Each is drawn from the textures the
 * player has loaded, and a sprite a resource pack provides for a stage takes precedence.
 */
final class CropStageSprites implements SpriteSource {
    static final CropStageSprites INSTANCE = new CropStageSprites();
    static final SpriteSourceType TYPE = new SpriteSourceType(MapCodec.unit(INSTANCE));

    private static final List<String> FLOWERS = List.of(
            "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip",
            "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower",
            "lily_of_the_valley");
    private static final List<String> FRUITS = List.of("pumpkin", "melon");
    /** Every card drawn here grows to size 4. */
    private static final int STAGES = 4;

    private record Square(int[] pixels, int size) {}

    private CropStageSprites() {}

    @Override
    public void run(ResourceManager resources, Output output) {
        for (String flower : FLOWERS) {
            ResourceLocation art = ResourceLocation.withDefaultNamespace("block/" + flower);
            for (int stage = 1; stage < STAGES; stage++) {
                int drawn = stage;
                draw(resources, output, stage(flower, stage), List.of(art),
                        images -> CropStageArt.flower(images.get(0).pixels(), images.get(0).size(), drawn));
            }
            copy(resources, output, stage(flower, STAGES), art);
        }
        ResourceLocation cane = ResourceLocation.withDefaultNamespace("block/sugar_cane");
        for (int stage = 1; stage < STAGES; stage++) {
            int drawn = stage;
            draw(resources, output, stage("sugar_cane", stage), List.of(cane), images -> {
                Square image = images.get(0);
                return CropStageArt.bottomRows(image.pixels(), image.size(), image.size() * drawn / STAGES);
            });
        }
        copy(resources, output, stage("sugar_cane", STAGES), cane);
        for (String fruit : FRUITS) {
            List<ResourceLocation> parts = List.of(
                    ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "crop_parts/" + fruit + "_vine"),
                    ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "crop_parts/" + fruit + "_fruit"),
                    ResourceLocation.withDefaultNamespace("block/" + fruit + "_side"));
            draw(resources, output, stage(fruit, STAGES), parts, images -> {
                Square vine = images.get(0);
                Square map = images.get(1);
                if (map.size() != vine.size()) {
                    throw new IllegalArgumentException("fruit map and vine differ in size");
                }
                return CropStageArt.fillFruit(
                        vine.pixels(), map.pixels(), vine.size(), images.get(2).pixels(), images.get(2).size());
            });
        }
    }

    @Override
    public SpriteSourceType type() {
        return TYPE;
    }

    private static ResourceLocation stage(String texture, int stage) {
        return ResourceLocation.fromNamespaceAndPath(Gt6Crops.MODID, "block/crop/" + texture + "/" + stage);
    }

    /** The first image sets the sprite's size. Resources resolve now and open on the loader thread. */
    private static void draw(
            ResourceManager resources,
            Output output,
            ResourceLocation id,
            List<ResourceLocation> sources,
            Function<List<Square>, int[]> art) {
        if (find(resources, id).isPresent()) {
            return;
        }
        List<Resource> files = new ArrayList<>(sources.size());
        for (ResourceLocation source : sources) {
            Optional<Resource> file = find(resources, source);
            if (file.isEmpty()) {
                Gt6Crops.LOGGER.warn("gt6crops: cannot draw crop stage {}; {} is missing", id, source);
                return;
            }
            files.add(file.get());
        }
        output.add(id, loader -> {
            try {
                List<Square> images = new ArrayList<>(files.size());
                for (Resource file : files) {
                    images.add(read(file));
                }
                return contents(id, art.apply(images), images.get(0).size());
            } catch (IOException | RuntimeException exception) {
                Gt6Crops.LOGGER.warn("gt6crops: cannot draw crop stage {}", id, exception);
                return null;
            }
        });
    }

    private static void copy(ResourceManager resources, Output output, ResourceLocation id, ResourceLocation source) {
        if (find(resources, id).isEmpty()) {
            find(resources, source).ifPresent(file -> output.add(id, file));
        }
    }

    private static Optional<Resource> find(ResourceManager resources, ResourceLocation sprite) {
        return resources.getResource(TEXTURE_ID_CONVERTER.idToFile(sprite));
    }

    /** Reads the first animation frame: the top square of the image. */
    private static Square read(Resource file) throws IOException {
        try (InputStream stream = file.open(); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, stream)) {
            int size = Math.min(image.getWidth(), image.getHeight());
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    pixels[y * size + x] = image.getPixelRGBA(x, y);
                }
            }
            return new Square(pixels, size);
        }
    }

    private static SpriteContents contents(ResourceLocation id, int[] pixels, int size) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, size, size, false);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setPixelRGBA(x, y, pixels[y * size + x]);
            }
        }
        return new SpriteContents(id, new FrameSize(size, size), image, ResourceMetadata.EMPTY);
    }
}
