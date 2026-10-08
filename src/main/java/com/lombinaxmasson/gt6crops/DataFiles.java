package com.lombinaxmasson.gt6crops;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Opens the addon's JSON data. A file of the same name in
 * {@code config/gt6crops/} replaces the copy shipped in the jar.
 */
public final class DataFiles {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DataFiles() {}

    public static InputStream open(String name) throws IOException {
        return open(FMLPaths.CONFIGDIR.get(), name);
    }

    /** {@code config} is {@code null} outside a game, such as in unit tests. */
    static InputStream open(Path config, String name) throws IOException {
        Path override = config == null ? null : config.resolve(Gt6Crops.MODID).resolve(name);
        if (override != null && Files.isRegularFile(override)) {
            LOGGER.info("gt6crops: using {} from {}", name, override);
            return Files.newInputStream(override);
        }
        InputStream stream = DataFiles.class.getResourceAsStream("/data/gt6crops/" + name);
        if (stream == null) {
            throw new IllegalStateException("Missing gt6crops " + name);
        }
        return stream;
    }
}
