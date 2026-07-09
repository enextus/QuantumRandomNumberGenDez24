package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.mode.VisualizationCategory;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.ThreeDotsSierpinski.rng.RNProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JLabel;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DotController image saving")
class DotControllerSaveImagesTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("saveImages() returns zero before the first rendered frame")
    void saveImagesReturnsZeroBeforeFirstRenderedFrame() throws Exception {
        RNProvider provider = new RNProvider(testSettings(), false, Thread::sleep, false);
        DotController controller = new DotController(provider, new NoOpVisualizationMode(), new JLabel());

        try {
            int saved = controller.saveImages(tempDir.toFile(), "before-first-render");

            assertEquals(0, saved, "No files should be saved before offscreen image initialization");
            try (var files = Files.list(tempDir)) {
                assertTrue(files.findAny().isEmpty(), "saveImages() must not create files when there is no image");
            }
        } finally {
            controller.shutdown();
            provider.shutdown();
        }
    }

    private static RNProvider.ProviderSettings testSettings() {
        return new RNProvider.ProviderSettings(
                "http://127.0.0.1:1", "test-key", "uint16",
                1, 1, 1,
                100, 100, 0,
                0, 1L, 1L
        );
    }

    private static final class NoOpVisualizationMode implements VisualizationMode {

        @Override
        public String getId() {
            return "noop";
        }

        @Override
        public String getName() {
            return "No-op";
        }

        @Override
        public String getDescription() {
            return "No-op test mode";
        }

        @Override
        public String getIcon() {
            return "·";
        }

        @Override
        public void initialize(BufferedImage canvas, int width, int height) {
            // No-op.
        }

        @Override
        public List<Point> step(RNProvider provider, BufferedImage canvas, int dotSize) {
            return List.of();
        }

        @Override
        public int getPointCount() {
            return 0;
        }

        @Override
        public int getRandomNumbersUsed() {
            return 0;
        }

        @Override
        public VisualizationCategory getCategory() {
            return VisualizationCategory.CHAOS_FRACTALS;
        }
    }
}
