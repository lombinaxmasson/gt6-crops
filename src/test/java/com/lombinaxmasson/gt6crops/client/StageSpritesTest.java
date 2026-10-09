package com.lombinaxmasson.gt6crops.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/** Stage sprites hold only the plant; the crop sticks come from the block model. */
class StageSpritesTest {
    /** The stick brown that IC2-era crop art, including GT6's, paints into every stage. */
    private static final int PAINTED_STICK = 0x684E1E;

    @Test
    void noStageSpritePaintsCropSticks() throws Exception {
        URL resource = StageSpritesTest.class.getResource("/assets/gt6crops/textures/block/crop");
        assertNotNull(resource);
        Path root = Path.of(resource.toURI());
        List<String> painted = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".png")).toList()) {
                if (paintsSticks(ImageIO.read(file.toFile()))) {
                    painted.add(root.relativize(file).toString());
                }
            }
        }
        assertTrue(painted.isEmpty(), () -> "stage sprites with painted crop sticks: " + painted);
    }

    private static boolean paintsSticks(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if (argb >>> 24 != 0 && (argb & 0xFFFFFF) == PAINTED_STICK) {
                    return true;
                }
            }
        }
        return false;
    }
}
