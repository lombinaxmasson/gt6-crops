package com.lombinaxmasson.gt6crops.machine;

import com.lombinaxmasson.gt6crops.rules.CropRules;

/** CropsNH's Seed Generator numbers, apart from the block entity so they can be tested. */
public final class SeedGeneratorRules {
    public static final int FERTILIZER_PER_STAT = 36;
    /** GT5u's basic machine tank: 16 buckets at LV, doubling per tier up to 256 at IV. */
    private static final int MAX_CAPACITY = 256_000;
    /** CropsNH gives seed generators below IV a tenth of GT5u's tank. */
    private static final int SMALL_TANK_DIVISOR = 10;

    private SeedGeneratorRules() {}

    public static int capacity(MachineTier tier) {
        int full = Math.min(MAX_CAPACITY, 8_000 << tier.index());
        return tier.compareTo(MachineTier.IV) < 0 ? full / SMALL_TANK_DIVISOR : full;
    }

    public static int fertilizerCost(CropRules.Stats stats) {
        return Math.max(1, (stats.growth() + stats.gain() + stats.resistance()) * FERTILIZER_PER_STAT);
    }

    /** The highest total stats a seed may have for this tier's tank to hold its cost. */
    public static int maxTotalStats(MachineTier tier) {
        return capacity(tier) / FERTILIZER_PER_STAT;
    }
}
