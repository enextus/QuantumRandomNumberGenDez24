package org.ThreeDotsSierpinski;

class RNLoadListenerImpl implements RNLoadListener {
    private final dotController controller;

    public RNLoadListenerImpl(dotController controller) {
        this.controller = controller;
    }

    @Override
    public void onLoadingStarted() {
        controller.updateStatusLabel("Loading data...");
    }

    @Override
    public void onLoadingCompleted() {
        controller.updateStatusLabel("Data loaded successfully.");
    }

    @Override
    public void onError(String errorMessage) {
        controller.updateStatusLabel("Error: " + errorMessage);
    }

    @Override
    public void onRawDataReceived(String rawData) {
        rawDataTextArea.append(rawData + "\n");
    }

}
