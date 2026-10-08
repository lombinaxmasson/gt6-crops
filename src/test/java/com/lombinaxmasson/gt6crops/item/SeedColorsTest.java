package com.lombinaxmasson.gt6crops.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class SeedColorsTest {
    private static final Pattern CROP_ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");

    @Test
    void everyCropUsesAWheatSeedTint() throws Exception {
        List<String> missing = new ArrayList<>();
        try (var stream = SeedColorsTest.class.getResourceAsStream("/data/gt6crops/crop_cards.json")) {
            assertNotNull(stream);
            String ledger = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher ids = CROP_ID.matcher(ledger);
            assertTrue(ids.find());
            do {
                String id = ids.group(1);
                if (!SeedColors.has(id)) {
                    missing.add(id);
                }
            } while (ids.find());
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
