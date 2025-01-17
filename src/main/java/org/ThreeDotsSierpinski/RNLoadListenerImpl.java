package org.ThreeDotsSierpinski;

class RNLoadListenerImpl implements RNLoadListener {
    private final DotDisplayController controller;

    public RNLoadListenerImpl(DotDisplayController controller) {
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

}
