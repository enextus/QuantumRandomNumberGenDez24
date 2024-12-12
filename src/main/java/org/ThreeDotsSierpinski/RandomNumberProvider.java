package org.ThreeDotsSierpinski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Class for fetching random numbers from an external API and providing them to the application.
 */
public class RandomNumberProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    // API parameters from configuration
    private static final String API_URL = Config.getString("api.url");
    private static final int MAX_API_REQUESTS = Config.getInt("api.max.requests");
    private static final int CONNECT_TIMEOUT = Config.getInt("api.connect.timeout");
    private static final int READ_TIMEOUT = Config.getInt("api.read.timeout");

    private final BlockingQueue<Integer> randomNumbersQueue; // Queue to store random numbers
    private final ObjectMapper objectMapper; // Object for handling JSON
    private int apiRequestCount = 0; // Counter for API requests
    private final Object lock = new Object(); // Lock for synchronizing loadInitialData calls

    private final ExecutorService executorService; // Thread pool for asynchronous tasks
    private volatile boolean isLoading = false; // Flag indicating if data is being loaded

    private final List<DataLoadListener> listeners = new CopyOnWriteArrayList<>(); // Listeners for data load events

    /**
     * Constructor for RandomNumberProvider.
     * Initializes the random number queue and JSON handler,
     * then loads initial data.
     */
    public RandomNumberProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>(); // Initialize thread-safe queue
        objectMapper = new ObjectMapper(); // Initialize ObjectMapper for JSON handling
        executorService = Executors.newSingleThreadExecutor(); // Initialize single-thread executor
        loadInitialDataAsync(); // Asynchronously load initial data
    }

    /**
     * Adds a listener for data loading events.
     *
     * @param listener The listener to add.
     */
    public void addDataLoadListener(DataLoadListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for data loading events.
     *
     * @param listener The listener to remove.
     */
    public void removeDataLoadListener(DataLoadListener listener) {
        listeners.remove(listener);
    }

    /**
     * Asynchronously loads random numbers from the API.
     * Uses ExecutorService to perform loading in a background thread.
     */
    private void loadInitialDataAsync() {
        synchronized (lock) {
            if (isLoading || apiRequestCount >= MAX_API_REQUESTS) {
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    LOGGER.warning("Maximum number of API requests reached: " + MAX_API_REQUESTS);
                }
                return; // Prevent concurrent loading
            }
            isLoading = true; // Set loading flag
            executorService.submit(this::loadInitialData); // Submit load task to executor
        }
    }

    /**
     * Loads random numbers from the API in a background thread.
     */
    private void loadInitialData() {
        notifyLoadingStarted();
        try {
            int n = 1024; // Number of random bytes to fetch
            String requestUrl = API_URL + "?length=" + n + "&format=HEX"; // Form the request URL

            LOGGER.info("Sending request: " + requestUrl);

            URI uri = new URI(requestUrl); // Create URI from request string
            URL url = uri.toURL(); // Convert URI to URL
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Open connection
            conn.setRequestMethod("GET"); // Set request method
            conn.setConnectTimeout(CONNECT_TIMEOUT); // Set connection timeout
            conn.setReadTimeout(READ_TIMEOUT); // Set read timeout

            // Read response
            String responseBody = getResponseBody(conn);
            LOGGER.info("Received response: " + responseBody);

            // Parse JSON response
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("qrn")) {
                String hexData = rootNode.get("qrn").asText(); // Extract HEX string with numbers
                int length = rootNode.get("length").asInt(); // Extract data length

                // Convert HEX string to byte array
                byte[] byteArray = hexStringToByteArray(hexData);

                // Convert bytes to integers
                for (byte b : byteArray) {
                    int num = b & 0xFF; // Convert unsigned byte to int (0 to 255)
                    try {
                        randomNumbersQueue.put(num); // Add number to queue
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore interrupt status
                        LOGGER.log(Level.WARNING, "Thread interrupted while adding number to queue: " + num, e);
                    }
                }
                LOGGER.info("Loaded " + length + " random numbers.");
                synchronized (lock) {
                    apiRequestCount++; // Increment API request count
                }
                LOGGER.info("API request count: " + apiRequestCount);
                notifyLoadingCompleted();
            } else if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").asText();
                LOGGER.severe("Error fetching random numbers: " + errorMsg);
                notifyError("Error from API: " + errorMsg);
            } else {
                LOGGER.warning("Unexpected response from server.");
                notifyError("Unexpected response from server.");
            }

            conn.disconnect(); // Disconnect
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Invalid URL: " + API_URL, e);
            notifyError("Invalid URL: " + API_URL);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch data from QRNG API.", e);
            notifyError("Failed to fetch data from QRNG API.");
        } finally {
            synchronized (lock) {
                isLoading = false; // Reset loading flag
            }
            // Check if more data needs to be loaded
            if (randomNumbersQueue.size() < 1000 && apiRequestCount < MAX_API_REQUESTS) {
                loadInitialDataAsync();
            }
        }
    }

    /**
     * Helper method to read the response body from the connection.
     *
     * @param conn The HttpURLConnection object.
     * @return The response body as a string.
     * @throws IOException If an I/O error occurs.
     */
    private static @NotNull String getResponseBody(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String responseLine;

        while ((responseLine = in.readLine()) != null) {
            response.append(responseLine.trim()); // Read and append the line to response
        }
        in.close(); // Close input stream

        return response.toString();
    }

    /**
     * Converts a HEX string to a byte array.
     *
     * @param s The HEX string to convert.
     * @return The resulting byte array.
     * @throws IllegalArgumentException If the HEX string is invalid.
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Invalid HEX string length.");
        }
        byte[] data = new byte[len / 2];
        for(int i = 0; i < len; i += 2){
            int high = Character.digit(s.charAt(i),16);
            int low = Character.digit(s.charAt(i+1),16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid character in HEX string.");
            }
            data[i / 2] = (byte) ((high << 4) + low); // Convert two chars to one byte
        }
        return data;
    }

    /**
     * Retrieves the next random number from the queue.
     *
     * @return A random number between 0 and 255.
     * @throws NoSuchElementException If no random numbers are available or API request limit is reached.
     */
    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS); // Try to get a number with 5 sec timeout
            if (nextNumber == null) {
                synchronized (lock) {
                    if (apiRequestCount >= MAX_API_REQUESTS) {
                        throw new NoSuchElementException("Reached maximum number of API requests and no available random numbers.");
                    }
                }
                loadInitialDataAsync(); // Asynchronously load new data
                nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
                if (nextNumber == null) {
                    throw new NoSuchElementException("No available random numbers.");
                }
            }
            // If few numbers left and API request limit not reached
            if (randomNumbersQueue.size() < 1000) {
                loadInitialDataAsync(); // Asynchronously load additional data
            }
            return nextNumber; // Return random number
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new NoSuchElementException("Waiting for random number was interrupted.");
        }
    }

    /**
     * Shuts down the ExecutorService gracefully.
     * Should be called when the application is closing.
     */
    public void shutdown() {
        executorService.shutdown(); // Request shutdown of executor
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Force shutdown if not terminated
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.severe("ExecutorService did not terminate.");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("ExecutorService successfully shut down.");
    }

    /**
     * Generates a random number within the specified range [min, max].
     *
     * @param min The lower bound of the range.
     * @param max The upper bound of the range.
     * @return A random number within [min, max].
     */
    public long getNextRandomNumberInRange(long min, long max) {
        int randomNum = getNextRandomNumber(); // Get number from 0 to 255
        int randomNum2 = getNextRandomNumber(); // Get second random number to increase entropy

        // Combine two random numbers to get a wider range
        int combined = (randomNum << 8) | randomNum2; // Combine two 8-bit numbers into one 16-bit number

        double normalized = combined / (double) 65535; // Normalize to [0.0, 1.0]
        long range = max - min; // Calculate range
        return min + (long) (normalized * range); // Scale number to [min, max]
    }

    /**
     * Notifies all listeners that loading has started.
     */
    private void notifyLoadingStarted() {
        for (DataLoadListener listener : listeners) {
            listener.onLoadingStarted();
        }
    }

    /**
     * Notifies all listeners that loading has completed successfully.
     */
    private void notifyLoadingCompleted() {
        for (DataLoadListener listener : listeners) {
            listener.onLoadingCompleted();
        }
    }

    /**
     * Notifies all listeners that an error occurred during loading.
     *
     * @param errorMessage The error message.
     */
    private void notifyError(String errorMessage) {
        for (DataLoadListener listener : listeners) {
            listener.onError(errorMessage);
        }
    }
}
