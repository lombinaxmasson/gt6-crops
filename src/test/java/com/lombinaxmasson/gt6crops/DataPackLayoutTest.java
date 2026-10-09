package com.lombinaxmasson.gt6crops;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Minecraft 1.21 renamed the plural data-pack folders and silently ignores files under the old names. */
class DataPackLayoutTest {
    private static final List<String> PRE_1_21_FOLDERS = List.of(
            "advancements", "functions", "item_modifiers", "loot_tables", "predicates", "recipes", "structures",
            "tags/blocks", "tags/entity_types", "tags/fluids", "tags/functions", "tags/game_events", "tags/items");

    @Test
    void noDataFolderUsesAPre121Name() throws Exception {
        URL resource = DataPackLayoutTest.class.getResource("/data");
        assertNotNull(resource);
        Path root = Path.of(resource.toURI());
        List<String> stale = new ArrayList<>();
        try (Stream<Path> namespaces = Files.list(root)) {
            for (Path namespace : namespaces.filter(Files::isDirectory).toList()) {
                for (String folder : PRE_1_21_FOLDERS) {
                    if (Files.exists(namespace.resolve(folder))) {
                        stale.add(root.relativize(namespace.resolve(folder)).toString());
                    }
                }
            }
        }
        assertTrue(stale.isEmpty(), () -> "data folders that Minecraft 1.21 ignores: " + stale);
    }
}
