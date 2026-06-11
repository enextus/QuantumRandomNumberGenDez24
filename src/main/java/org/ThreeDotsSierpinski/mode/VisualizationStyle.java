package org.ThreeDotsSierpinski.mode;

import org.ThreeDotsSierpinski.app.*;
import org.ThreeDotsSierpinski.config.*;
import org.ThreeDotsSierpinski.math.*;
import org.ThreeDotsSierpinski.mode.*;
import org.ThreeDotsSierpinski.model.*;
import org.ThreeDotsSierpinski.rng.*;
import org.ThreeDotsSierpinski.stats.*;

/**
 * Visual skin (Mod) selected by a visualization mode.
 * DEFAULT keeps the current application look. APPLE_MAC is a retro grayscale
 * early-personal-computer skin for the classic Sierpinski UI.
 */
public enum VisualizationStyle {
    DEFAULT("Default"),
    APPLE_MAC("AppleMac");

    private final String displayName;

    VisualizationStyle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}