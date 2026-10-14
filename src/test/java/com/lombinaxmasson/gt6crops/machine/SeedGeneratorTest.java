package com.lombinaxmasson.gt6crops.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.lombinaxmasson.gt6crops.rules.CropRules;

class SeedGeneratorTest {
    @Test
    void tanksAreATenthOfGt5uBelowIv() {
        assertEquals(1_600, SeedGeneratorRules.capacity(MachineTier.LV));
        assertEquals(3_200, SeedGeneratorRules.capacity(MachineTier.MV));
        assertEquals(6_400, SeedGeneratorRules.capacity(MachineTier.HV));
        assertEquals(12_800, SeedGeneratorRules.capacity(MachineTier.EV));
        assertEquals(256_000, SeedGeneratorRules.capacity(MachineTier.IV));
    }

    @Test
    void copiesCost36MillibucketsPerStatPoint() {
        assertEquals(1, SeedGeneratorRules.fertilizerCost(new CropRules.Stats(0, 0, 0)));
        assertEquals(36 * 3, SeedGeneratorRules.fertilizerCost(new CropRules.Stats(1, 1, 1)));
        assertEquals(3_348, SeedGeneratorRules.fertilizerCost(new CropRules.Stats(31, 31, 31)));
    }

    @Test
    void onlyLvAndMvAreTooSmallForMaxedSeeds() {
        assertEquals(44, SeedGeneratorRules.maxTotalStats(MachineTier.LV));
        assertEquals(88, SeedGeneratorRules.maxTotalStats(MachineTier.MV));
        assertEquals(177, SeedGeneratorRules.maxTotalStats(MachineTier.HV));
    }
}
