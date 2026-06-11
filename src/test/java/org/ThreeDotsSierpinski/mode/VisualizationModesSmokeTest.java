package org.ThreeDotsSierpinski.mode;

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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Visualization modes smoke tests")
@Tag("fast")
class VisualizationModesSmokeTest {

    private static final int CANVAS_WIDTH = 180;
    private static final int CANVAS_HEIGHT = 160;
    private static final int DOT_SIZE = 2;

    @Test
    @DisplayName("BarnsleyFernMode consumes numbers and draws points")
    void barnsleyFernConsumesNumbersAndDrawsPoints() {
        BarnsleyFernMode mode = new BarnsleyFernMode();
        BufferedImage canvas = newCanvas();
        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);

        List<Point> points = mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertFalse(points.isEmpty());
        assertEquals(points.size(), mode.getPointCount());
        assertEquals(mode.getPointCount(), mode.getRandomNumbersUsed());
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
    }

    @Test
    @DisplayName("RandomWalkHeatmapMode consumes random directions")
    void randomWalkHeatmapConsumesDirections() {
        RandomWalkHeatmapMode mode = new RandomWalkHeatmapMode();
        BufferedImage canvas = newCanvas();
        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);

        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertTrue(mode.getPointCount() > 0);
        assertEquals(mode.getPointCount(), mode.getRandomNumbersUsed());
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
    }


    @Test
    @DisplayName("Rule30AutomatonMode draws CA rows and raw RNG stream")
    void rule30AutomatonDrawsRowsAndRawStream() {
        Rule30AutomatonMode mode = new Rule30AutomatonMode();
        BufferedImage canvas = newCanvas();
        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);

        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertTrue(mode.getPointCount() > 0);
        assertTrue(mode.getRandomNumbersUsed() > 0);
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
        assertFalse(mode.usesRandomNumbersStackOverlay());
    }

    @Test
    @DisplayName("GaltonBoardMode animates active balls and eventually completes balls")
    void galtonBoardEventuallyCompletesBalls() {
        GaltonBoardMode mode = new GaltonBoardMode();
        BufferedImage canvas = newCanvas();
        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);
        RNProvider provider = sequentialProvider();

        for (int i = 0; i < 40; i++) {
            mode.step(provider, canvas, DOT_SIZE);
        }

        assertTrue(mode.getPointCount() > 0);
        assertTrue(mode.getRandomNumbersUsed() > mode.getPointCount());
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
    }


    @Test
    @DisplayName("MonteCarloPiMode consumes pairs and supports reset control")
    void monteCarloPiModeConsumesPairsAndSupportsReset() {
        MonteCarloPiMode mode = new MonteCarloPiMode();
        BufferedImage canvas = newCanvas();
        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);

        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertTrue(mode.getPointCount() > 0);
        assertEquals(mode.getPointCount() * 2, mode.getRandomNumbersUsed());
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());

        CountingDotController controller = new CountingDotController(new MonteCarloPiMode());
        List<JComponent> controls = mode.createModeControls(controller);
        JButton resetButton = controls.stream()
                .filter(JButton.class::isInstance)
                .map(JButton.class::cast)
                .findFirst()
                .orElseThrow();

        resetButton.doClick();
        assertEquals(1, controller.refreshCount());
        controller.shutdown();
    }

    @Test
    @DisplayName("VoronoiMode exposes marker controls and refreshes on toggle")
    void voronoiMarkerToggleRefreshesController() {
        VoronoiMode mode = new VoronoiMode();
        CountingDotController controller = new CountingDotController(new VoronoiMode());

        List<JComponent> controls = mode.createModeControls(controller);

        assertFalse(controls.isEmpty());
        JToggleButton toggle = controls.stream()
                .filter(JToggleButton.class::isInstance)
                .map(JToggleButton.class::cast)
                .findFirst()
                .orElseThrow();

        toggle.doClick();

        assertEquals(1, controller.refreshCount());
        controller.shutdown();
    }

    @Test
    @DisplayName("VoronoiMode step and redraw work with stored seeds")
    void voronoiStepAndRedrawWork() {
        VoronoiMode mode = new VoronoiMode();
        BufferedImage canvas = newCanvas();
        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);

        List<Point> points = mode.step(sequentialProvider(), canvas, DOT_SIZE);
        assertFalse(points.isEmpty());
        assertTrue(mode.getPointCount() > 0);
        assertTrue(mode.getRandomNumbersUsed() > 0);

        assertDoesNotThrow(() -> mode.redraw(canvas, CANVAS_WIDTH, CANVAS_HEIGHT, DOT_SIZE));
    }

    @Test
    @DisplayName("PercolationMode consumes cell decisions and supports redraw")
    void percolationConsumesDecisionsAndRedraws() {
        PercolationMode mode = new PercolationMode();
        BufferedImage canvas = newCanvas();
        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);

        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertTrue(mode.getPointCount() > 0);
        assertEquals(mode.getPointCount(), mode.getRandomNumbersUsed());
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
        assertDoesNotThrow(() -> mode.redraw(canvas, CANVAS_WIDTH, CANVAS_HEIGHT, DOT_SIZE));
    }

    @Test
    @DisplayName("Modes handle temporarily empty provider without crashing")
    void modesHandleEmptyProviderWithoutCrashing() {
        RNProvider emptyProvider = emptyProvider();

        List<VisualizationMode> modes = List.of(
                new BarnsleyFernMode(),
                new RandomWalkHeatmapMode(),
                new Rule30AutomatonMode(),
                new GaltonBoardMode(),
                new MonteCarloPiMode(),
                new MonteCarloMandelbrotAreaMode(),
                new MonteCarloMandelbrot3DAreaMode(),
                new LorenzAttractor3DMode(),
                new PercolationMode()
        );

        for (VisualizationMode mode : modes) {
            BufferedImage canvas = newCanvas();
            mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);
            assertDoesNotThrow(() -> mode.step(emptyProvider, canvas, DOT_SIZE), mode.getName());
            assertEquals(0, mode.getRandomNumbersUsed(), mode.getName());
        }
    }

    @Test
    @DisplayName("LorenzAttractor3DMode advances trajectories and consumes QRNG jitter")
    void lorenzAttractor3DAdvancesTrajectoriesAndConsumesJitter() {
        LorenzAttractor3DMode mode = new LorenzAttractor3DMode();
        BufferedImage canvas = newCanvas();

        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);
        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertTrue(mode.getPointCount() > 0);
        assertTrue(mode.getRandomNumbersUsed() > 0);
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
        assertDoesNotThrow(() -> mode.redraw(canvas, CANVAS_WIDTH, CANVAS_HEIGHT, DOT_SIZE));
    }

    @Test
    @DisplayName("MonteCarloMandelbrotAreaMode consumes random pairs")
    void monteCarloMandelbrotAreaConsumesRandomPairs() {
        MonteCarloMandelbrotAreaMode mode = new MonteCarloMandelbrotAreaMode();
        BufferedImage canvas = newCanvas();

        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);
        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertTrue(mode.getPointCount() > 0);
        assertEquals(mode.getPointCount() * 2, mode.getRandomNumbersUsed());
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
    }

    @Test
    @DisplayName("MonteCarloMandelbrot3DAreaMode consumes random pairs")
    void monteCarloMandelbrot3DAreaConsumesRandomPairs() {
        MonteCarloMandelbrot3DAreaMode mode = new MonteCarloMandelbrot3DAreaMode();
        BufferedImage canvas = newCanvas();

        mode.initialize(canvas, CANVAS_WIDTH, CANVAS_HEIGHT);
        mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertTrue(mode.getPointCount() > 0);
        assertEquals(mode.getPointCount() * 2, mode.getRandomNumbersUsed());
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
    }

    private static BufferedImage newCanvas() {
        return new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
    }

    private static RNProvider sequentialProvider() {
        AtomicInteger value = new AtomicInteger();
        return new TestRNProvider(() -> OptionalInt.of(value.getAndIncrement()));
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
            super(testSettings(), false, _ -> { });
            this.numberSupplier = numberSupplier;
        }

        @Override
        public OptionalInt getNextRandomNumber() {
            return numberSupplier.next();
        }
    }

    private static final class CountingDotController extends DotController {
        private int refreshCount = 0;

        private CountingDotController(VisualizationMode mode) {
            super(emptyProvider(), mode, new JLabel());
        }

        @Override
        public void refreshVisualization() {
            refreshCount++;
        }

        private int refreshCount() {
            return refreshCount;
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