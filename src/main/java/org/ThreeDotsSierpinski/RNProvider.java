package org.ThreeDotsSierpinski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс для загрузки случайных чисел из внешнего API.
 */
public class RNProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();
    private static final String API_URL = Config.getString("api.url");
    private static final int MAX_API_REQUESTS = Config.getInt("api.max.requests");
    private static final int CONNECT_TIMEOUT = Config.getInt("api.connect.timeout");
    private static final int READ_TIMEOUT = Config.getInt("api.read.timeout");
    private static final int LOAD_SIZE = Config.getInt("random.load.size");
    private static final int QUEUE_MIN_SIZE = Config.getInt("random.queue.min.size");

    private final BlockingQueue<Integer> randomNumbersQueue;
    private final ObjectMapper objectMapper;
    private final RandomNumberProcessor numberProcessor;
    private int apiRequestCount = 0;
    private final List<RNLoadListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean isLoading = false;

    public RNProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>();
        objectMapper = new ObjectMapper();
        numberProcessor = new RandomNumberProcessor();
        loadInitialDataAsync();
    }

    public List<Integer> getTrueRandomNumbersFromHex(String hexData) {
        return numberProcessor.processHexToNumbers(hexData);
    }

    public void addDataLoadListener(RNLoadListener listener) {
        listeners.add(listener);
    }

    private void loadInitialDataAsync() {
        synchronized (this) {
            if (isLoading || apiRequestCount >= MAX_API_REQUESTS) {
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    LOGGER.warning("Maximum number of API requests reached: " + MAX_API_REQUESTS);
                }
                return;
            }
            isLoading = true;
        }

        CompletableFuture.runAsync(this::loadInitialData)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Exception during data loading", ex);
                    notifyError("Exception during data loading: " + ex.getMessage());
                    synchronized (this) {
                        isLoading = false;
                    }
                    return null;
                });
    }

    private void loadInitialData() {
        notifyLoadingStarted();
        try {
            String requestUrl = API_URL + "?length=" + LOAD_SIZE + "&format=HEX";
            LOGGER.info("Sending request: " + requestUrl);

            URI uri = new URI(requestUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            String responseBody = getResponseBody(conn);
            LOGGER.info("Received response: " + responseBody);

            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("qrn")) {
                String hexData = rootNode.get("qrn").asText();
                List<Integer> randomNumbers = numberProcessor.processHexToNumbers(hexData);
                randomNumbersQueue.addAll(randomNumbers);
                LOGGER.info("Loaded " + randomNumbers.size() + " random numbers.");
                synchronized (this) {
                    apiRequestCount++;
                }
                notifyRawDataReceived(hexData);
                notifyLoadingCompleted();
            } else if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").asText();
                LOGGER.severe("Error fetching random numbers: " + errorMsg);
                notifyError("Error from API: " + errorMsg);
            } else {
                LOGGER.warning("Unexpected response from server.");
                notifyError("Unexpected response from server.");
            }

            conn.disconnect();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch data from QRNG API.", e);
            notifyError("Failed to fetch data from QRNG API: " + e.getMessage());
        } finally {
            synchronized (this) {
                isLoading = false;
            }
            checkAndLoadMore();
        }
    }

    private @NotNull String getResponseBody(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String responseLine;
        while ((responseLine = in.readLine()) != null) {
            response.append(responseLine.trim());
        }
        in.close();
        return response.toString();
    }

    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(1, TimeUnit.SECONDS);
            if (nextNumber == null) {
                synchronized (this) {
                    if (apiRequestCount >= MAX_API_REQUESTS) {
                        throw new NoSuchElementException("Reached maximum number of API requests.");
                    }
                }
                loadInitialDataAsync();
                nextNumber = randomNumbersQueue.poll(1, TimeUnit.SECONDS);
                if (nextNumber == null) {
                    throw new NoSuchElementException("No available random numbers.");
                }
            }
            if (randomNumbersQueue.size() < QUEUE_MIN_SIZE && apiRequestCount < MAX_API_REQUESTS && !isLoading) {
                loadInitialDataAsync();
            }
            return nextNumber;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Interrupted while waiting for random number.");
        }
    }

    public long getNextRandomNumberInRange(long min, long max) {
        int randomNum = getNextRandomNumber();
        return numberProcessor.generateNumberInRange(randomNum, min, max);
    }

    public void shutdown() {
        LOGGER.info("RNProvider is shutting down.");
    }

    private void checkAndLoadMore() {
        if (randomNumbersQueue.size() < QUEUE_MIN_SIZE && apiRequestCount < MAX_API_REQUESTS && !isLoading) {
            loadInitialDataAsync();
        }
    }

    private void notifyLoadingStarted() {
        listeners.forEach(RNLoadListener::onLoadingStarted);
    }

    private void notifyLoadingCompleted() {
        listeners.forEach(RNLoadListener::onLoadingCompleted);
    }

    private void notifyError(String errorMessage) {
        listeners.forEach(listener -> listener.onError(errorMessage));
    }

    private void notifyRawDataReceived(String rawData) {
        listeners.forEach(listener -> listener.onRawDataReceived(rawData));
    }

}
