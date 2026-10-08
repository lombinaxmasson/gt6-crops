package com.lombinaxmasson.gt6crops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataFilesTest {
    @TempDir
    Path config;

    @Test
    void configFileReplacesTheBuiltInCopy() throws Exception {
        Path dir = Files.createDirectories(config.resolve("gt6crops"));
        Files.writeString(dir.resolve("seed_colors.json"), "{\"custom\": [\"000000\", \"FFFFFF\"]}");

        assertEquals("{\"custom\": [\"000000\", \"FFFFFF\"]}", read(config, "seed_colors.json"));
    }

    @Test
    void otherFilesKeepTheBuiltInCopy() throws Exception {
        Path dir = Files.createDirectories(config.resolve("gt6crops"));
        Files.writeString(dir.resolve("seed_colors.json"), "{}");

        assertTrue(read(config, "mutations.json").contains("\"pools\""));
    }

    @Test
    void noConfigDirectoryUsesTheBuiltInCopy() throws Exception {
        assertTrue(read(null, "crop_cards.json").contains("\"galvania\""));
    }

    private static String read(Path config, String name) throws Exception {
        try (InputStream stream = DataFiles.open(config, name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
