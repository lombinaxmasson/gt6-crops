package com.lombinaxmasson.gt6crops.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    public record Child(CropCard card, Stats stats) {}

    public static Stats initialStats(CropCard card, Random random) {
        int tierBonus = Math.min(4, Math.max(0, card.tier() / 4));
        return new Stats(
                1 + random.nextInt(5) + tierBonus,
                1 + random.nextInt(5) + tierBonus,
                1 + random.nextInt(5) + tierBonus);
    }

    public static Stats inherit(Stats first, Stats second, Random random) {
        return new Stats(
                inherited(first.growth(), second.growth(), random),
                inherited(first.gain(), second.gain(), random),
                inherited(first.resistance(), second.resistance(), random));
    }

    public static CropCard chooseChild(
            CropCard first,
            CropCard second,
            Collection<CropCard> available,
            Random random) {
        List<CropCard> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (CropCard candidate : available) {
            int weight = affinity(first, second, candidate);
            if (weight > 0) {
                candidates.add(candidate);
                weights.add(weight);
                total += weight;
            }
        }
        if (candidates.isEmpty()) {
            return first;
        }
        int selected = random.nextInt(total);
        for (int index = 0; index < candidates.size(); index++) {
            selected -= weights.get(index);
            if (selected < 0) {
                return candidates.get(index);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    /**
     * Higher values mean that a card is a more plausible child of the two
     * parents. Same-card reproduction gets the classic IC2-style strong bias.
     */
    public static int affinity(CropCard first, CropCard second, CropCard child) {
        if (child.isWeed()) {
            return 0;
        }
        int score = first.id().equals(child.id()) || second.id().equals(child.id()) ? 500 : 1;
        int sharedWithFirst = sharedAttributes(first, child);
        int sharedWithSecond = sharedAttributes(second, child);
        score += (sharedWithFirst + sharedWithSecond) * 5;

        int[] childStats = child.cardStats();
        int[] firstStats = first.cardStats();
        int[] secondStats = second.cardStats();
        int difference = 0;
        for (int index = 0; index < childStats.length; index++) {
            difference += Math.abs(childStats[index] - firstStats[index]);
            difference += Math.abs(childStats[index] - secondStats[index]);
        }
        score += Math.max(0, 40 - difference);
        score -= Math.abs(child.tier() - first.tier()) + Math.abs(child.tier() - second.tier());
        return Math.max(1, score);
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

    private static int inherited(int first, int second, Random random) {
        return clamp((first + second) / 2 + random.nextInt(3) - 1);
    }

    private static int sharedAttributes(CropCard first, CropCard second) {
        int result = 0;
        for (String attribute : first.attributes()) {
            if (second.attributes().contains(attribute)) {
                result++;
            }
        }
        return result;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_STAT, value));
    }

    private static int clampEnvironment(int value) {
        return Math.max(0, Math.min(10, value));
    }
}
