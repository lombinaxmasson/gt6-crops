package com.lombinaxmasson.gt6crops.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

class SeedColorsTest {
    @Test
    void everyCropUsesAWheatSeedTint() throws Exception {
        List<String> missing = new ArrayList<>();
        try (var stream = SeedColorsTest.class.getResourceAsStream("/data/gt6crops/crop_cards.json")) {
            assertNotNull(stream);
            JsonElement root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (JsonElement crop : root.getAsJsonObject().getAsJsonArray("crops")) {
                String id = crop.getAsJsonObject().get("id").getAsString();
                if (!SeedColors.has(id)) {
                    missing.add(id);
                }
            }
        }
        assertTrue(missing.isEmpty(), () -> "crops without seed colors: " + missing);
    }

    @Test
    void wheatUsesTheCropsNhPair() {
        assertEquals(0xFFB7BB3F, SeedColors.argb("wheat", 0));
        assertEquals(0xFF00E210, SeedColors.argb("wheat", 1));
        assertEquals(0xFFB7BB3F, SeedColors.argb(null, 0));
    }
}
