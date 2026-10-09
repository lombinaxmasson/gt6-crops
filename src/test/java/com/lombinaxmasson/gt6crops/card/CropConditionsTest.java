package com.lombinaxmasson.gt6crops.card;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Checks the built-in condition data as text; the test classpath has no Minecraft or Gson. */
class CropConditionsTest {
    private static final Pattern CONDITION = Pattern.compile(
            "\"(soil|sub_soil|liked_biomes)\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern ENTRY = Pattern.compile("\"#([a-z0-9_.-]+):([a-z0-9_/.-]+)\"");

    @Test
    void everyBuiltInSoilTagExists() throws Exception {
        List<String> missing = new ArrayList<>();
        for (Tag tag : tags()) {
            if (!tag.biome() && "gt6crops".equals(tag.namespace())
                    && CropConditionsTest.class.getResource(
                            "/data/gt6crops/tags/block/" + tag.path() + ".json") == null) {
                missing.add(tag.namespace() + ":" + tag.path());
            }
        }
        assertTrue(missing.isEmpty(), () -> "card tags without a tag file: " + missing);
    }

    @Test
    void everyConditionHasAnEnglishAndChineseName() throws Exception {
        String english = read("/assets/gt6crops/lang/en_us.json");
        String chinese = read("/assets/gt6crops/lang/zh_cn.json");
        List<String> missing = new ArrayList<>();
        for (Tag tag : tags()) {
            String key = "\"" + tag.translationKey() + "\":";
            if (!english.contains(key) || !chinese.contains(key)) {
                missing.add(tag.translationKey());
            }
        }
        assertTrue(missing.isEmpty(), () -> "untranslated conditions: " + missing);
    }

    /** Mirrors NeoForge's {@code Tags.getTagTranslationKey}. */
    private record Tag(boolean biome, String namespace, String path) {
        String translationKey() {
            return "tag." + (biome ? "worldgen.biome" : "block") + "." + namespace + "."
                    + path.replace('/', '.');
        }
    }

    private static Set<Tag> tags() throws Exception {
        Set<Tag> tags = new LinkedHashSet<>();
        Matcher conditions = CONDITION.matcher(read("/data/gt6crops/crop_cards.json"));
        while (conditions.find()) {
            boolean biome = "liked_biomes".equals(conditions.group(1));
            Matcher entries = ENTRY.matcher(conditions.group(2));
            while (entries.find()) {
                tags.add(new Tag(biome, entries.group(1), entries.group(2)));
            }
        }
        assertFalse(tags.isEmpty());
        return tags;
    }

    private static String read(String path) throws Exception {
        try (InputStream stream = CropConditionsTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
