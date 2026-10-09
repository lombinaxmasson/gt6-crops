package com.lombinaxmasson.gt6crops.rules;

import java.util.Random;

import com.lombinaxmasson.gt6crops.card.CropCard;

/**
 * The deterministic part of the crop simulation.
 *
 * <p>Keeping these calculations independent from Minecraft makes the
 * crossbreeding rules executable in unit tests and gives the block entity a
 * small, auditable state-transition surface.</p>
 */
public final class CropRules {
    public static final int MAX_STAT = 31;
    public static final int MAX_ENVIRONMENT = 10;
    public static final int MAX_LIKED_BIOMES = 2;
    private static final double LIKED_BIOME_BONUS = 0.25;

    private static final int NUTRIENT_BASE = 5;
    private static final int NUTRIENT_SKY_BONUS = 2;
    private static final int NUTRIENT_BIOME_BONUS = 14;
    private static final float LOW_DOWNFALL = 0.5F;
    private static final float HIGH_DOWNFALL = 0.8F;
    private static final int NUTRIENT_SCALE = 5;
    private static final int NUTRIENTS_PER_TIER = 10;
    private static final int BASE_GROWTH_SPEED = 6;

    private CropRules() {}

    public record Stats(int growth, int gain, int resistance) {
        public Stats {
            growth = clamp(growth);
            gain = clamp(gain);
            resistance = clamp(resistance);
        }
    }

    public record Environment(int nutrients, int humidity, int airQuality) {
        public Environment {
            nutrients = clampEnvironment(nutrients);
            humidity = clampEnvironment(humidity);
            airQuality = clampEnvironment(airQuality);
        }

        public double growthFactor() {
            double average = (nutrients + humidity + airQuality) / 30.0;
            return 0.25 + average * 0.75;
        }
    }

    public static Stats initialStats(CropCard card, Random random) {
        int tierBonus = Math.min(4, Math.max(0, card.tier() / 4));
        return new Stats(
                1 + random.nextInt(5) + tierBonus,
                1 + random.nextInt(5) + tierBonus,
                1 + random.nextInt(5) + tierBonus);
    }

    public static int growthThreshold(CropCard card) {
        int configured = card.growthSpeed() > 0 ? card.growthSpeed() : card.tier() * 200;
        return Math.max(20, configured);
    }

    public static int growthPoints(
            CropCard card,
            Stats stats,
            Environment environment,
            boolean hasNutrients,
            boolean hasWater,
            int likedBiomes) {
        double resources = hasNutrients && hasWater ? 1.0 : 0.35;
        double biome = 1.0 + Math.min(MAX_LIKED_BIOMES, Math.max(0, likedBiomes)) * LIKED_BIOME_BONUS;
        double points = (1.0 + stats.growth() / 4.0)
                * environment.growthFactor()
                * resources
                * biome
                * (1.0 + Math.max(0, card.maxSize() - 3) * 0.05);
        return Math.max(1, (int) Math.round(points));
    }

    public static boolean shouldAttemptCrossbreed(Random random) {
        return random.nextInt(3) == 0;
    }

    public static boolean shouldSpawnWeed(Random random) {
        return random.nextInt(100) == 0;
    }

    public static boolean shouldSpreadWeed(int resistance, Random random) {
        return random.nextInt(MAX_STAT + 1) >= resistance;
    }

    public static int harvestCount(Stats stats, Random random) {
        int bonus = stats.gain() / 8;
        return 1 + bonus + (random.nextInt(32) < stats.gain() ? 1 : 0);
    }

    /**
     * CropsNH's resistance check, passed when resistance beats a roll from 0 to 30.
     * It decides whether a crop removed without a spade keeps its seed, and whether
     * a starving or exposed crop escapes disease.
     */
    public static boolean resists(int resistance, Random random) {
        return resistance > random.nextInt(MAX_STAT);
    }

    /**
     * CropsNH's nutrient score: 5, up to 10 each from stored water and fertilizer,
     * 2 under open sky, and 14 per liked biome (at most two) or up to 14 in a humid
     * biome, whichever is more.
     */
    public static int nutrientScore(
            int water,
            int fertilizer,
            int maxStorage,
            boolean seesSky,
            int likedBiomes,
            float downfall) {
        int score = NUTRIENT_BASE
                + storageBonus(water, maxStorage)
                + storageBonus(fertilizer, maxStorage)
                + (seesSky ? NUTRIENT_SKY_BONUS : 0);
        float humidity = Math.max(0.0F, Math.min(1.0F,
                (downfall - LOW_DOWNFALL) / (HIGH_DOWNFALL - LOW_DOWNFALL)));
        int liked = Math.min(MAX_LIKED_BIOMES, Math.max(0, likedBiomes));
        return score + Math.max((int) (humidity * NUTRIENT_BIOME_BONUS), liked * NUTRIENT_BIOME_BONUS);
    }

    /**
     * CropsNH: a crop starves when its nutrient score is so far below what its
     * tier needs that its growth rate reaches zero. A starving crop does not grow.
     */
    public static boolean isStarving(int tier, int growth, int nutrientScore) {
        int points = nutrientScore * NUTRIENT_SCALE;
        int need = tier * NUTRIENTS_PER_TIER;
        if (points >= need) {
            return false;
        }
        return (BASE_GROWTH_SPEED + growth) * (100 - (need - points) * 4) / 100 <= 0;
    }

    /** The lowest nutrient score at which a crop of this tier and growth stat does not starve. */
    public static int nutrientsNeeded(int tier, int growth) {
        int score = 0;
        while (isStarving(tier, growth, score)) {
            score++;
        }
        return score;
    }

    private static int storageBonus(int storage, int maxStorage) {
        int percent = Math.max(0, Math.min(maxStorage, storage)) * 100 / maxStorage;
        return (percent + 9) / 10;
    }

    public static boolean isEnvironmentHealthy(Environment environment) {
        return environment.nutrients() >= 5
                && environment.humidity() >= 5
                && environment.airQuality() >= 5;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_STAT, value));
    }

    private static int clampEnvironment(int value) {
        return Math.max(0, Math.min(MAX_ENVIRONMENT, value));
    }
}
