package org.ThreeDotsSierpinski.mode.montecarlo;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.mode.chaos.*;
import org.ThreeDotsSierpinski.mode.montecarlo.*;
import org.ThreeDotsSierpinski.mode.physics.*;
import org.ThreeDotsSierpinski.mode.stochastic.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MonteCarloMandelbrot3DAreaMode")
@Tag("fast")
class MonteCarloMandelbrot3DAreaModeTest {

    private static final int CANVAS_WIDTH = 420;
    private static final int CANVAS_HEIGHT = 320;
    private static final int DOT_SIZE = 2;

    @Test
    @DisplayName("Metadata and rendering flags are correct")
    void metadataAndFlagsAreCorrect() {
        MonteCarloMandelbrot3DAreaMode mode = new MonteCarloMandelbrot3DAreaMode();

        assertEquals("monte-carlo-mandelbrot-3d-area", mode.getId());
        assertEquals("Monte Carlo Mandelbrot 3D Area", mode.getName());
        assertFalse(mode.getDescription().isBlank());
        assertFalse(mode.getIcon().isBlank());

        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
    }

    @Test
    @DisplayName("Mode consumes random pairs and counts samples")
    void consumesRandomPairsAndCountsSamples() {
        MonteCarloMandelbrot3DAreaMode mode = new MonteCarloMandelbrot3DAreaMode();
        BufferedImage canvas = newCanvas();

        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);
        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertTrue(mode.getPointCount() > 0);
        assertEquals(mode.getPointCount() * 2, mode.getRandomNumbersUsed());
    }

    @Test
    @DisplayName("Empty provider does not crash and consumes nothing")
    void emptyProviderDoesNotCrash() {
        MonteCarloMandelbrot3DAreaMode mode = new MonteCarloMandelbrot3DAreaMode();
        BufferedImage canvas = newCanvas();

        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);

        assertDoesNotThrow(() -> mode.step(emptyProvider(), canvas, DOT_SIZE));
        assertEquals(0, mode.getPointCount());
        assertEquals(0, mode.getRandomNumbersUsed());
    }

    @Test
    @DisplayName("Redraw does not consume random numbers")
    void redrawDoesNotConsumeRandomNumbers() {
        MonteCarloMandelbrot3DAreaMode mode = new MonteCarloMandelbrot3DAreaMode();
        BufferedImage canvas = newCanvas();

        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);
        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        int pointsBefore = mode.getPointCount();
        int randomBefore = mode.getRandomNumbersUsed();

        mode.redraw(canvas, CANVAS_WIDTH, CANVAS_HEIGHT, DOT_SIZE);

        assertEquals(pointsBefore, mode.getPointCount());
        assertEquals(randomBefore, mode.getRandomNumbersUsed());
    }

    @Test
    @DisplayName("Controls are available")
    void controlsAreAvailable() {
        MonteCarloMandelbrot3DAreaMode mode = new MonteCarloMandelbrot3DAreaMode();

        List<JComponent> controls = mode.createModeControls(null);

        assertFalse(controls.isEmpty());
        assertTrue(controls.stream().anyMatch(JButton.class::isInstance));
        assertTrue(controls.stream().anyMatch(JComboBox.class::isInstance));
        assertTrue(controls.stream().anyMatch(JCheckBox.class::isInstance));
        assertTrue(controls.stream().anyMatch(JSlider.class::isInstance));
    }

    @Test
    @DisplayName("Registry contains Monte Carlo Mandelbrot 3D mode")
    void registryContainsMode() {
        boolean found = Arrays.stream(VisualizationModes.all())
                .anyMatch(mode -> "monte-carlo-mandelbrot-3d-area".equals(mode.getId()));

        assertTrue(found);
    }

    private static BufferedImage newCanvas() {
        return new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    private static RNProvider sequentialProvider() {
        AtomicInteger value = new AtomicInteger();
        return new TestRNProvider(() -> OptionalInt.of(value.getAndAdd(997) & 0xFFFF));
    }

    private static RNProvider emptyProvider() {
        return new TestRNProvider(OptionalInt::empty);
    }

    private interface NumberSupplier {
        OptionalInt next();
    }

    private static final class TestRNProvider extends RNProvider {
        private final NumberSupplier numberSupplier;

        private TestRNProvider(NumberSupplier numberSupplier) {
            super(testSettings(), false, ignored -> { });
            this.numberSupplier = numberSupplier;
        }

        @Override
        public OptionalInt getNextRandomNumber() {
            return numberSupplier.next();
        }
    }

    private static RNProvider.ProviderSettings testSettings() {
        return new RNProvider.ProviderSettings(
                "http://localhost/test",
                "test-api-key",
                "uint16",
                16,
                2,
                1,
                10,
                10,
                1,
                0,
                0L,
                0L
        );
    }
}