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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VisualizationMode registry")
@Tag("fast")
class VisualizationModeRegistryTest {

    @Test
    @DisplayName("Registers all current visualization modes")
    void registersAllCurrentVisualizationModes() {
        Set<String> ids = Arrays.stream(VisualizationMode.allModes())
                .map(VisualizationMode::getId)
                .collect(Collectors.toSet());

        assertTrue(ids.contains("Sierpinski"));
        assertTrue(ids.contains("voronoi"));
        assertTrue(ids.contains("barnsley-fern"));
        assertTrue(ids.contains("random-walk-heatmap"));
        assertTrue(ids.contains("rule-30-automaton"));
        assertTrue(ids.contains("galton-board"));
        assertTrue(ids.contains("monte-carlo-pi"));
        assertTrue(ids.contains("percolation"));
        assertTrue(ids.contains("forest-fire"));
        assertTrue(ids.contains("spectral-plot"));
        assertTrue(ids.contains("cgr-bitstream"));
        assertTrue(ids.contains("dla"));
        assertTrue(ids.contains("monte-carlo-mandelbrot-3d-area"));
    }

    @Test
    @DisplayName("Mode ids are unique")
    void modeIdsAreUnique() {
        VisualizationMode[] modes = VisualizationMode.allModes();
        long uniqueIds = Arrays.stream(modes)
                .map(VisualizationMode::getId)
                .distinct()
                .count();

        assertEquals(modes.length, uniqueIds);
    }
}