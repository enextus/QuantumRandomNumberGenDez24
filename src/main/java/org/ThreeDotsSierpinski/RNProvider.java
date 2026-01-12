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
import java.util.stream.Collectors;

/**
 * Класс для загрузки случайных чисел из ANU Quantum Numbers API.
 *
 * Новый API: https://api.quantumnumbers.anu.edu.au
 * Документация: https://quantumnumbers.anu.edu.au/documentation
 *
 * Требует API ключ в заголовке x-api-key.
 */
public class RNProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    // API Configuration
    private static final String API_URL = Config.getString("api.url");
    private static final String API_KEY = Config.getString("api.key");
    private static final String DATA_TYPE = Config.getString("api.data.type");
    private static final int ARRAY_LENGTH = Config.getInt("api.array.length");
    private static final int BLOCK_SIZE = Config.getInt("api.block.size");
    private static final int MAX_API_REQUESTS = Config.getInt("api.max.requests");
    private static final int CONNECT_TIMEOUT = Config.getInt("api.connect.timeout");
    private static final int READ_TIMEOUT = Config.getInt("api.read.timeout");
    private static final int QUEUE_MIN_SIZE = Config.getInt("random.queue.min.size");

    private final BlockingQueue<Integer> randomNumbersQueue;
    private final ObjectMapper objectMapper;
    private final RandomNumberProcessor numberProcessor;
    private int apiRequestCount = 0;
    private final List<RNLoadListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean isLoading = false;
    private volatile boolean initialLoadComplete = false;
    private volatile String lastError = null;

    public RNProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>();
        objectMapper = new ObjectMapper();
        numberProcessor = new RandomNumberProcessor();

        // Проверка наличия API ключа
        if (API_KEY == null || API_KEY.isEmpty()) {
            LOGGER.severe("API key is not configured! Please set api.key in config.properties");
            lastError = "API key is not configured";
        } else {
            loadInitialDataAsync();
        }
    }

    /**
     * Ожидает загрузки начальных данных.
     */
    public boolean waitForInitialData(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (!initialLoadComplete && lastError == null &&
                (System.currentTimeMillis() - start) < timeoutMs) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !randomNumbersQueue.isEmpty();
    }

    public String getLastError() {
        return lastError;
    }

    public boolean hasAvailableNumbers() {
        return !randomNumbersQueue.isEmpty();
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
                    lastError = "Exception during data loading: " + ex.getMessage();
                    notifyError(lastError);
                    synchronized (this) {
                        isLoading = false;
                    }
                    return null;
                });
    }

    /**
     * Строит URL запроса согласно API документации.
     * Формат: https://api.quantumnumbers.anu.edu.au?length=[length]&type=[type]&size=[size]
     */
    private String buildRequestUrl() {
        StringBuilder url = new StringBuilder(API_URL);
        url.append("?length=").append(Math.min(ARRAY_LENGTH, 1024));
        url.append("&type=").append(DATA_TYPE);

        if ("hex16".equals(DATA_TYPE)) {
            url.append("&size=").append(Math.min(BLOCK_SIZE, 1024));
        }

        return url.toString();
    }

    private void loadInitialData() {
        notifyLoadingStarted();
        HttpURLConnection conn = null;

        try {
            String requestUrl = buildRequestUrl();
            LOGGER.info("Sending request: " + requestUrl);

            URI uri = new URI(requestUrl);
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            // ВАЖНО: Добавляем API ключ в заголовок
            conn.setRequestProperty("x-api-key", API_KEY);

            int responseCode = conn.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorBody = getErrorBody(conn);
                LOGGER.severe("HTTP error: " + responseCode + " - " + errorBody);
                throw new IOException("HTTP error code: " + responseCode + " - " + errorBody);
            }

            String responseBody = getResponseBody(conn);
            LOGGER.info("Received response: " + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");

            JsonNode rootNode = objectMapper.readTree(responseBody);

            // API возвращает данные в поле "data"
            if (rootNode.has("data")) {
                JsonNode dataNode = rootNode.get("data");

                if (dataNode.isArray()) {
                    int loadedCount = 0;

                    for (JsonNode element : dataNode) {
                        if ("hex16".equals(DATA_TYPE)) {
                            String hexValue = element.asText();
                            int intValue = Integer.parseInt(hexValue, 16);
                            randomNumbersQueue.add(intValue);
                        } else {
                            randomNumbersQueue.add(element.asInt());
                        }
                        loadedCount++;
                    }

                    LOGGER.info("Loaded " + loadedCount + " random numbers. Queue size: " + randomNumbersQueue.size());
                    synchronized (this) {
                        apiRequestCount++;
                        initialLoadComplete = true;
                    }
                    notifyRawDataReceived(responseBody);
                    notifyLoadingCompleted();
                } else {
                    lastError = "Invalid response format: 'data' is not an array.";
                    LOGGER.warning(lastError);
                    notifyError(lastError);
                }
            } else if (rootNode.has("message")) {
                // Новый API может возвращать ошибки в поле "message"
                String errorMsg = rootNode.get("message").asText();
                lastError = "API Error: " + errorMsg;
                LOGGER.severe(lastError);
                notifyError(lastError);
            } else {
                lastError = "Unexpected response from server.";
                LOGGER.warning("Unexpected response: " + responseBody);
                notifyError(lastError);
            }

        } catch (Exception e) {
            lastError = "Failed to fetch data from QRNG API: " + e.getMessage();
            LOGGER.log(Level.SEVERE, "Failed to fetch data from QRNG API.", e);
            notifyError(lastError);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            synchronized (this) {
                isLoading = false;
            }
            checkAndLoadMore();
        }
    }

    /**
     * Читает тело ответа с использованием try-with-resources.
     */
    private @NotNull String getResponseBody(HttpURLConnection conn) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return in.lines().collect(Collectors.joining());
        }
    }

    /**
     * Читает тело ошибки при неуспешном ответе.
     */
    private String getErrorBody(HttpURLConnection conn) {
        try {
            if (conn.getErrorStream() != null) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    return in.lines().collect(Collectors.joining());
                }
            }
        } catch (Exception e) {
            LOGGER.fine("Could not read error body: " + e.getMessage());
        }
        return "";
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
                nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
                if (nextNumber == null) {
                    throw new NoSuchElementException("No available random numbers. " +
                            (lastError != null ? lastError : "Check API connection."));
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
        LOGGER.info("RNProvider is shutting down. Total API requests: " + apiRequestCount);
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
