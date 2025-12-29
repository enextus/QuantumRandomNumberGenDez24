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
 * Класс для загрузки случайных чисел из внешнего API (ANU QRNG).
 *
 * API Documentation: https://qrng.anu.edu.au/contact/api-documentation/
 *
 * Поддерживаемые типы данных:
 * - uint8: целые числа 0-255
 * - uint16: целые числа 0-65535
 * - hex16: шестнадцатеричные блоки 00-ff
 */
public class RNProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    // API Configuration
    private static final String API_URL = Config.getString("api.url");
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

    /**
     * Строит URL запроса согласно API документации.
     *
     * Формат: https://qrng.anu.edu.au/API/jsonI.php?length=[length]&type=[type]&size=[size]
     *
     * @return Полный URL для запроса
     */
    private String buildRequestUrl() {
        StringBuilder url = new StringBuilder(API_URL);
        url.append("?length=").append(Math.min(ARRAY_LENGTH, 1024)); // Max 1024
        url.append("&type=").append(DATA_TYPE);

        // size параметр нужен только для hex16
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

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            String responseBody = getResponseBody(conn);
            LOGGER.info("Received response: " + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");

            JsonNode rootNode = objectMapper.readTree(responseBody);

            // API возвращает данные в поле "data", а не "qrn"!
            if (rootNode.has("data")) {
                JsonNode dataNode = rootNode.get("data");

                if (dataNode.isArray()) {
                    int loadedCount = 0;

                    for (JsonNode element : dataNode) {
                        if ("hex16".equals(DATA_TYPE)) {
                            // Для hex16 конвертируем строку в число
                            String hexValue = element.asText();
                            int intValue = Integer.parseInt(hexValue, 16);
                            randomNumbersQueue.add(intValue);
                        } else {
                            // Для uint8 и uint16 значения уже числовые
                            randomNumbersQueue.add(element.asInt());
                        }
                        loadedCount++;
                    }

                    LOGGER.info("Loaded " + loadedCount + " random numbers.");
                    synchronized (this) {
                        apiRequestCount++;
                    }
                    notifyRawDataReceived(responseBody);
                    notifyLoadingCompleted();
                } else {
                    LOGGER.warning("'data' field is not an array.");
                    notifyError("Invalid response format: 'data' is not an array.");
                }
            } else if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").asText();
                LOGGER.severe("Error fetching random numbers: " + errorMsg);
                notifyError("Error from API: " + errorMsg);
            } else {
                LOGGER.warning("Unexpected response from server: no 'data' field found.");
                notifyError("Unexpected response from server.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch data from QRNG API.", e);
            notifyError("Failed to fetch data from QRNG API: " + e.getMessage());
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
     * Читает тело ответа из HttpURLConnection с использованием try-with-resources.
     * ИСПРАВЛЕНО: Теперь ресурсы закрываются корректно даже при исключениях.
     */
    private @NotNull String getResponseBody(HttpURLConnection conn) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return in.lines().collect(Collectors.joining());
        }
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
