package org.ThreeDotsSierpinski.mode;

import org.ThreeDotsSierpinski.rng.RNProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RNG step budget")
class RngStepBudgetTest {

    @Test
    @DisplayName("PSEUDO mode keeps the fast visual budget")
    void pseudoModeKeepsPseudoBudget() {
        assertEquals(1_200, RngStepBudget.forMode(RNProvider.Mode.PSEUDO, 1_200, 32));
    }

    @Test
    @DisplayName("QUANTUM mode caps the visual budget")
    void quantumModeUsesConservativeBudget() {
        assertEquals(32, RngStepBudget.forMode(RNProvider.Mode.QUANTUM, 1_200, 32));
    }

    @Test
    @DisplayName("Forced PSEUDO provider keeps the fast visual budget")
    void forcedPseudoProviderKeepsPseudoBudget() {
        RNProvider provider = new RNProvider(testSettings(), false, _ -> { }, false);

        assertEquals(1_200, RngStepBudget.forProvider(provider, 1_200, 32));
    }

    @Test
    @DisplayName("QUANTUM budget never exceeds the pseudo budget")
    void quantumBudgetDoesNotExceedPseudoBudget() {
        assertEquals(20, RngStepBudget.forMode(RNProvider.Mode.QUANTUM, 20, 100));
    }

    @Test
    @DisplayName("Invalid budgets are clamped to at least one step")
    void invalidBudgetsAreClamped() {
        assertEquals(1, RngStepBudget.forMode(RNProvider.Mode.PSEUDO, 0, 0));
        assertEquals(1, RngStepBudget.forMode(RNProvider.Mode.QUANTUM, 0, 0));
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
