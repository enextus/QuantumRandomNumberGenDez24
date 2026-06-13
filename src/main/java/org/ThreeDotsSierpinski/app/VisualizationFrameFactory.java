package org.ThreeDotsSierpinski.app;

import org.ThreeDotsSierpinski.config.Config;
import org.ThreeDotsSierpinski.mode.VisualizationMode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * Creates and positions the main visualization frame.
 */
final class VisualizationFrameFactory {

    private static final int VISUALIZATION_WINDOW_EXTRA_WIDTH = 100;
    private static final double VISUALIZATION_WINDOW_ASPECT_WIDTH = 4.0;
    private static final double VISUALIZATION_WINDOW_ASPECT_HEIGHT = 3.0;

    private VisualizationFrameFactory() {
    }

    static JFrame createFrame(VisualizationMode mode, GraphicsConfiguration targetGraphicsConfiguration) {
        String windowTitle = "Quantum Visualizer — " + mode.getName();
        JFrame frame = new JFrame(windowTitle, targetGraphicsConfiguration);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        int basePanelWidth = Config.getInt("panel.size.width");
        double scaleWidth = Config.getDouble("window.scale.width");

        int finalWidth = (int) Math.round(basePanelWidth * scaleWidth) + VISUALIZATION_WINDOW_EXTRA_WIDTH;
        int finalHeight = (int) Math.round(
                finalWidth * VISUALIZATION_WINDOW_ASPECT_HEIGHT / VISUALIZATION_WINDOW_ASPECT_WIDTH
        );

        frame.setSize(finalWidth, finalHeight);
        centerWindowOnGraphicsConfiguration(frame, targetGraphicsConfiguration);
        return frame;
    }

    private static void centerWindowOnGraphicsConfiguration(
            Window window,
            GraphicsConfiguration graphicsConfiguration
    ) {
        if (graphicsConfiguration == null) {
            window.setLocationRelativeTo(null);
            return;
        }

        Rectangle usableBounds = getUsableScreenBounds(graphicsConfiguration);

        int x = usableBounds.x + Math.max(0, (usableBounds.width - window.getWidth()) / 2);
        int y = usableBounds.y + Math.max(0, (usableBounds.height - window.getHeight()) / 2);

        window.setLocation(x, y);
    }

    private static Rectangle getUsableScreenBounds(GraphicsConfiguration graphicsConfiguration) {
        return getRectangle(graphicsConfiguration);
    }

    @NotNull
    static Rectangle getRectangle(GraphicsConfiguration graphicsConfiguration) {
        Rectangle bounds = graphicsConfiguration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);

        return new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom
        );
    }
}
