package com.lombinaxmasson.gt6crops.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.ItemRef;

class CropRulesTest {
    @Test
    void variedStatsStayWithinGeneticRange() {
        CropRules.Stats first = new CropRules.Stats(0, 10, 31);
        CropRules.Stats second = new CropRules.Stats(31, 20, 0);

        CropRules.Stats child = CropBreeding.variate(
                List.of(first, second), false, new Random(7));

        assertTrue(child.growth() >= 0 && child.growth() <= 31);
        assertTrue(child.gain() >= 0 && child.gain() <= 31);
        assertTrue(child.resistance() >= 0 && child.resistance() <= 31);
    }

    @Test
    void fertilizerPreventsNegativeStatVariation() {
        CropRules.Stats parent = new CropRules.Stats(4, 4, 4);
        Random random = new Random(1);
        for (int attempt = 0; attempt < 40; attempt++) {
            CropRules.Stats child = CropBreeding.variate(List.of(parent, parent), true, random);
            assertTrue(child.growth() >= 4);
            assertTrue(child.gain() >= 4);
            assertTrue(child.resistance() >= 4);
        }
    }

    @Test
    void deterministicMutationBeatsThePool() {
        CropBreeding.Book book = book();
        CropBreeding.Parent wheat = parent("wheat");
        CropBreeding.Parent azure = parent("azure_bluet");

        boolean sawFlax = false;
        Random random = new Random(3);
        for (int attempt = 0; attempt < 30 && !sawFlax; attempt++) {
            CropBreeding.Outcome outcome = book.resolve(List.of(wheat, azure), random);
            if (outcome != null && "flax".equals(outcome.cardId())) {
                sawFlax = true;
            }
        }
        assertTrue(sawFlax);
    }

    @Test
    void mutationBookContainsTheSugarCaneRecipe() {
        CropBreeding.Book book = book();
        assertTrue(book.mutations().stream().anyMatch(mutation ->
                "sugar_cane".equals(mutation.child())
                        && mutation.parents().contains("carrot")
                        && mutation.parents().contains("potato")));
        assertNotNull(book);
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

    private static CropBreeding.Book book() {
        try (InputStream stream = CropRulesTest.class.getResourceAsStream(
                "/data/gt6crops/mutations.json")) {
            assertNotNull(stream);
            return CropBreeding.Book.load(stream, List.of(
                    "wheat", "azure_bluet", "carrot", "potato", "sugar_cane", "flax"));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static CropBreeding.Parent parent(String id) {
        return new CropBreeding.Parent(
                id, new CropRules.Stats(8, 8, 8), true, true, false);
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
