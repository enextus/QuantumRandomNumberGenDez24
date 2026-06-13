package org.ThreeDotsSierpinski.app;

import javax.swing.*;
import java.awt.*;

/**
 * Multi-monitor placement helpers used by Swing dialogs/windows.
 */
final class ScreenPlacement {

    private static final int SCREEN_CENTER_DIVISOR = 2;
    private static final int MIN_USABLE_SCREEN_SIZE = 1;
    private static final int NO_INTERSECTION_AREA = 0;
    private static final int MIN_CENTERING_OFFSET = 0;

    private ScreenPlacement() {
    }

    /**
     * Returns the screen where the window is currently located, preferring the
     * largest intersection and falling back to the window center/default screen.
     */
    static GraphicsConfiguration resolveGraphicsConfiguration(Window window) {
        if (window == null) {
            return getDefaultGraphicsConfiguration();
        }

        Rectangle windowBounds = window.getBounds();

        if (!windowBounds.isEmpty()) {
            GraphicsConfiguration largestIntersectionGraphicsConfiguration =
                    findGraphicsConfigurationWithLargestIntersection(windowBounds);
            if (largestIntersectionGraphicsConfiguration != null) {
                return largestIntersectionGraphicsConfiguration;
            }

            Point windowCenter = new Point(
                    windowBounds.x + windowBounds.width / SCREEN_CENTER_DIVISOR,
                    windowBounds.y + windowBounds.height / SCREEN_CENTER_DIVISOR
            );

            GraphicsConfiguration centerGraphicsConfiguration = findGraphicsConfiguration(windowCenter);
            if (centerGraphicsConfiguration != null) {
                return centerGraphicsConfiguration;
            }
        }

        GraphicsConfiguration currentGraphicsConfiguration = window.getGraphicsConfiguration();
        if (currentGraphicsConfiguration != null) {
            return currentGraphicsConfiguration;
        }

        return getDefaultGraphicsConfiguration();
    }

    static void centerDialog(
            JDialog dialog,
            JFrame parent,
            GraphicsConfiguration targetGraphicsConfiguration
    ) {
        if (parent != null) {
            dialog.setLocationRelativeTo(parent);
            return;
        }

        if (targetGraphicsConfiguration == null) {
            dialog.setLocationRelativeTo(null);
            return;
        }

        Rectangle usableBounds = getUsableScreenBounds(targetGraphicsConfiguration);

        int x = usableBounds.x + Math.max(
                MIN_CENTERING_OFFSET,
                (usableBounds.width - dialog.getWidth()) / SCREEN_CENTER_DIVISOR
        );
        int y = usableBounds.y + Math.max(
                MIN_CENTERING_OFFSET,
                (usableBounds.height - dialog.getHeight()) / SCREEN_CENTER_DIVISOR
        );

        dialog.setLocation(x, y);
    }

    private static GraphicsConfiguration findGraphicsConfigurationWithLargestIntersection(Rectangle windowBounds) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsConfiguration bestGraphicsConfiguration = null;
        long bestIntersectionArea = NO_INTERSECTION_AREA;

        for (GraphicsDevice screenDevice : graphicsEnvironment.getScreenDevices()) {
            GraphicsConfiguration graphicsConfiguration = screenDevice.getDefaultConfiguration();
            Rectangle intersection = graphicsConfiguration.getBounds().intersection(windowBounds);

            long intersectionArea = (long) Math.max(NO_INTERSECTION_AREA, intersection.width)
                    * Math.max(NO_INTERSECTION_AREA, intersection.height);

            if (intersectionArea > bestIntersectionArea) {
                bestIntersectionArea = intersectionArea;
                bestGraphicsConfiguration = graphicsConfiguration;
            }
        }

        return bestGraphicsConfiguration;
    }

    private static GraphicsConfiguration findGraphicsConfiguration(Point point) {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (GraphicsDevice screenDevice : graphicsEnvironment.getScreenDevices()) {
            GraphicsConfiguration graphicsConfiguration = screenDevice.getDefaultConfiguration();
            if (graphicsConfiguration.getBounds().contains(point)) {
                return graphicsConfiguration;
            }
        }

        return null;
    }

    private static GraphicsConfiguration getDefaultGraphicsConfiguration() {
        return GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
    }

    private static Rectangle getUsableScreenBounds(GraphicsConfiguration graphicsConfiguration) {
        Rectangle bounds = graphicsConfiguration.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);

        return new Rectangle(
                bounds.x + insets.left,
                bounds.y + insets.top,
                Math.max(MIN_USABLE_SCREEN_SIZE, bounds.width - insets.left - insets.right),
                Math.max(MIN_USABLE_SCREEN_SIZE, bounds.height - insets.top - insets.bottom)
        );
    }
}
