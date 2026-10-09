package com.lombinaxmasson.gt6crops.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Growth stages drawn from the player's own vanilla textures, so no Mojang art ships in
 * this jar. Images are square, row-major, and packed as {@code NativeImage} stores them:
 * alpha, blue, green, red from the high byte down.
 */
final class CropStageArt {
    private CropStageArt() {}

    /**
     * Stage 1 is the lower half of the stem, stage 2 the whole stem, and stage 3 the stem
     * with the bloom recoloured into a green bud. A flower with no green keeps its art.
     */
    static int[] flower(int[] pixels, int size, int stage) {
        List<Integer> stem = new ArrayList<>();
        List<Integer> bloom = new ArrayList<>();
        int stemTop = size;
        int bottom = 0;
        for (int index = 0; index < pixels.length; index++) {
            if (alpha(pixels[index]) == 0) {
                continue;
            }
            bottom = index / size;
            if (isStem(pixels[index])) {
                stem.add(index);
                stemTop = Math.min(stemTop, index / size);
            } else {
                bloom.add(index);
            }
        }
        if (stem.isEmpty()) {
            return pixels.clone();
        }
        int[] result = new int[pixels.length];
        for (int index : stem) {
            if (stage > 1 || 2 * (index / size) >= stemTop + bottom) {
                result[index] = pixels[index];
            }
        }
        if (stage >= 3 && !bloom.isEmpty()) {
            Comparator<Integer> byLuminance = Comparator.comparingInt(index -> luminance(pixels[index]));
            List<Integer> greens = new ArrayList<>(stem);
            greens.sort(byLuminance);
            bloom.sort(byLuminance);
            for (int rank = 0; rank < bloom.size(); rank++) {
                int green = pixels[greens.get(rank * greens.size() / bloom.size())];
                int index = bloom.get(rank);
                result[index] = withAlpha(green, alpha(pixels[index]));
            }
        }
        return result;
    }

    /** Keeps only the bottom {@code rows} rows, so a stalk looks shorter. */
    static int[] bottomRows(int[] pixels, int size, int rows) {
        int[] result = new int[pixels.length];
        int start = (size - rows) * size;
        System.arraycopy(pixels, start, result, start, pixels.length - start);
        return result;
    }

    /**
     * Paints {@code vine} and fills each pixel the lookup map marks with the fruit texture
     * pixel whose 16-pixel coordinates the map stores in its red and green channels.
     */
    static int[] fillFruit(int[] vine, int[] map, int size, int[] fruit, int fruitSize) {
        int[] result = vine.clone();
        for (int index = 0; index < map.length; index++) {
            int lookup = map[index];
            if (alpha(lookup) == 0) {
                continue;
            }
            int x = red(lookup) * fruitSize / 16;
            int y = green(lookup) * fruitSize / 16;
            result[index] = withAlpha(fruit[y * fruitSize + x], alpha(lookup));
        }
        return result;
    }

    /** Leaf and stem greens: hue 70 to 170 degrees and saturation above 20%. */
    static boolean isStem(int pixel) {
        int red = red(pixel);
        int green = green(pixel);
        int blue = blue(pixel);
        int max = Math.max(red, Math.max(green, blue));
        int range = max - Math.min(red, Math.min(green, blue));
        if (green != max || red == max || range * 5 <= max) {
            return false;
        }
        int hueShift = 6 * (blue - red);
        return hueShift >= -5 * range && hueShift <= 5 * range;
    }

    private static int luminance(int pixel) {
        return 299 * red(pixel) + 587 * green(pixel) + 114 * blue(pixel);
    }

    private static int withAlpha(int pixel, int alpha) {
        return alpha << 24 | pixel & 0xFFFFFF;
    }

    private static int alpha(int pixel) {
        return pixel >>> 24;
    }

    private static int blue(int pixel) {
        return pixel >> 16 & 0xFF;
    }

    private static int green(int pixel) {
        return pixel >> 8 & 0xFF;
    }

    private static int red(int pixel) {
        return pixel & 0xFF;
    }
}
