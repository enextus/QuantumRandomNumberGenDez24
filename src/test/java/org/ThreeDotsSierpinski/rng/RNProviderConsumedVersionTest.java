package org.ThreeDotsSierpinski.rng;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RNProviderConsumedVersionTest {

    private static final RNProvider.Sleeper NO_SLEEP = ignored -> {
    };

    @Test
    void consumedVersionIncrementsForEveryConsumedNumber() {
        RNProvider provider = new RNProvider(testSettings(), false, NO_SLEEP, false);
        provider.setForcedPseudo(true);

        assertEquals(0L, provider.getConsumedVersion());
        assertEquals(0, provider.getConsumedCount());

        assertTrue(provider.getNextRandomNumber().isPresent());
        assertEquals(1L, provider.getConsumedVersion());
        assertEquals(1, provider.getConsumedCount());

        assertTrue(provider.getNextRandomNumber().isPresent());
        assertEquals(2L, provider.getConsumedVersion());
        assertEquals(2, provider.getConsumedCount());
    }

    private static RNProvider.ProviderSettings testSettings() {
        return new RNProvider.ProviderSettings(
                "http://localhost:1",
                "test-api-key",
                "uint16",
                5,
                2,
                100,
                100,
                100,
                3,
                1,
                1L,
                1L
        );
    }
}
