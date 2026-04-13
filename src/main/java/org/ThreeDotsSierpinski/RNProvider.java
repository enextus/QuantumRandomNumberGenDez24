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
import java.util.random.RandomGenerator;

/**
 * Класс для загрузки случайных чисел из ANU Quantum Numbers API.
 *
 * При недоступности API автоматически переключается на L128X256MixRandom
 * (LXM family, Java 17+, период 2³⁸⁴, проходит TestU01 и PractRand).
 * При восстановлении API переключается обратно на квантовые числа.
 *
 * Особенности:
 * - Неблокирующий getNextRandomNumber() — безопасен для вызова из EDT
 * - Exponential backoff при ошибках API
 * - Graceful degradation: QUANTUM → PSEUDO → QUANTUM
 * - Фоновая предзагрузка при снижении буфера ниже порога
 */
public class RNProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    // ========================================================================
    // Режим работы
    // ========================================================================

    /**
     * Источник случайных чисел.
     */
    public enum Mode {
        /** Квантовые числа от ANU API */
        QUANTUM,
        /** Псевдослучайные числа от L128X256MixRandom (fallback) */
        PSEUDO
    }

    // ========================================================================
    // Настройки экземпляра
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
    // HTTP клиент, fallback PRNG и состояние
    // ========================================================================

    private final HttpClient httpClient;
    private final RandomGenerator fallbackRng;
    private final BlockingQueue<Integer> randomNumbersQueue;
    private final ObjectMapper objectMapper;
    private final RandomNumberProcessor numberProcessor;
    private int apiRequestCount = 0;
    private final List<RNLoadListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean isLoading = false;

    private final List<Long> consumedNumbers = new CopyOnWriteArrayList<>();

    private volatile boolean initialLoadComplete = false;
    private volatile String lastError = null;
    private volatile int consecutiveFailures = 0;
    private volatile Mode currentMode = Mode.QUANTUM;
    private volatile boolean apiKeyConfigured = true;

    /** Сколько pseudo-чисел генерировать за одну «подгрузку» */
    private static final int PSEUDO_BATCH_SIZE = 1024;

    /** После скольких pseudo-batch-ей пытаться переподключиться к API */
    private static final int RECONNECT_EVERY_N_BATCHES = 5;
    private volatile int pseudoBatchCount = 0;

    // ========================================================================
    // Sleeper и ProviderSettings (без изменений)
    // ========================================================================

    @FunctionalInterface
    interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    record ProviderSettings(
            String apiUrl, String apiKey, String dataType,
            int arrayLength, int blockSize, int maxApiRequests,
            int connectTimeout, int readTimeout, int queueMinSize,
            int maxRetries, long initialBackoffMs, long maxBackoffMs
    ) {
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
                    5, 1000L, 30000L
            );
        }
    }

    // ========================================================================
    // Конструкторы
    // ========================================================================

    public RNProvider() {
        this(ProviderSettings.fromConfig(), true, Thread::sleep);
    }

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

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();

        // L128X256MixRandom: LXM family, период 2^384, 4-equidistributed
        // Самый качественный PRNG в стандартной Java (JEP 356)
        this.fallbackRng = RandomGenerator.of("L128X256MixRandom");

        randomNumbersQueue = new LinkedBlockingQueue<>();
        objectMapper = new ObjectMapper();
        numberProcessor = new RandomNumberProcessor();

        // Проверка наличия API ключа
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
            LOGGER.warning("API key is not configured. Falling back to pseudo-random mode (L128X256MixRandom).");
            apiKeyConfigured = false;
            activatePseudoMode("API key not configured");
        } else if (autoLoadOnStart) {
            loadInitialDataAsync();
        }
    }

    // ========================================================================
    // Публичный API
    // ========================================================================

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
        return initialLoadComplete;
    }

    public String getLastError() {
        return lastError;
    }

    public int getQueueSize() {
        return randomNumbersQueue.size();
    }

    /** Текущий режим работы: QUANTUM или PSEUDO */
    public Mode getMode() {
        return currentMode;
    }

    /** Возвращает неизменяемую копию всех потребленных чисел с момента старта */
    public List<Long> getConsumedNumbers() {
        return List.copyOf(consumedNumbers);
    }

    public void addDataLoadListener(RNLoadListener listener) {
        listeners.add(listener);
    }

    /**
     * Возвращает следующее случайное число.
     * НЕБЛОКИРУЮЩИЙ — безопасен для вызова из EDT.
     *
     * В режиме QUANTUM: из буфера (или бросает exception если буфер пуст).
     * В режиме PSEUDO: генерирует на лету, если буфер пуст.
     *
     * @return случайное число (0–65535 для uint16)
     * @throws NoSuchElementException только если QUANTUM и буфер пуст, или лимит запросов
     */
    public int getNextRandomNumber() {
        Integer nextNumber = randomNumbersQueue.poll();

        if (nextNumber == null) {
            if (currentMode == Mode.PSEUDO) {
                // В pseudo-режиме — генерируем на лету
                int pseudoNum = fallbackRng.nextInt(65536);
                consumedNumbers.add((long) pseudoNum); // ИСПРАВЛЕНИЕ: сохраняем число
                return pseudoNum;
            }
            // QUANTUM mode — буфер пуст
            synchronized (this) {
                if (apiRequestCount >= maxApiRequests) {
                    // Лимит исчерпан → переключаемся на pseudo
                    activatePseudoMode("API request limit reached (" + maxApiRequests + ")");
                    int pseudoNum = fallbackRng.nextInt(65536);
                    consumedNumbers.add((long) pseudoNum); // ИСПРАВЛЕНИЕ: сохраняем число
                    return pseudoNum;
                }
            }
            loadInitialDataAsync();
            throw new NoSuchElementException("Buffer empty, background loading started. " +
                    (lastError != null ? lastError : "Waiting for API response."));
        }

        // ИСПРАВЛЕНИЕ: сохраняем число из очереди
        consumedNumbers.add((long) nextNumber);

        // Превентивная подгрузка
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
        LOGGER.info("RNProvider shutting down. Mode: " + currentMode
                + ", API requests: " + apiRequestCount
                + ", pseudo batches: " + pseudoBatchCount);
    }

    // ========================================================================
    // Package-private accessors
    // ========================================================================

    int getApiRequestCount() { return apiRequestCount; }
    boolean isInitialLoadComplete() { return initialLoadComplete; }

    void triggerLoad() { loadInitialDataAsync(); }

    // ========================================================================
    // Pseudo-random fallback
    // ========================================================================

    /**
     * Активирует pseudo-random режим: заполняет буфер и уведомляет listeners.
     */
    private void activatePseudoMode(String reason) {
        if (currentMode == Mode.PSEUDO) return; // Уже в pseudo

        currentMode = Mode.PSEUDO;
        lastError = null; // Сбрасываем ошибку — приложение работоспособно
        LOGGER.info("Switched to PSEUDO mode (L128X256MixRandom). Reason: " + reason);

        fillQueueWithPseudo();
        initialLoadComplete = true;

        notifyModeChanged(Mode.PSEUDO);
        notifyLoadingCompleted();
    }

    /**
     * Заполняет буфер pseudo-random числами.
     */
    private void fillQueueWithPseudo() {
        for (int i = 0; i < PSEUDO_BATCH_SIZE; i++) {
            randomNumbersQueue.add(fallbackRng.nextInt(65536));
        }
        pseudoBatchCount++;
        LOGGER.fine("Filled queue with " + PSEUDO_BATCH_SIZE + " pseudo-random numbers. "
                + "Queue size: " + randomNumbersQueue.size());
    }

    /**
     * Возвращается в QUANTUM режим после успешного ответа API.
     */
    private void switchToQuantumMode() {
        if (currentMode == Mode.QUANTUM) return;

        currentMode = Mode.QUANTUM;
        pseudoBatchCount = 0;
        LOGGER.info("Switched back to QUANTUM mode (ANU API).");
        notifyModeChanged(Mode.QUANTUM);
    }

    // ========================================================================
    // Внутренняя логика загрузки
    // ========================================================================

    private void loadInitialDataAsync() {
        synchronized (this) {
            if (isLoading || apiRequestCount >= maxApiRequests) {
                if (apiRequestCount >= maxApiRequests && currentMode == Mode.QUANTUM) {
                    activatePseudoMode("API request limit reached");
                }
                return;
            }
            isLoading = true;
        }

        CompletableFuture.runAsync(this::loadWithRetry, Thread::startVirtualThread)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Exception during data loading", ex);
                    handleLoadFailure("Exception: " + ex.getMessage());
                    synchronized (this) {
                        isLoading = false;
                    }
                    return null;
                });
    }

    private void loadWithRetry() {
        int retryCount = 0;

        try {
            while (retryCount <= maxRetries) {
                try {
                    loadInitialData();

                    // Успех! Возвращаемся в QUANTUM если были в PSEUDO
                    consecutiveFailures = 0;
                    switchToQuantumMode();
                    checkAndLoadMore();
                    return;

                } catch (Exception e) {
                    retryCount++;
                    consecutiveFailures++;

                    if (retryCount > maxRetries) {
                        LOGGER.severe("All " + maxRetries + " retries failed: " + e.getMessage());
                        handleLoadFailure("API unavailable after " + maxRetries + " retries: " + e.getMessage());
                        return;
                    }

                    long backoffMs = calculateBackoff(retryCount);
                    LOGGER.warning(String.format("API failed (attempt %d/%d). Retry in %d ms. Error: %s",
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
     * Обрабатывает неудачную загрузку: переключается в pseudo mode вместо остановки.
     */
    private void handleLoadFailure(String reason) {
        if (currentMode == Mode.QUANTUM) {
            activatePseudoMode(reason);
        } else {
            // Уже в pseudo — просто дозаполняем буфер если мало
            if (randomNumbersQueue.size() < queueMinSize) {
                fillQueueWithPseudo();
            }
        }
    }

    long calculateBackoff(int retryAttempt) {
        long backoff = initialBackoffMs * (1L << (retryAttempt - 1));
        return Math.min(backoff, maxBackoffMs);
    }

    private String buildRequestUrl() {
        var url = new StringBuilder(apiUrl);
        url.append("?length=").append(Math.min(arrayLength, 1024));
        url.append("&type=").append(dataType);
        if ("hex16".equals(dataType)) {
            url.append("&size=").append(Math.min(blockSize, 1024));
        }
        return url.toString();
    }

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
                    randomNumbersQueue.add(Integer.parseInt(element.asText(), 16));
                } else {
                    randomNumbersQueue.add(element.asInt());
                }
                loadedCount++;
            }

            LOGGER.info("Loaded " + loadedCount + " quantum random numbers. Queue: " + randomNumbersQueue.size());

            synchronized (this) {
                apiRequestCount++;
                initialLoadComplete = true;
                lastError = null;
            }

            notifyRawDataReceived(responseBody);
            notifyLoadingCompleted();

        } else if (rootNode.has("message")) {
            throw new IOException("API Error: " + rootNode.get("message").asText());
        } else {
            throw new IOException("Unexpected response from server.");
        }
    }

    private void checkAndLoadMore() {
        if (randomNumbersQueue.size() < queueMinSize && apiRequestCount < maxApiRequests && !isLoading) {
            loadInitialDataAsync();
        } else if (randomNumbersQueue.size() < queueMinSize && currentMode == Mode.PSEUDO) {
            fillQueueWithPseudo();
        }
    }

    // ========================================================================
    // Notifications
    // ========================================================================

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

    private void notifyModeChanged(Mode mode) {
        listeners.forEach(listener -> listener.onModeChanged(mode));
    }

}
