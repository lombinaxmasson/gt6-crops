package com.lombinaxmasson.gt6crops.item;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lombinaxmasson.gt6crops.DataFiles;

/**
 * CropsNH wheat-seed tints. The seed item is two white masks: the body takes
 * the crop's primary color and the highlight takes the secondary color.
 */
public final class SeedColors {
    private static final Gson GSON = new Gson();
    private static final int[] WHEAT = {0xB7BB3F, 0x00E210};
    private static final Map<String, int[]> COLORS = load();

    private SeedColors() {}

    public static int argb(String cardId, int tintIndex) {
        int[] pair = cardId == null ? WHEAT : COLORS.get(cardId);
        if (pair == null) {
            pair = hashed(cardId);
        }
        int rgb = pair[tintIndex <= 0 ? 0 : 1];
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static boolean has(String cardId) {
        return COLORS.containsKey(cardId);
    }

    private static int[] hashed(String cardId) {
        int hash = cardId.hashCode();
        float hue = (hash & 0x7fffffff) % 360 / 360.0f;
        return new int[] {hsl(hue, 0.55f, 0.34f), hsl(hue, 0.62f, 0.62f)};
    }

    private static int hsl(float hue, float saturation, float lightness) {
        float q = lightness < 0.5f
                ? lightness * (1.0f + saturation)
                : lightness + saturation - lightness * saturation;
        float p = 2.0f * lightness - q;
        int red = Math.round(channel(p, q, hue + 1.0f / 3.0f) * 255.0f);
        int green = Math.round(channel(p, q, hue) * 255.0f);
        int blue = Math.round(channel(p, q, hue - 1.0f / 3.0f) * 255.0f);
        return (red << 16) | (green << 8) | blue;
    }

    private static float channel(float p, float q, float t) {
        if (t < 0.0f) {
            t += 1.0f;
        }
        if (t > 1.0f) {
            t -= 1.0f;
        }
        if (t < 1.0f / 6.0f) {
            return p + (q - p) * 6.0f * t;
        }
        if (t < 1.0f / 2.0f) {
            return q;
        }
        if (t < 2.0f / 3.0f) {
            return p + (q - p) * (2.0f / 3.0f - t) * 6.0f;
        }
        return p;
    }

    private static Map<String, int[]> load() {
        try (InputStream stream = DataFiles.open("seed_colors.json")) {
            JsonObject root = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    JsonObject.class);
            Map<String, int[]> colors = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonArray pair = entry.getValue().getAsJsonArray();
                colors.put(entry.getKey(), new int[] {
                    Integer.parseInt(pair.get(0).getAsString(), 16),
                    Integer.parseInt(pair.get(1).getAsString(), 16)
                });
            }
            return Map.copyOf(colors);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load gt6crops seed colors", exception);
        }
    }
}
