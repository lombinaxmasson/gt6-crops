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
            boolean hasWater) {
        double resources = hasNutrients && hasWater ? 1.0 : 0.35;
        double points = (1.0 + stats.growth() / 4.0)
                * environment.growthFactor()
                * resources
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

    public static boolean isEnvironmentHealthy(Environment environment) {
        return environment.nutrients() >= 5
                && environment.humidity() >= 5
                && environment.airQuality() >= 5;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_STAT, value));
    }

    private static int clampEnvironment(int value) {
        return Math.max(0, Math.min(10, value));
    }
}
