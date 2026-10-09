package com.lombinaxmasson.gt6crops.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;

/**
 * Immutable crop-card metadata. Runtime state (age and genetic values) lives
 * in {@code CropTile}; this class only describes a species.
 */
public record CropCard(
        String id,
        String name,
        String discoveredBy,
        ItemRef drop,
        List<ItemRef> specialDrops,
        ItemRef baseSeed,
        boolean crossbreedOnly,
        int tier,
        int maxSize,
        int growthSpeed,
        int afterHarvestSize,
        int harvestSize,
        int statChemical,
        int statFood,
        int statDefensive,
        int statColor,
        int statWeed,
        List<String> attributes,
        String texture,
        RenderShape renderShape,
        CropConditions conditions) {

    /** How the stage sheets stand in the crop stick, after CropsNH's plant render shapes. */
    public enum RenderShape {
        /** Two sheets each way at 4 and 12 pixels, like vanilla crops. */
        HASH,
        /** Two diagonal sheets, like vanilla flowers and sugar cane. */
        X
    }

    public static CropCard fromJson(JsonObject json) {
        List<ItemRef> specials = new ArrayList<>();
        if (json.has("special_drops") && !json.get("special_drops").isJsonNull()) {
            JsonArray array = json.getAsJsonArray("special_drops");
            array.forEach(value -> specials.add(ItemRef.fromJson(value.getAsJsonObject())));
        }
        int[] stats = stats(json);
        return new CropCard(
                json.get("id").getAsString(),
                json.get("name").getAsString(),
                string(json, "discovered_by", "gt6crops"),
                ItemRef.fromJson(json.getAsJsonObject("drop")),
                List.copyOf(specials),
                ItemRef.fromJson(json.getAsJsonObject("base_seed")),
                json.get("crossbreed_only").getAsBoolean(),
                Math.max(1, json.get("tier").getAsInt()),
                Math.max(3, json.get("max_size").getAsInt()),
                Math.max(0, json.get("growth_speed").getAsInt()),
                Math.max(1, json.get("after_harvest_size").getAsInt()),
                Math.max(2, json.get("harvest_size").getAsInt()),
                stats[0],
                stats[1],
                stats[2],
                stats[3],
                stats[4],
                attributes(json),
                string(json, "texture", json.get("id").getAsString().replace("_", "")),
                renderShape(json),
                CropConditions.fromJson(json));
    }

    public boolean canGrow(int size) {
        return size < maxSize;
    }

    public boolean canHarvest(int size) {
        return size >= harvestSize;
    }

    /** CropsNH default: a crop can cross or breed once it reaches 80% of its max size. */
    public boolean canCross(int size) {
        return size * 5 >= maxSize * 4;
    }

    public boolean canBreed(int size) {
        return canCross(size);
    }

    public boolean isWeed() {
        return "weed".equals(id);
    }

    public boolean hasPlantableBaseSeed() {
        return !crossbreedOnly && !baseSeed.isNone() && baseSeed.resolve().isPresent();
    }

    public boolean matchesBaseSeed(ItemStack stack) {
        return !crossbreedOnly && baseSeed.matches(stack);
    }

    public Optional<ItemStack> harvestDrop() {
        return drop.resolve();
    }

    public List<ItemStack> extraDrops(RandomSource random) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemRef special : specialDrops) {
            if (random.nextInt(4) == 0) {
                special.resolve().ifPresent(result::add);
            }
        }
        return result;
    }

    public int[] cardStats() {
        return new int[] {statChemical, statFood, statDefensive, statColor, statWeed};
    }

    private static int[] stats(JsonObject json) {
        if (json.has("stats")) {
            JsonArray values = json.getAsJsonArray("stats");
            int[] result = new int[5];
            for (int index = 0; index < result.length && index < values.size(); index++) {
                result[index] = values.get(index).getAsInt();
            }
            return result;
        }
        return new int[] {
            integer(json, "stat_chemical"),
            integer(json, "stat_food"),
            integer(json, "stat_defensive"),
            integer(json, "stat_color"),
            integer(json, "stat_weed")
        };
    }

    private static RenderShape renderShape(JsonObject json) {
        String shape = string(json, "render_shape", "hash");
        return switch (shape) {
            case "hash" -> RenderShape.HASH;
            case "x" -> RenderShape.X;
            default -> throw new IllegalArgumentException("Unknown render_shape " + shape
                    + " on crop card " + json.get("id").getAsString());
        };
    }

    private static List<String> attributes(JsonObject json) {
        List<String> result = new ArrayList<>();
        if (json.has("attributes")) {
            json.getAsJsonArray("attributes")
                    .forEach(value -> result.add(value.getAsString()));
        }
        return List.copyOf(result);
    }

    private static int integer(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsInt() : 0;
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString()
                : fallback;
    }
}
