package com.lombinaxmasson.gt6crops.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.lombinaxmasson.gt6crops.card.CropCard;
import com.lombinaxmasson.gt6crops.card.CropConditions;
import com.lombinaxmasson.gt6crops.card.ItemRef;

class CropRulesTest {
    @Test
    void variedStatsStayWithinGeneticRange() {
        CropRules.Stats first = new CropRules.Stats(0, 10, 31);
        CropRules.Stats second = new CropRules.Stats(31, 20, 0);

        CropRules.Stats child = CropBreeding.variate(
                List.of(first, second), false, CropBreeding.Variation.DEFAULT, new Random(7));

        assertTrue(child.growth() >= 0 && child.growth() <= 31);
        assertTrue(child.gain() >= 0 && child.gain() <= 31);
        assertTrue(child.resistance() >= 0 && child.resistance() <= 31);
    }

    @Test
    void fertilizerPreventsNegativeStatVariation() {
        CropRules.Stats parent = new CropRules.Stats(4, 4, 4);
        Random random = new Random(1);
        for (int attempt = 0; attempt < 40; attempt++) {
            CropRules.Stats child = CropBreeding.variate(
                    List.of(parent, parent), true, CropBreeding.Variation.DEFAULT, random);
            assertTrue(child.growth() >= 4);
            assertTrue(child.gain() >= 4);
            assertTrue(child.resistance() >= 4);
        }
    }

    @Test
    void fixedVariationAddsExactlyThatChange() {
        CropRules.Stats parent = new CropRules.Stats(10, 10, 10);
        CropBreeding.Variation plusTwo = new CropBreeding.Variation(2, 2);

        CropRules.Stats child = CropBreeding.variate(
                List.of(parent, parent), false, plusTwo, new Random(5));

        assertEquals(new CropRules.Stats(12, 12, 12), child);
    }

    @Test
    void variationHighBelowLowCountsAsLow() {
        assertEquals(new CropBreeding.Variation(3, 3), new CropBreeding.Variation(3, -1));
    }

    @Test
    void deterministicMutationBeatsThePool() {
        CropBreeding.Book book = book();
        CropBreeding.Parent wheat = parent("wheat");
        CropBreeding.Parent azure = parent("azure_bluet");

        boolean sawFlax = false;
        Random random = new Random(3);
        for (int attempt = 0; attempt < 30 && !sawFlax; attempt++) {
            CropBreeding.Outcome outcome = book.resolve(
                    List.of(wheat, azure), CropBreeding.Variation.DEFAULT, random);
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
                card, stats, new CropRules.Environment(10, 10, 10), true, true, 0);
        int deprived = CropRules.growthPoints(
                card, stats, new CropRules.Environment(0, 0, 0), false, false, 0);

        assertTrue(healthy > deprived);
    }

    @Test
    void likedBiomesSpeedUpGrowthUpToTheCap() {
        CropCard card = card("test", 2, "Crop");
        CropRules.Stats stats = new CropRules.Stats(31, 4, 4);
        CropRules.Environment environment = new CropRules.Environment(10, 10, 10);

        int none = CropRules.growthPoints(card, stats, environment, true, true, 0);
        int capped = CropRules.growthPoints(
                card, stats, environment, true, true, CropRules.MAX_LIKED_BIOMES);
        int beyondCap = CropRules.growthPoints(
                card, stats, environment, true, true, CropRules.MAX_LIKED_BIOMES + 3);

        assertTrue(capped > none);
        assertEquals(capped, beyondCap);
    }

    @Test
    void resistanceCheckAlwaysPassesAtMaxAndNeverAtZero() {
        Random random = new Random(5);
        int passed = 0;
        for (int attempt = 0; attempt < 200; attempt++) {
            assertTrue(CropRules.resists(CropRules.MAX_STAT, random));
            assertFalse(CropRules.resists(0, random));
            if (CropRules.resists(15, random)) {
                passed++;
            }
        }
        assertTrue(passed > 0 && passed < 200);
    }

    @Test
    void nutrientScoreFollowsCropsNh() {
        assertEquals(5, CropRules.nutrientScore(0, 0, 32, false, 0, 0.0F));
        assertEquals(27, CropRules.nutrientScore(32, 32, 32, true, 0, 0.0F));
        assertEquals(3 + 3 + 5, CropRules.nutrientScore(8, 8, 32, false, 0, 0.0F));
        assertEquals(5 + 28, CropRules.nutrientScore(0, 0, 32, false, 5, 0.0F));
        assertEquals(5 + 14, CropRules.nutrientScore(0, 0, 32, false, 0, 0.9F));
    }

