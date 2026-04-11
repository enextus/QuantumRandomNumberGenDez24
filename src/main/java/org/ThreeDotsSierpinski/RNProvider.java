package org.ThreeDotsSierpinski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * - Использует java.net.http.HttpClient (Java 11+) вместо HttpURLConnection
 */
public class RNProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    // ========================================================================
    // Настройки экземпляра (вместо static final — для тестируемости)
    // ========================================================================

    private final String apiUrl;
    private final String apiKey;
    private final String dataType;
    private final int arrayLength;
    private final int blockSize;
    private final int maxApiRequests;
    private final int connectTimeout;
    private final int readTimeout;
    private final int queueMinSize;
    private final int maxRetries;
    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final Sleeper sleeper;

    // ========================================================================
    // HTTP клиент и состояние экземпляра
    // ========================================================================

    private final HttpClient httpClient;
    private final BlockingQueue<Integer> randomNumbersQueue;
    private final ObjectMapper objectMapper;
    private final RandomNumberProcessor numberProcessor;
    private int apiRequestCount = 0;
    private final List<RNLoadListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean isLoading = false;
    private volatile boolean initialLoadComplete = false;
    private volatile String lastError = null;
    private volatile int consecutiveFailures = 0;

    // ========================================================================
    // Функциональный интерфейс для инъекции паузы (backoff)
    // ========================================================================

    /**
     * Абстракция над Thread.sleep() для тестируемости retry-логики.
     * В продакшене: Thread::sleep. В тестах: no-op или мгновенный sleep.
     */
    @FunctionalInterface
    interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    // ========================================================================
    // Настройки провайдера (record для группировки параметров)
    // ========================================================================

    /**
     * Все параметры конфигурации провайдера в одном месте.
     * Package-private — используется для тестирования.
     */
    record ProviderSettings(
            String apiUrl,
            String apiKey,
            String dataType,
            int arrayLength,
            int blockSize,
            int maxApiRequests,
            int connectTimeout,
            int readTimeout,
            int queueMinSize,
            int maxRetries,
            long initialBackoffMs,
            long maxBackoffMs
    ) {
        /**
         * Создаёт настройки из Config (продакшен-значения).
         */
        static ProviderSettings fromConfig() {
            return new ProviderSettings(
                    Config.getString("api.url"),
                    Config.getString("api.key"),
                    Config.getString("api.data.type"),
                    Config.getInt("api.array.length"),
                    Config.getInt("api.block.size"),
                    Config.getInt("api.max.requests"),
                    Config.getInt("api.connect.timeout"),
                    Config.getInt("api.read.timeout"),
                    Config.getInt("random.queue.min.size"),
                    5,      // MAX_RETRIES
                    1000L,  // INITIAL_BACKOFF_MS
                    30000L  // MAX_BACKOFF_MS
            );
        }
    }

    // ========================================================================
    // Конструкторы
    // ========================================================================

    /**
     * Продакшен-конструктор. Читает настройки из Config и автоматически
     * запускает загрузку данных (если API ключ сконфигурирован).
     */
    public RNProvider() {
        this(ProviderSettings.fromConfig(), true, Thread::sleep);
    }

    /**
     * Тестовый конструктор с полным контролем над параметрами.
     * Package-private — доступен из тестов в том же пакете.
     *
     * @param settings        все параметры конфигурации
     * @param autoLoadOnStart true = сразу начать загрузку; false = ждать ручного вызова
     * @param sleeper         реализация паузы для backoff (Thread::sleep в продакшене)
     */
    RNProvider(ProviderSettings settings, boolean autoLoadOnStart, Sleeper sleeper) {
        this.apiUrl = settings.apiUrl();
        this.apiKey = settings.apiKey();
        this.dataType = settings.dataType();
        this.arrayLength = settings.arrayLength();
        this.blockSize = settings.blockSize();
        this.maxApiRequests = settings.maxApiRequests();
        this.connectTimeout = settings.connectTimeout();
        this.readTimeout = settings.readTimeout();
        this.queueMinSize = settings.queueMinSize();
        this.maxRetries = settings.maxRetries();
        this.initialBackoffMs = settings.initialBackoffMs();
        this.maxBackoffMs = settings.maxBackoffMs();
        this.sleeper = sleeper;

        // HttpClient — потокобезопасный, один на весь lifetime провайдера
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();

        randomNumbersQueue = new LinkedBlockingQueue<>();
        objectMapper = new ObjectMapper();
        numberProcessor = new RandomNumberProcessor();

        // Проверка наличия API ключа
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
            LOGGER.severe("API key is not configured! Set environment variable QRNG_API_KEY, "
                    + "create .env file (see .env.example), or update config.properties");
            lastError = "API key is not configured. See .env.example for setup instructions";
        } else if (autoLoadOnStart) {
            loadInitialDataAsync();
        }
    }

    // ========================================================================
    // Публичный API
    // ========================================================================

    /**
     * Ожидает завершения начальной загрузки.
     * Возвращает true, если начальная загрузка завершилась успешно.
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
        return initialLoadComplete && lastError == null;
    }

    public String getLastError() {
        return lastError;
    }

    public int getQueueSize() {
        return randomNumbersQueue.size();
    }

    public void addDataLoadListener(RNLoadListener listener) {
        listeners.add(listener);
    }

    /**
     * Возвращает следующее случайное число из буфера.
     * НЕБЛОКИРУЮЩИЙ — безопасен для вызова из EDT (Swing Timer).
     * Если буфер пуст — выбрасывает NoSuchElementException вместо ожидания.
     *
     * @return случайное число из буфера
     * @throws NoSuchElementException если буфер пуст или достигнут лимит запросов
     */
    public int getNextRandomNumber() {
        Integer nextNumber = randomNumbersQueue.poll();

        if (nextNumber == null) {
            synchronized (this) {
                if (apiRequestCount >= maxApiRequests) {
                    throw new NoSuchElementException("Reached maximum number of API requests.");
                }
            }
            loadInitialDataAsync();
            throw new NoSuchElementException("Buffer empty, background loading started. " +
                    (lastError != null ? lastError : "Waiting for API response."));
        }

        if (randomNumbersQueue.size() < queueMinSize && apiRequestCount < maxApiRequests && !isLoading) {
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

    // ========================================================================
    // Package-private accessors (для тестов и диагностики)
    // ========================================================================

    int getApiRequestCount() {
        return apiRequestCount;
    }

    boolean isInitialLoadComplete() {
        return initialLoadComplete;
    }

    /**
     * Запускает асинхронную загрузку данных вручную.
     * Package-private — для тестов, когда autoLoadOnStart=false.
     */
    void triggerLoad() {
        loadInitialDataAsync();
    }

    // ========================================================================
    // Внутренняя логика загрузки
    // ========================================================================

    private void loadInitialDataAsync() {
        synchronized (this) {
            if (isLoading || apiRequestCount >= maxApiRequests) {
                if (apiRequestCount >= maxApiRequests) {
                    LOGGER.warning("Maximum number of API requests reached: " + maxApiRequests);
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
     * При сбое API ждёт 1s → 2s → 4s → 8s → 16s (максимум maxRetries попыток).
     * При успехе сбрасывает счётчик ошибок и lastError.
     */
    private void loadWithRetry() {
        int retryCount = 0;

        try {
            while (retryCount <= maxRetries) {
                try {
                    loadInitialData();

                    consecutiveFailures = 0;
                    checkAndLoadMore();
                    return;

                } catch (Exception e) {
                    retryCount++;
                    consecutiveFailures++;

                    if (retryCount > maxRetries) {
                        LOGGER.severe("All " + maxRetries + " retry attempts failed. Last error: " + e.getMessage());
                        lastError = "API unavailable after " + maxRetries + " retries: " + e.getMessage();
                        notifyError(lastError);
                        return;
                    }

                    long backoffMs = calculateBackoff(retryCount);
                    LOGGER.warning(String.format("API request failed (attempt %d/%d). Retrying in %d ms. Error: %s",
                            retryCount, maxRetries, backoffMs, e.getMessage()));
                    notifyError("Retry " + retryCount + "/" + maxRetries + ": " + e.getMessage());

                    try {
                        sleeper.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        LOGGER.info("Retry interrupted.");
                        return;
                    }
                }
            }
        } finally {
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
        long backoff = initialBackoffMs * (1L << (retryAttempt - 1));
        return Math.min(backoff, maxBackoffMs);
    }

    /**
     * Строит URL запроса согласно API документации.
     * Формат: https://api.quantumnumbers.anu.edu.au?length=[length]&type=[type]&size=[size]
     */
    private String buildRequestUrl() {
        var url = new StringBuilder(apiUrl);
        url.append("?length=").append(Math.min(arrayLength, 1024));
        url.append("&type=").append(dataType);

        if ("hex16".equals(dataType)) {
            url.append("&size=").append(Math.min(blockSize, 1024));
        }

        return url.toString();
    }

    /**
     * Выполняет один запрос к API с использованием java.net.http.HttpClient.
     * При ошибке выбрасывает исключение для обработки в loadWithRetry().
     */
    private void loadInitialData() throws Exception {
        notifyLoadingStarted();

        var requestUrl = buildRequestUrl();
        LOGGER.info("Sending request: " + requestUrl);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("x-api-key", apiKey)
                .timeout(Duration.ofMillis(readTimeout))
                .GET()
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode != 200) {
            var errorBody = response.body();
            LOGGER.severe("HTTP error: " + statusCode + " - " + errorBody);
            throw new IOException("HTTP error code: " + statusCode + " - " + errorBody);
        }

        var responseBody = response.body();
        LOGGER.info("Received response: " +
                responseBody.substring(0, Math.min(200, responseBody.length())) + "...");

        var rootNode = objectMapper.readTree(responseBody);

        if (rootNode.has("data")) {
            var dataNode = rootNode.get("data");

            if (!dataNode.isArray()) {
                throw new IOException("Invalid response format: 'data' is not an array.");
            }

            int loadedCount = 0;
            for (JsonNode element : dataNode) {
                if ("hex16".equals(dataType)) {
                    var hexValue = element.asText();
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
                lastError = null;
            }

            notifyRawDataReceived(responseBody);
            notifyLoadingCompleted();

        } else if (rootNode.has("message")) {
            var errorMsg = rootNode.get("message").asText();
            throw new IOException("API Error: " + errorMsg);
        } else {
            throw new IOException("Unexpected response from server.");
        }
    }

    private void checkAndLoadMore() {
        if (randomNumbersQueue.size() < queueMinSize && apiRequestCount < maxApiRequests && !isLoading) {
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