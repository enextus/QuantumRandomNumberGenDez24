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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Класс для загрузки случайных чисел из ANU Quantum Numbers API.
 *
 * API: https://api.quantumnumbers.anu.edu.au
 * Документация: https://quantumnumbers.anu.edu.au/documentation
 * Требует API ключ в заголовке x-api-key.
 *
 * Особенности:
 * - Неблокирующий getNextRandomNumber() — безопасен для вызова из EDT (Swing)
 * - Exponential backoff при ошибках API (1s → 2s → 4s → 8s → max 30s)
 * - Автоматическое восстановление после временных сбоев API
 * - Фоновая предзагрузка при снижении буфера ниже порога
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

    // Retry configuration
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long MAX_BACKOFF_MS = 30000;

    private final BlockingQueue<Integer> randomNumbersQueue;
    private final ObjectMapper objectMapper;
    private final RandomNumberProcessor numberProcessor;
    private int apiRequestCount = 0;
    private final List<RNLoadListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean isLoading = false;
    private volatile boolean initialLoadComplete = false;
    private volatile String lastError = null;
    private volatile int consecutiveFailures = 0;

    public RNProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>();
        objectMapper = new ObjectMapper();
        numberProcessor = new RandomNumberProcessor();

        // Проверка наличия API ключа
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.startsWith("YOUR_")) {
            LOGGER.severe("API key is not configured! Set environment variable QRNG_API_KEY, "
                    + "create .env file (see .env.example), or update config.properties");
            lastError = "API key is not configured. See .env.example for setup instructions";
        } else {
            loadInitialDataAsync();
        }
    }

    /**
     * Ожидает загрузки начальных данных.
     * Вызывается из фонового потока в App.java, НЕ из EDT.
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

        CompletableFuture.runAsync(this::loadWithRetry)
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
     * Загрузка данных с exponential backoff при ошибках.
     *
     * При сбое API ждёт 1s → 2s → 4s → 8s → 16s (максимум MAX_RETRIES попыток).
     * При успехе сбрасывает счётчик ошибок и lastError.
     */
    private void loadWithRetry() {
        int retryCount = 0;

        try {
            while (retryCount <= MAX_RETRIES) {
                try {
                    loadInitialData();

                    // Успешная загрузка — сбрасываем счётчик ошибок
                    consecutiveFailures = 0;
                    checkAndLoadMore();
                    return;

                } catch (Exception e) {
                    retryCount++;
                    consecutiveFailures++;

                    if (retryCount > MAX_RETRIES) {
                        LOGGER.severe("All " + MAX_RETRIES + " retry attempts failed. Last error: " + e.getMessage());
                        lastError = "API unavailable after " + MAX_RETRIES + " retries: " + e.getMessage();
                        notifyError(lastError);
                        return;
                    }

                    long backoffMs = calculateBackoff(retryCount);
                    LOGGER.warning(String.format("API request failed (attempt %d/%d). Retrying in %d ms. Error: %s",
                            retryCount, MAX_RETRIES, backoffMs, e.getMessage()));
                    notifyError("Retry " + retryCount + "/" + MAX_RETRIES + ": " + e.getMessage());

                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOGGER.info("Retry interrupted.");
                        return;
                    }
                }
            }
        } finally {
            // Гарантированно сбрасываем флаг — один раз, при любом исходе
            synchronized (this) {
                isLoading = false;
            }
        }
    }

    /**
     * Вычисляет задержку перед следующей попыткой (exponential backoff).
     *
     * @param retryAttempt номер попытки (1, 2, 3, ...)
     * @return задержка в миллисекундах
     */
    long calculateBackoff(int retryAttempt) {
        long backoff = INITIAL_BACKOFF_MS * (1L << (retryAttempt - 1));
        return Math.min(backoff, MAX_BACKOFF_MS);
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

    /**
     * Выполняет один запрос к API.
     * При ошибке выбрасывает исключение (для обработки в loadWithRetry).
     */
    private void loadInitialData() throws Exception {
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
                        lastError = null; // Сбрасываем ошибку при успешной загрузке
                    }
                    notifyRawDataReceived(responseBody);
                    notifyLoadingCompleted();
                } else {
                    throw new IOException("Invalid response format: 'data' is not an array.");
                }
            } else if (rootNode.has("message")) {
                String errorMsg = rootNode.get("message").asText();
                throw new IOException("API Error: " + errorMsg);
            } else {
                throw new IOException("Unexpected response from server.");
            }

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            // НЕ сбрасываем isLoading здесь — это делает loadWithRetry
            // НЕ вызываем checkAndLoadMore — это делает loadWithRetry после успеха
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

    /**
     * Возвращает следующее случайное число из буфера.
     *
     * НЕБЛОКИРУЮЩИЙ — безопасен для вызова из EDT (Swing Timer).
     * Если буфер пуст — выбрасывает NoSuchElementException вместо ожидания.
     * DotController перехватит исключение и пропустит тик анимации.
     *
     * @return случайное число из буфера
     * @throws NoSuchElementException если буфер пуст или достигнут лимит запросов
     */
    public int getNextRandomNumber() {
        Integer nextNumber = randomNumbersQueue.poll(); // Неблокирующий!

        if (nextNumber == null) {
            // Буфер пуст — запускаем фоновую подгрузку
            synchronized (this) {
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    throw new NoSuchElementException("Reached maximum number of API requests.");
                }
            }
            loadInitialDataAsync();
            throw new NoSuchElementException("Buffer empty, background loading started. " +
                    (lastError != null ? lastError : "Waiting for API response."));
        }

        // Превентивная подгрузка при снижении буфера
        if (randomNumbersQueue.size() < QUEUE_MIN_SIZE && apiRequestCount < MAX_API_REQUESTS && !isLoading) {
            loadInitialDataAsync();
        }

        return nextNumber;
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
