package org.ThreeDotsSierpinski.mode;

import org.ThreeDotsSierpinski.rng.RNProvider;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Small helper for mode-local RNG sampling.
 *
 * <p>It centralizes uint16 normalization and counts how many random values were
 * actually consumed through this sampler instance.</p>
 */
public final class RngSampler {

    public static final int UINT16_MAX = 65_535;
    private static final double UINT16_MAX_DOUBLE = UINT16_MAX;

    private final RNProvider provider;
    private int used;

    private RngSampler(RNProvider provider) {
        this.provider = provider;
    }

    public static RngSampler from(RNProvider provider) {
        return new RngSampler(provider);
    }

    public OptionalInt next() {
        if (provider == null) {
            return OptionalInt.empty();
        }

        OptionalInt value = provider.getNextRandomNumber();
        if (value.isPresent()) {
            used++;
        }
        return value;
    }

    public OptionalDouble nextUnit() {
        OptionalInt value = next();
        if (value.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(toUnit(value.getAsInt()));
    }

    public OptionalDouble nextSignedUnit() {
        OptionalInt value = next();
        if (value.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(toSignedUnit(value.getAsInt()));
    }

    public int used() {
        return used;
    }

    public static double toUnit(int randomValue) {
        return Math.clamp(randomValue / UINT16_MAX_DOUBLE, 0.0, 1.0);
    }

    public static double toSignedUnit(int randomValue) {
        return toUnit(randomValue) * 2.0 - 1.0;
    }
}
