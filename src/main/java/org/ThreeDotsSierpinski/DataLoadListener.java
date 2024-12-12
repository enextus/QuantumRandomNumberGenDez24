package org.ThreeDotsSierpinski;

/**
 * Interface for listening to data loading events from RandomNumberProvider.
 */
public interface DataLoadListener {
    /**
     * Called when data loading starts.
     */
    void onLoadingStarted();

    /**
     * Called when data loading completes successfully.
     */
    void onLoadingCompleted();

    /**
     * Called when an error occurs during data loading.
     *
     * @param errorMessage The error message.
     */
    void onError(String errorMessage);
}
