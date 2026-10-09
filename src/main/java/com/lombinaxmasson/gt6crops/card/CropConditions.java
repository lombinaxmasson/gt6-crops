package com.lombinaxmasson.gt6crops.card;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/**
 * Where a crop can be planted and what it needs to keep growing. The soil is
 * the block under the crop stick and the sub-soil is the block under the soil.
 */
public record CropConditions(
        List<RegistryRef<Block>> soil,
        List<RegistryRef<Block>> subSoil,
        int minLight,
        int maxLight,
        List<RegistryRef<Biome>> likedBiomes) {
    public static final int MAX_LIGHT = 15;
    public static final CropConditions DEFAULT = new CropConditions(
            List.of(RegistryRef.parse(Registries.BLOCK, "#gt6crops:soil/farmland")),
            List.of(),
            0,
            MAX_LIGHT,
            List.of());

    public static CropConditions fromJson(JsonObject json) {
        return new CropConditions(
                json.has("soil") ? refs(json, "soil", Registries.BLOCK) : DEFAULT.soil(),
                refs(json, "sub_soil", Registries.BLOCK),
                light(json, "min_light", 0),
                light(json, "max_light", MAX_LIGHT),
                refs(json, "liked_biomes", Registries.BIOME));
    }

    public boolean tooDark(int light) {
        return light < minLight;
    }

    public boolean tooBright(int light) {
        return light > maxLight;
    }

    private static <T> List<RegistryRef<T>> refs(
            JsonObject json, String key, ResourceKey<? extends Registry<T>> registry) {
        List<RegistryRef<T>> result = new ArrayList<>();
        if (json.has(key)) {
            json.getAsJsonArray(key)
                    .forEach(value -> result.add(RegistryRef.parse(registry, value.getAsString())));
        }
        return List.copyOf(result);
    }

    private static int light(JsonObject json, String key, int fallback) {
        return json.has(key)
                ? Math.max(0, Math.min(MAX_LIGHT, json.get(key).getAsInt()))
                : fallback;
    }
}
