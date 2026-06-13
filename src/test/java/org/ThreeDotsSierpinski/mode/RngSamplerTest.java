package org.ThreeDotsSierpinski.mode;

import org.ThreeDotsSierpinski.rng.RNProvider;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class RngSamplerTest {

    @Test
    void countsOnlyPresentValues() {
        RngSampler sampler = RngSampler.from(new TestRNProvider(0, 65_535));

        assertEquals(0, sampler.next().orElseThrow());
        assertEquals(65_535, sampler.next().orElseThrow());
        assertTrue(sampler.next().isEmpty());
        assertEquals(2, sampler.used());
    }

    @Test
    void normalizesUInt16Values() {
        assertEquals(0.0, RngSampler.toUnit(0));
        assertEquals(1.0, RngSampler.toUnit(65_535));
        assertEquals(-1.0, RngSampler.toSignedUnit(0));
        assertEquals(1.0, RngSampler.toSignedUnit(65_535));
    }

    private static final class TestRNProvider extends RNProvider {
        private final int[] values;
        private int index;

        private TestRNProvider(int... values) {
            super(new ProviderSettings(
                    "http://localhost",
                    "YOUR_API_KEY_HERE",
                    "uint16",
                    1,
                    1,
                    1,
                    100,
                    100,
                    1,
                    1,
                    1L,
                    1L
            ), false, ms -> {
            });
            this.values = values;
        }

        @Override
        public OptionalInt getNextRandomNumber() {
            if (index >= values.length) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(values[index++]);
        }
    }
}
