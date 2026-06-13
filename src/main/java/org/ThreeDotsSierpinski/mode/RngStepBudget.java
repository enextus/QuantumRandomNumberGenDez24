package org.ThreeDotsSierpinski.mode;

import org.ThreeDotsSierpinski.rng.RNProvider;

/**
 * Small helper for keeping heavy visualizations fast in PSEUDO mode but
 * conservative in QUANTUM mode, where every consumed value may spend API quota.
 */
public final class RngStepBudget {

    private static final int MIN_BUDGET = 1;

    private RngStepBudget() {
    }

    /**
     * Returns {@code pseudoBudget} for local/PSEUDO rendering and a capped
     * {@code quantumBudget} for QUANTUM rendering.
     */
    public static int forProvider(RNProvider provider, int pseudoBudget, int quantumBudget) {
        if (provider == null || provider.isForcedPseudo()) {
            return forMode(RNProvider.Mode.PSEUDO, pseudoBudget, quantumBudget);
        }

        return forMode(provider.getMode(), pseudoBudget, quantumBudget);
    }

    static int forMode(RNProvider.Mode mode, int pseudoBudget, int quantumBudget) {
        int safePseudoBudget = Math.max(MIN_BUDGET, pseudoBudget);
        int safeQuantumBudget = Math.max(MIN_BUDGET, quantumBudget);

        if (mode == RNProvider.Mode.QUANTUM) {
            return Math.min(safePseudoBudget, safeQuantumBudget);
        }

        return safePseudoBudget;
    }
}
