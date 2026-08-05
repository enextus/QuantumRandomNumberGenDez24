package org.ThreeDotsSierpinski.mode.physics;

import org.ThreeDotsSierpinski.rng.RNProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FourierSeriesSpiralMode")
@Tag("fast")
class FourierSeriesSpiralModeTest {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 180;
    private static final int DOT_SIZE = 2;

    @Test
    @DisplayName("Consumes RNG values and draws Fourier spiral samples")
    void consumesRngValuesAndDrawsSpiralSamples() {
        FourierSeriesSpiralMode mode = new FourierSeriesSpiralMode();
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        mode.initialize(canvas, WIDTH, HEIGHT);
        List<Point> points = mode.step(sequentialProvider(), canvas, DOT_SIZE);

        assertFalse(points.isEmpty());
        assertEquals(points.size(), mode.getPointCount());
        assertEquals(points.size(), mode.getRandomNumbersUsed());
        assertTrue(mode.usesDarkBackground());
        assertFalse(mode.usesRecolorAnimation());
        assertFalse(mode.usesRandomNumbersStackOverlay());
        assertDoesNotThrow(() -> mode.redraw(canvas, WIDTH, HEIGHT, DOT_SIZE));
    }

    @Test
    @DisplayName("Handles temporarily empty provider without consuming values")
    void handlesEmptyProviderWithoutConsumingValues() {
        FourierSeriesSpiralMode mode = new FourierSeriesSpiralMode();
        BufferedImage canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        mode.initialize(canvas, WIDTH, HEIGHT);
        assertDoesNotThrow(() -> mode.step(emptyProvider(), canvas, DOT_SIZE));
        assertEquals(0, mode.getPointCount());
        assertEquals(0, mode.getRandomNumbersUsed());
    }

    private static RNProvider sequentialProvider() {
        AtomicInteger value = new AtomicInteger(1);
        return new TestRNProvider(() -> OptionalInt.of(value.getAndAdd(379) & 0xFFFF));
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
            super(testSettings(), false, _ -> { }, false);
            this.numberSupplier = numberSupplier;
        }

        @Override
        public OptionalInt getNextRandomNumber() {
            return numberSupplier.next();
        }

        @Override
        public Mode getMode() {
            return Mode.PSEUDO;
        }

        @Override
        public boolean isForcedPseudo() {
            return true;
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
