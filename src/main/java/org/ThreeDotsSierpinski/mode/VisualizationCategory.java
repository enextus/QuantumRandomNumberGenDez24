package org.ThreeDotsSierpinski.mode;

/**
 * High-level grouping for visualization modes in the mode selection dialog.
 *
 * The menu uses this enum to present a two-level navigation:
 * category first, concrete visualization mode second.
 */
public enum VisualizationCategory {
    CHAOS_FRACTALS(
            "Chaos & Fractals",
            "Chaos games, IFS fractals, CGR and aggregation structures.",
            "△"
    ),
    MONTE_CARLO(
            "Monte Carlo",
            "Random sampling methods for geometry and numerical estimation.",
            "π"
    ),
    RANDOM_PROCESSES(
            "Random Processes",
            "Walks, distributions, mosaics and sequence diagnostics.",
            "⋯"
    ),
    STATISTICAL_PHYSICS(
            "Statistical Physics",
            "Lattice, percolation and spreading-process simulations.",
            "▦"
    ),
    ATTRACTORS(
            "Attractors",
            "Chaotic attractors and bifurcation dynamics.",
            "∞"
    ),
    LISSAJOUS(
            "Lissajous",
            "Oscilloscope curves, phase systems and QRNG-driven harmonics.",
            "∿"
    );

    private final String displayName;
    private final String description;
    private final String icon;

    VisualizationCategory(String displayName, String description, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }
}
