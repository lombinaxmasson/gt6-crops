package com.lombinaxmasson.gt6crops.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.ItemRef;

class CropRulesTest {
    @Test
    void inheritedStatsStayWithinGeneticRange() {
        CropRules.Stats first = new CropRules.Stats(0, 10, 31);
        CropRules.Stats second = new CropRules.Stats(31, 20, 0);

        CropRules.Stats child = CropRules.inherit(first, second, new Random(7));

        assertTrue(child.growth() >= 0 && child.growth() <= 31);
        assertTrue(child.gain() >= 0 && child.gain() <= 31);
        assertTrue(child.resistance() >= 0 && child.resistance() <= 31);
    }

    @Test
    void sameSpeciesHasTheStrongReproductionWeight() {
        CropCard first = card("first", 2, "Flower");
        CropCard second = card("second", 2, "Flower");
        CropCard same = card("first", 2, "Flower");
        CropCard unrelated = card("unrelated", 9, "Reed");

        assertTrue(CropRules.affinity(first, second, same)
                > CropRules.affinity(first, second, unrelated));
    }

    @Test
    void environmentalResourcesChangeGrowthPoints() {
        CropCard card = card("test", 2, "Crop");
        CropRules.Stats stats = new CropRules.Stats(8, 4, 4);

        int healthy = CropRules.growthPoints(
                card, stats, new CropRules.Environment(10, 10, 10), true, true);
        int deprived = CropRules.growthPoints(
                card, stats, new CropRules.Environment(0, 0, 0), false, false);

        assertTrue(healthy > deprived);
    }

    @Test
    void growthThresholdUsesTierWhenCardHasNoOverride() {
        assertEquals(600, CropRules.growthThreshold(card("test", 3, "Crop")));
    }

    private static CropCard card(String id, int tier, String attribute) {
        return new CropCard(
                id,
                id,
                "test",
                ItemRef.NONE,
                List.of(),
                ItemRef.NONE,
                true,
                tier,
                4,
                0,
                1,
                4,
                1,
                1,
                1,
                1,
                1,
                List.of(attribute),
                id);
    }
}