    @Test
    void highTierCropsStarveWithoutCare() {
        int bare = CropRules.nutrientScore(0, 0, 32, true, 0, 0.4F);
        int tended = CropRules.nutrientScore(32, 32, 32, true, 0, 0.4F);

        assertFalse(CropRules.isStarving(5, 0, bare));
        assertTrue(CropRules.isStarving(6, 0, bare));
        assertFalse(CropRules.isStarving(12, 0, tended));
        assertEquals(CropRules.nutrientsNeeded(6, 0), bare + 1);
    }

    @Test
    void onlyRipeFastGrowingCropsSpreadWeeds() {
        CropCard card = card("test", 2, "Crop");
        CropRules.Stats fast = new CropRules.Stats(25, 0, 24);

        assertTrue(CropRules.spreadsWeeds(card, 4, fast));
        assertFalse(CropRules.spreadsWeeds(card, 3, fast));
        assertFalse(CropRules.spreadsWeeds(card, 4, new CropRules.Stats(24, 0, 0)));
        assertFalse(CropRules.spreadsWeeds(card, 4, new CropRules.Stats(25, 0, 25)));
    }

    @Test
    void weedsSpreadOnceHalfGrown() {
        CropCard weed = card("weed", 1, "Weed");

        assertFalse(CropRules.spreadsWeeds(weed, 1, CropRules.WEED_STATS));
        assertTrue(CropRules.spreadsWeeds(weed, 2, CropRules.WEED_STATS));
    }

    @Test
    void weedSpreadRollFavoursHighGrowth() {
        Random random = new Random(9);
        int slow = 0;
        int fast = 0;
        for (int attempt = 0; attempt < 1000; attempt++) {
            if (CropRules.shouldSpreadWeed(1, CropRules.WEED_SPREAD_CHANCE, random)) {
                slow++;
            }
            if (CropRules.shouldSpreadWeed(CropRules.MAX_STAT, CropRules.WEED_SPREAD_CHANCE, random)) {
                fast++;
            }
        }
        assertTrue(slow > 0 && slow < fast && fast < 1000);
    }

    @Test
    void fullResistanceCannotBeTrampled() {
        assertTrue(CropRules.canBeTrampled(CropRules.MAX_STAT - 1));
        assertFalse(CropRules.canBeTrampled(CropRules.MAX_STAT));
    }

    @Test
    void sprintTramplingNeedsResistanceBelowTheRoll() {
        Random random = new Random(11);
        int weak = 0;
        for (int attempt = 0; attempt < 10_000; attempt++) {
            if (CropRules.tramplesWhenSprinting(1, 0, random)) {
                weak++;
            }
            assertFalse(CropRules.tramplesWhenSprinting(1, 30, random));
        }
        assertTrue(weak > 0);
    }

    @Test
    void landingTramplesOnlyAboveThreeQuartersOfABlock() {
        Random random = new Random(13);
        int high = 0;
        for (int attempt = 0; attempt < 1000; attempt++) {
            assertFalse(CropRules.tramplesWhenLanding(CropRules.MIN_TRAMPLE_FALL, random));
            if (CropRules.tramplesWhenLanding(1.25F, random)) {
                high++;
            }
            assertTrue(CropRules.tramplesWhenLanding(2.0F, random));
        }
        assertTrue(high > 300 && high < 700);
    }

    @Test
    void machineHarvestBonusAddsItsShareOnAverage() {
        CropRules.Stats noGain = new CropRules.Stats(1, 0, 1);
        Random random = new Random(17);
        int total = 0;
        for (int attempt = 0; attempt < 10000; attempt++) {
            int count = CropRules.harvestCount(noGain, 1.05, random);
            assertTrue(count == 1 || count == 2);
            total += count;
        }
        assertTrue(total > 10300 && total < 10700);
    }

    @Test
    void machineHarvestWithoutBonusMatchesTheHandHarvest() {
        CropRules.Stats noGain = new CropRules.Stats(1, 0, 1);
        assertEquals(1, CropRules.harvestCount(noGain, 1.0, new Random(3)));
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
                id,
                CropCard.RenderShape.HASH,
                CropConditions.DEFAULT,
                List.of());
    }
}
