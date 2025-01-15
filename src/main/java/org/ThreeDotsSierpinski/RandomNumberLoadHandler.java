package org.ThreeDotsSierpinski;

import javax.swing.*;

public class RandomNumberLoadHandler implements RandomNumberLoadListener {
    private final JLabel statusLabel;

    public RandomNumberLoadHandler(JLabel statusLabel) {
        this.statusLabel = statusLabel;
    }

    @Override
    public void onLoadingStarted() {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Loading data..."));
    }

    @Override
    public void onLoadingCompleted() {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Data loaded successfully."));
    }

    @Override
    public void onError(String errorMessage) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Error: " + errorMessage));
    }
}
