package com.lombinaxmasson.gt6crops.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class CropStageArtTest {
    private static final int STEM_DARK = abgr(255, 40, 120, 30);
    private static final int STEM_LIGHT = abgr(255, 90, 200, 60);
    private static final int PETAL = abgr(255, 220, 30, 30);

    /** A 4x4 flower: a red petal on top of a three-pixel stem in column 1. */
    private static int[] flower() {
        int[] pixels = new int[16];
        pixels[1] = PETAL;
        pixels[5] = STEM_LIGHT;
        pixels[9] = STEM_DARK;
        pixels[13] = STEM_DARK;
        return pixels;
    }

    @Test
    void flowerStagesGrowTheStemThenABud() {
        int[] sprout = CropStageArt.flower(flower(), 4, 1);
        assertEquals(0, sprout[1]);
        assertEquals(0, sprout[5]);
        assertEquals(STEM_DARK, sprout[9]);
        assertEquals(STEM_DARK, sprout[13]);

        int[] stem = CropStageArt.flower(flower(), 4, 2);
        assertEquals(0, stem[1]);
        assertEquals(STEM_LIGHT, stem[5]);

        int[] bud = CropStageArt.flower(flower(), 4, 3);
        assertTrue(CropStageArt.isStem(bud[1]), "the petal becomes a green bud");
        assertEquals(STEM_LIGHT, bud[5]);
    }

    @Test
    void aFlowerWithoutGreenKeepsItsArt() {
        int[] petals = {PETAL, 0, 0, PETAL};
        assertArrayEquals(petals, CropStageArt.flower(petals, 2, 1));
    }

    @Test
    void stemsAreSaturatedGreens() {
        assertTrue(CropStageArt.isStem(STEM_DARK));
        assertFalse(CropStageArt.isStem(abgr(255, 230, 220, 40)), "yellow");
        assertFalse(CropStageArt.isStem(abgr(255, 120, 125, 120)), "grey");
        assertFalse(CropStageArt.isStem(PETAL));
    }

    @Test
    void bottomRowsCutsTheTopOff() {
        int[] cane = new int[16];
        Arrays.fill(cane, STEM_LIGHT);
        int[] stub = CropStageArt.bottomRows(cane, 4, 1);
        for (int index = 0; index < 12; index++) {
            assertEquals(0, stub[index]);
        }
        for (int index = 12; index < 16; index++) {
            assertEquals(STEM_LIGHT, stub[index]);
        }
    }

    @Test
    void fillFruitLooksUpTheFruitTexture() {
        int[] fruit = new int[32 * 32];
        for (int index = 0; index < fruit.length; index++) {
            fruit[index] = abgr(255, index % 32, index / 32, 7);
        }
        int[] vine = {STEM_DARK, 0, 0, 0};
        int[] map = {0, abgr(255, 3, 5, 0), 0, 0};
        int[] ripe = CropStageArt.fillFruit(vine, map, 2, fruit, 32);
        assertEquals(STEM_DARK, ripe[0]);
        assertEquals(fruit[10 * 32 + 6], ripe[1], "16-pixel coordinates scale to the fruit texture");
        assertEquals(0, ripe[2]);
    }

    private static int abgr(int alpha, int red, int green, int blue) {
        return alpha << 24 | blue << 16 | green << 8 | red;
    }
}
