package com.lombinaxmasson.gt6crops;

import com.lombinaxmasson.gt6crops.rules.CropBreeding;
import com.lombinaxmasson.gt6crops.rules.CropRules;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * CropsNH's crop and weed settings and the machines' overvoltage rule, kept in
 * {@code config/gt6crops-common.toml}. Only the server reads them.
 */
public final class Gt6CropsConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue GROWTH_MULTIPLIER;
    private static final ModConfigSpec.IntValue BREEDING_CHANCE;
    private static final ModConfigSpec.IntValue BREEDING_LOW;
    private static final ModConfigSpec.IntValue BREEDING_HIGH;
    private static final ModConfigSpec.BooleanValue ENABLE_WEEDS;
    private static final ModConfigSpec.IntValue WEED_SPAWN_CHANCE;
    private static final ModConfigSpec.IntValue WEED_SPREAD_CHANCE;
    private static final ModConfigSpec.BooleanValue OVERVOLTAGE_EXPLOSIONS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Crop growth and crossbreeding.").translation(key("crops")).push("crops");
        GROWTH_MULTIPLIER = builder
                .comment("Multiplies the growth every crop gains per growth cycle. 0 stops all growth.")
                .translation(key("growth_multiplier"))
                .defineInRange("growth_multiplier", 1.0, 0.0, 2.0);
        BREEDING_CHANCE = builder
                .comment("An empty cross tries to crossbreed one growth cycle in this many.")
                .translation(key("breeding_chance"))
                .defineInRange("breeding_chance", CropRules.BREEDING_CHANCE, 1, Integer.MAX_VALUE);
        BREEDING_LOW = builder
                .comment("The lowest change to each stat of a crossbred child, from the parents' average.")
                .translation(key("breeding_low"))
                .defineInRange("breeding_low", CropBreeding.BREEDING_LOW, -CropRules.MAX_STAT, CropRules.MAX_STAT);
        BREEDING_HIGH = builder
                .comment("The highest change to each stat of a crossbred child. Below breeding_low, it counts as breeding_low.")
                .translation(key("breeding_high"))
                .defineInRange("breeding_high", CropBreeding.BREEDING_HIGH, -CropRules.MAX_STAT, CropRules.MAX_STAT);
        builder.pop();

        builder.comment("Weeds on crop sticks.").translation(key("weeds")).push("weeds");
        ENABLE_WEEDS = builder
                .comment("Whether weeds grow on empty sticks and spread from weeds and fast-growing crops.")
                .translation(key("enable_weeds"))
                .define("enable_weeds", true);
        WEED_SPAWN_CHANCE = builder
                .comment("An empty single stick grows a weed one growth cycle in this many.")
                .translation(key("weed_spawn_chance"))
                .defineInRange("weed_spawn_chance", CropRules.WEED_SPAWN_CHANCE, 1, Integer.MAX_VALUE);
        WEED_SPREAD_CHANCE = builder
                .comment("A crop that spreads weeds does so when a roll below this, minus its growth stat, is 2 or less.")
                .translation(key("weed_spread_chance"))
                .defineInRange("weed_spread_chance", CropRules.WEED_SPREAD_CHANCE, 2, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("GT6 electric crop machines.").translation(key("machines")).push("machines");
        OVERVOLTAGE_EXPLOSIONS = builder
                .comment("Whether a machine fed more than twice its voltage explodes, as GT6CE's own machines do."
                        + " When off, the packet is lost instead.")
                .translation(key("overvoltage_explosions"))
                .define("overvoltage_explosions", true);
        builder.pop();

        SPEC = builder.build();
    }

    private Gt6CropsConfig() {}

    public static double growthMultiplier() {
        return GROWTH_MULTIPLIER.get();
    }

    public static int breedingChance() {
        return BREEDING_CHANCE.get();
    }

    public static CropBreeding.Variation breedingVariation() {
        return new CropBreeding.Variation(BREEDING_LOW.get(), BREEDING_HIGH.get());
    }

    public static boolean weedsEnabled() {
        return ENABLE_WEEDS.get();
    }

    public static int weedSpawnChance() {
        return WEED_SPAWN_CHANCE.get();
    }

    public static int weedSpreadChance() {
        return WEED_SPREAD_CHANCE.get();
    }

    public static boolean overvoltageExplosions() {
        return OVERVOLTAGE_EXPLOSIONS.get();
    }

    private static String key(String name) {
        return Gt6Crops.MODID + ".configuration." + name;
    }
}
