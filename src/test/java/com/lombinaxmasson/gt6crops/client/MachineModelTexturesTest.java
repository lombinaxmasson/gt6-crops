package com.lombinaxmasson.gt6crops.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Machine models draw GT6CE's casing straight from the GT6CE jar, so a casing
 * GT6CE moves or renames would only show up in game as the missing texture.
 */
class MachineModelTexturesTest {
    private static final Pattern TEXTURES = Pattern.compile("\"textures\"\\s*:\\s*\\{([^}]*)}");
    private static final Pattern TEXTURE = Pattern.compile("\"[^\"]+\"\\s*:\\s*\"([a-z0-9_.-]+):([a-z0-9_./-]+)\"");

    @Test
    void everyMachineModelTextureExists() throws Exception {
        URL resource = MachineModelTexturesTest.class.getResource("/assets/gt6crops/models/block/machine");
        assertNotNull(resource);
        Path root = Path.of(resource.toURI());
        List<String> checked = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                Matcher textures = TEXTURES.matcher(Files.readString(file));
                if (!textures.find()) {
                    continue;
                }
                Matcher texture = TEXTURE.matcher(textures.group(1));
                while (texture.find()) {
                    String path = "/assets/" + texture.group(1) + "/textures/" + texture.group(2) + ".png";
                    checked.add(path);
                    if (MachineModelTexturesTest.class.getResource(path) == null) {
                        missing.add(root.relativize(file) + " -> " + texture.group(1) + ":" + texture.group(2));
                    }
                }
            }
        }
        assertTrue(checked.stream().anyMatch(path -> path.startsWith("/assets/gregtech/")), "no GT6CE casing checked");
        assertTrue(missing.isEmpty(), () -> "machine model textures not on the classpath: " + missing);
    }
}
