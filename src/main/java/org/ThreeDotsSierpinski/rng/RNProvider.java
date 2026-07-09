package org.ThreeDotsSierpinski.rng;

import org.ThreeDotsSierpinski.config.Config;
import org.ThreeDotsSierpinski.config.LoggerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

/**
 * Класс для загрузки случайных чисел из ANU Quantum Numbers API.
 * При недоступности API автоматически переключается на L128X256MixRandom
 * (LXM family, Java 17+, период 2³⁸⁴, проходит TestU01 и PractRand).
 * При восстановлении API переключается обратно на квантовые числа.
 * Особенности:
 * - Неблокирующий getNextRandomNumber() — безопасен для вызова из EDT
 * - Exponential backoff при ошибках API
 * - Graceful degradation: QUANTUM → PSEUDO → QUANTUM
 * - Фоновая предзагрузка при снижении буфера ниже порога
 * - Кольцевой буфер (Ring Buffer) для истории потребленных чисел (фиксированный расход памяти)
 */
public class RNProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();
    /**
     * Максимальный размер истории потребленных чисел (~0.8 МБ памяти)
     */
    private static final int HISTORY_MAX_SIZE = 100_000;
    /**
     * Сколько pseudo-чисел генерировать за одну «подгрузку»
     */
    private static final int PSEUDO_BATCH_SIZE = 1024;

    /**
     * Polling interval used by waitForInitialData(). Kept as a named constant so
     * the old timing behavior remains explicit and easy to adjust later.
     */
    private static final long INITIAL_DATA_POLL_INTERVAL_MS = 100L;
    /**
     * Background reconnect interval after temporary network/API failures.
     */
    private static final long RECONNECT_RETRY_INTERVAL_MS = 15_000L;

    // ========================================================================
    // Настройки экземпляра
    // ========================================================================
    private final String apiKey;
    private final int maxApiRequests;
    private final int queueMinSize;
    private final int maxRetries;
    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final Sleeper sleeper;

    // ========================================================================
    // HTTP клиент, fallback PRNG и состояние
    // ========================================================================

    private final QuantumNumbersApiClient quantumNumbersApiClient;
    private final RandomGenerator fallbackRng;
    private final Object fallbackRngLock = new Object();
    private final BlockingQueue<RandomNumberEntry> randomNumbersQueue;
    private final RandomNumberProcessor numberProcessor;
    private final RandomNumbersLog randomNumbersLog;
    private final List<RNLoadListener> listeners = new CopyOnWriteArrayList<>();
    /**
     * Сам массив-буфер
     */
    private final Object consumedNumbersLock = new Object();

    // ========================================================================
    // RING BUFFER ДЛЯ ИСТОРИИ (Вместо List<Long>)
    // ========================================================================
    private final long[] consumedNumbersRing = new long[HISTORY_MAX_SIZE];
    private int apiRequestCount = 0;
    /**
     * Указатель, куда писать следующее число.
     * Доступ только под consumedNumbersLock.
     */
    private int ringWriteIndex = 0;
    /**
     * Количество реально заполненных значений в history-buffer.
     * Доступ только под consumedNumbersLock.
     */
    private int totalConsumed = 0;
    private volatile boolean isLoading = false;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private volatile boolean shutdownRequested = false;
    private volatile boolean initialLoadComplete = false;
    private volatile String lastError = null;
    private volatile String fallbackReason = null;
    private volatile FallbackReason fallbackReasonCode = null;
    private volatile int consecutiveFailures = 0;
    private volatile Mode currentMode = Mode.QUANTUM;
    private volatile boolean isForcedPseudo = true; // По умолчанию всегда стартуем локально
    private volatile boolean apiKeyConfigured = true;
    private volatile int pseudoBatchCount = 0;

    public RNProvider() {
        this(ProviderSettings.fromConfig(), true, Thread::sleep);
    }

    public RNProvider(ProviderSettings settings, boolean autoLoadOnStart, Sleeper sleeper) {
        this(settings, autoLoadOnStart, sleeper, true);
    }

    public RNProvider(ProviderSettings settings, boolean autoLoadOnStart, Sleeper sleeper, boolean randomNumberLoggingEnabled) {
        this.apiKey = settings.apiKey();
        this.maxApiRequests = settings.maxApiRequests();
        this.queueMinSize = settings.queueMinSize();
        this.maxRetries = settings.maxRetries();
        this.initialBackoffMs = settings.initialBackoffMs();
        this.maxBackoffMs = settings.maxBackoffMs();
        this.sleeper = sleeper;

        this.quantumNumbersApiClient = new QuantumNumbersApiClient(settings);

        // L128X256MixRandom: LXM family, период 2^384, 4-equidistributed
        // Самый качественный PRNG в стандартной Java (JEP 356)
        this.fallbackRng = RandomGenerator.of("L128X256MixRandom");

        randomNumbersQueue = new LinkedBlockingQueue<>();
        numberProcessor = new RandomNumberProcessor();
        randomNumbersLog = randomNumberLoggingEnabled ? new RandomNumbersLog() : RandomNumbersLog.disabled();

        // Проверка наличия API ключа
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
            LOGGER.warning("API key is not configured. Falling back to pseudo-random mode (L128X256MixRandom).");
            apiKeyConfigured = false;
            activatePseudoMode(FallbackReason.NO_API_KEY, "no API key");
        } else if (autoLoadOnStart) {
            // Ключ есть!
            if (isForcedPseudo) {
                activatePseudoMode(FallbackReason.DEFAULT_LOCAL, "Default local mode"); // Стартуем визуально как PSEUDO
                loadInitialDataAsync(); // Запускаем фоновую загрузку
            } else {
                loadInitialDataAsync();
            }
        }
    }

    public boolean isForcedPseudo() {
        return isForcedPseudo;
    }

    /**
     * Принудительно переключает в локальный режим (без запросов к API).
     */
    public void setForcedPseudo(boolean forced) {
        if (shutdownRequested) {
            return;
        }

        this.isForcedPseudo = forced;
        if (forced) {
            currentMode = Mode.PSEUDO;
            fallbackReasonCode = FallbackReason.MANUAL;
            fallbackReason = "Manually forced to PSEUDO";
            reconnecting.set(false);
            notifyModeChanged(Mode.PSEUDO);
        } else {
            // Юзер кликнул ВПРАВО (QUANTUM)
            if (!apiKeyConfigured) {
                // Нет ключа — не даём переключиться
                notifyModeChanged(Mode.PSEUDO); // Сигнализируем, что остались в PSEUDO
                return;
            }

            fallbackReasonCode = null;
            fallbackReason = null;
            currentMode = Mode.QUANTUM;
            reconnecting.set(false);
            loadInitialDataAsync(); // Попытка подключиться
        }
    }

    public boolean waitForInitialData(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (!initialLoadComplete && lastError == null &&
                (System.currentTimeMillis() - start) < timeoutMs) {
            try {
                Thread.sleep(INITIAL_DATA_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return initialLoadComplete;
    }

    // ========================================================================
    // Sleeper и ProviderSettings
    // ========================================================================

    /**
     * Проверяет, был ли изначально сконфигурирован API ключ
     */
    public boolean isApiKeyConfigured() {
        return apiKeyConfigured;
    }

    public String getLastError() {
        return lastError;
    }

    // ========================================================================
    // Конструкторы
    // ========================================================================

    public int getQueueSize() {
        return randomNumbersQueue.size();
    }

    /**
     * Текущий режим работы: QUANTUM или PSEUDO
     */
    public Mode getMode() {
        return currentMode;
    }

    // ========================================================================
    // Публичный API
    // ========================================================================

    /**
     * Возвращает причину последнего переключения в PSEUDO режим
     */
    public String getFallbackReason() {
        return fallbackReason;
    }

    public FallbackReason getFallbackReasonCode() {
        return fallbackReasonCode;
    }

    public String getFallbackReasonDisplayText() {
        return fallbackReasonCode == null ? fallbackReason : fallbackReasonCode.displayText();
    }

    public int getConsumedCount() {
        synchronized (consumedNumbersLock) {
            return totalConsumed;
        }
    }

    /**
     * Возвращает неизменяемую копию всех доступных потребленных чисел (до HISTORY_MAX_SIZE).
     */
    public List<Long> getConsumedNumbers() {
        return getLastConsumedNumbers(HISTORY_MAX_SIZE);
    }

    /**
     * Возвращает последние N потребленных чисел (для UI без лагов).
     */
    public List<Long> getLastConsumedNumbers(int limit) {
        long[] result;

        synchronized (consumedNumbersLock) {
            int actualSize = Math.min(limit, Math.min(totalConsumed, HISTORY_MAX_SIZE));
            if (actualSize <= 0) {
                return List.of();
            }

            result = new long[actualSize];

            // Читаем буфер в обратном порядке: от самых новых к старым.
            int currentIdx = ringWriteIndex == 0 ? HISTORY_MAX_SIZE - 1 : ringWriteIndex - 1;

            for (int i = 0; i < actualSize; i++) {
                result[i] = consumedNumbersRing[currentIdx];
                currentIdx = currentIdx == 0 ? HISTORY_MAX_SIZE - 1 : currentIdx - 1;
            }
        }

        // Разворачиваем массив, чтобы индекс 0 был самым старым из выборки.
        for (int i = 0; i < result.length / 2; i++) {
            long temp = result[i];
            result[i] = result[result.length - 1 - i];
            result[result.length - 1 - i] = temp;
        }

        // Конвертируем в List<Long> для совместимости с остальным кодом.
        List<Long> list = new ArrayList<>(result.length);
        for (long num : result) {
            list.add(num);
        }
        return list;
    }

    public void addDataLoadListener(RNLoadListener listener) {
        listeners.add(listener);
    }

    /**
     * Возвращает следующее случайное число.
     * НЕБЛОКИРУЮЩИЙ - безопасен для вызова из EDT.
     *
     * @return OptionalInt: число готово, или Empty (если QUANTUM буфер пуст и идет загрузка).
     */
    public OptionalInt getNextRandomNumber() {
        // Manual/local PSEUDO mode must return fallback numbers directly.
        // After the background API load succeeds, currentMode becomes QUANTUM;
        // then we must consume queued QUANTUM entries instead of continuing
        // to generate local pseudo numbers just because the app started in
        // the default-local boot mode.
        if (isForcedPseudo && currentMode == Mode.PSEUDO) {
            int pseudoNum = nextPseudoUInt16();
            addConsumedNumber(pseudoNum, Mode.PSEUDO);
            return OptionalInt.of(pseudoNum);
        }

        RandomNumberEntry nextEntry = randomNumbersQueue.poll();
        if (nextEntry == null) {
            if (currentMode == Mode.PSEUDO) {
                fillQueueWithPseudo();
                int pseudoNum = nextPseudoUInt16();
                addConsumedNumber(pseudoNum, Mode.PSEUDO);
                return OptionalInt.of(pseudoNum);
            }

            if (isApiRequestLimitReached()) {
                activatePseudoMode(FallbackReason.RATE_LIMIT, "API request limit reached (" + maxApiRequests + ")");
                int pseudoNum = nextPseudoUInt16();
                addConsumedNumber(pseudoNum, Mode.PSEUDO);
                return OptionalInt.of(pseudoNum);
            }

            loadInitialDataAsync();
            return OptionalInt.empty();
        }

        addConsumedNumber(nextEntry.value(), nextEntry.sourceMode());

        if (randomNumbersQueue.size() < queueMinSize && apiRequestCount < maxApiRequests && !isLoading) {
            loadInitialDataAsync();
        }

        return OptionalInt.of(nextEntry.value());
    }

    /**
     * Фоновая задача: периодически пытается достучаться до API.
     * Если успешно — переключает обратно в QUANTUM и разблокирует UI.
     */
    private void startReconnectMonitor() {
        if (shutdownRequested || !reconnecting.compareAndSet(false, true)) {
            return;
        }

        Thread.startVirtualThread(() -> {
            try {
                while (!shutdownRequested && reconnecting.get() && currentMode == Mode.PSEUDO) {
                    try {
                        Thread.sleep(RECONNECT_RETRY_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    if (shutdownRequested || !reconnecting.get() || currentMode != Mode.PSEUDO) {
                        break;
                    }

                    if (!tryBeginApiLoad()) {
                        if (isApiRequestLimitReached()) {
                            break;
                        }
                        LOGGER.fine("Skipping reconnect attempt because another API load is already running.");
                        continue;
                    }

                    LOGGER.info("Background reconnect attempt...");
                    try {
                        loadInitialData();

                        if (shutdownRequested) {
                            break;
                        }

                        consecutiveFailures = 0;
                        switchToQuantumMode();
                        notifyApiAvailability(true);
                        break;
                    } catch (RateLimitException e) {
                        break;
                    } catch (Exception e) {
                        LOGGER.fine("Reconnect failed, will retry in "
                                + RECONNECT_RETRY_INTERVAL_MS
                                + " ms: "
                                + e.getMessage());
                    } finally {
                        finishApiLoad();
                    }
                }
            } finally {
                reconnecting.set(false);
            }
        });
    }

    public OptionalLong tryGetNextRandomNumberInRange(long min, long max) {
        OptionalInt randomNum = getNextRandomNumber();
        if (randomNum.isEmpty()) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(numberProcessor.generateNumberInRange(randomNum.getAsInt(), min, max));
    }

    public long getNextRandomNumberInRange(long min, long max) {
        return tryGetNextRandomNumberInRange(min, max)
                .orElseThrow(() -> new IllegalStateException("No random number available."));
    }

    public void shutdown() {
        shutdownRequested = true;
        reconnecting.set(false);
        synchronized (this) {
            isLoading = false;
        }
        randomNumbersLog.finishBatch();
        randomNumbersLog.close();
        LOGGER.info("RNProvider shutting down. Mode: " + currentMode
                + ", API requests: " + apiRequestCount
                + ", pseudo batches: " + pseudoBatchCount);
    }

    int getApiRequestCount() {
        return apiRequestCount;
    }

    boolean isInitialLoadComplete() {
        return initialLoadComplete;
    }

    void triggerLoad() {
        loadInitialDataAsync();
    }

    private boolean isApiRequestLimitReached() {
        synchronized (this) {
            return apiRequestCount >= maxApiRequests;
        }
    }

    private boolean tryBeginApiLoad() {
        synchronized (this) {
            if (shutdownRequested || isLoading || apiRequestCount >= maxApiRequests) {
                return false;
            }

            isLoading = true;
            return true;
        }
    }

    private void finishApiLoad() {
        synchronized (this) {
            isLoading = false;
        }
    }

    /**
     * Добавляет число в кольцевой буфер.
     * Потокобезопасно: запись значения, сдвиг индекса и обновление размера
     * выполняются под одним lock.
     */
    private void addConsumedNumber(long value, Mode sourceMode) {
        synchronized (consumedNumbersLock) {
            consumedNumbersRing[ringWriteIndex] = value;
            ringWriteIndex = (ringWriteIndex + 1) % HISTORY_MAX_SIZE;

            if (totalConsumed < HISTORY_MAX_SIZE) {
                totalConsumed++;
            }
        }

        if (shutdownRequested) {
            return;
        }

        if (sourceMode == Mode.QUANTUM) {
            randomNumbersLog.writeTrueNumber(value);
        } else {
            randomNumbersLog.writePseudoNumber(value);
        }
    }

    private void activatePseudoMode(FallbackReason reasonCode, String reason) {
        if (shutdownRequested) {
            return;
        }

        // Всегда обновляем причину, даже если уже в PSEUDO
        this.fallbackReasonCode = reasonCode;
        this.fallbackReason = reason;

        if (currentMode == Mode.PSEUDO) {
            // Уже в PSEUDO — просто обновляем причину и дозаполняем буфер если нужно
            LOGGER.info("Updated PSEUDO fallback reason: " + reason);
            if (randomNumbersQueue.size() < queueMinSize) {
                fillQueueWithPseudo();
            }
            return;
        }

        currentMode = Mode.PSEUDO;
        lastError = null;
        LOGGER.info("Switched to PSEUDO mode (L128X256MixRandom). Reason: " + reason);

        fillQueueWithPseudo();
        initialLoadComplete = true;

        notifyModeChanged(Mode.PSEUDO);
        notifyLoadingCompleted();
    }

    private int nextPseudoUInt16() {
        synchronized (fallbackRngLock) {
            return fallbackRng.nextInt(65_536);
        }
    }

    private void fillQueueWithPseudo() {
        if (shutdownRequested) {
            return;
        }

        for (int i = 0; i < PSEUDO_BATCH_SIZE; i++) {
            randomNumbersQueue.add(new RandomNumberEntry(nextPseudoUInt16(), Mode.PSEUDO));
        }
        pseudoBatchCount++;
        LOGGER.fine("Filled queue with " + PSEUDO_BATCH_SIZE + " pseudo-random numbers. "
                + "Queue size: " + randomNumbersQueue.size());
    }

    /**
     * Removes stale fallback values from the queue before adding fresh API data.
     * <p>
     * The application starts in a local PSEUDO mode while the API request runs
     * in the background. When the API succeeds, the queue may still contain
     * fallback numbers. Keeping them would make the UI show QUANTUM while the
     * first consumed values are still pseudo-random and therefore not written
     * to rnds-true.log.
     */
    private void removePseudoEntriesFromQueue() {
        int queueSizeBefore = randomNumbersQueue.size();
        boolean removedAny = randomNumbersQueue.removeIf(entry -> entry.sourceMode() == Mode.PSEUDO);

        if (removedAny) {
            int removedCount = Math.max(0, queueSizeBefore - randomNumbersQueue.size());
            LOGGER.info("Removed " + removedCount + " stale PSEUDO numbers before enqueuing QUANTUM data.");
        }
    }

    private void switchToQuantumMode() {
        if (shutdownRequested) {
            return;
        }

        // Кнопка активна по умолчанию (если есть ключ), замораживается только при handleLoadFailure.
        // Successful API loading ends the default-local startup phase.
        // Without this, getNextRandomNumber() would keep generating PSEUDO
        // values even while the UI already shows QUANTUM.
        isForcedPseudo = false;

        if (currentMode == Mode.QUANTUM) return;

        currentMode = Mode.QUANTUM;
        fallbackReasonCode = null;
        fallbackReason = null;
        pseudoBatchCount = 0;
        LOGGER.info("Switched back to QUANTUM mode (ANU API).");
        notifyModeChanged(Mode.QUANTUM);
    }

    // ========================================================================
    // Внутренняя логика Ring Buffer
    // ========================================================================

    private void loadInitialDataAsync() {
        if (shutdownRequested) {
            return;
        }

        boolean activateRateLimitFallback = false;
        boolean fillPseudoQueue = false;
        boolean startApiLoad = false;

        synchronized (this) {
            if (shutdownRequested || isLoading) {
                return;
            }

            if (apiRequestCount >= maxApiRequests) {
                activateRateLimitFallback = currentMode == Mode.QUANTUM;
            } else if (currentMode == Mode.PSEUDO && !isForcedPseudo) {
                fillPseudoQueue = true;
            } else {
                isLoading = true;
                startApiLoad = true;
            }
        }

        if (activateRateLimitFallback) {
            activatePseudoMode(FallbackReason.RATE_LIMIT, "API request limit reached");
            return;
        }

        if (fillPseudoQueue) {
            fillQueueWithPseudo();
            return;
        }

        if (!startApiLoad) {
            return;
        }

        CompletableFuture.runAsync(this::loadWithRetry, Thread::startVirtualThread)
                .exceptionally(ex -> {
                    if (!shutdownRequested) {
                        LOGGER.log(Level.SEVERE, "Exception during data loading", ex);
                        handleLoadFailure("Exception: " + ex.getMessage());
                    }
                    finishApiLoad();
                    return null;
                });
    }

    // ========================================================================
    // Pseudo-random fallback
    // ========================================================================

    private void loadWithRetry() {
        int retryCount = 0;

        try {
            while (!shutdownRequested && retryCount <= maxRetries) {
                try {
                    loadInitialData();

                    if (shutdownRequested) {
                        return;
                    }

                    consecutiveFailures = 0;
                    switchToQuantumMode();
                    checkAndLoadMore();
                    return;

                } catch (RateLimitException e) {
                    LOGGER.info("Rate limit (429) detected. Bypassing retries, activating fallback.");
                    handleLoadFailure("Суточный лимит исчерпан, переключаю на псевдослучайные числа.");
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

                    if (shutdownRequested) {
                        return;
                    }

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
            finishApiLoad();
        }
    }

    private void handleLoadFailure(String reason) {
        if (shutdownRequested) {
            return;
        }

        FallbackReason reasonCode = classifyFallbackReason(reason);
        if (reasonCode == FallbackReason.RATE_LIMIT) {
            LOGGER.info("Rate limit active. Reconnect monitor disabled until manual retry.");
        }

        activatePseudoMode(reasonCode, reason);

        if (apiKeyConfigured && reasonCode.allowsReconnect()) {
            startReconnectMonitor();
        }
    }

    private static FallbackReason classifyFallbackReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return FallbackReason.API_ERROR;
        }

        String normalized = reason.toLowerCase(java.util.Locale.ROOT);
        if (reason.contains("429") || normalized.contains("limit") || normalized.contains("лимит")) {
            return FallbackReason.RATE_LIMIT;
        }
        if (normalized.contains("connection refused")
                || normalized.contains("timed out")
                || normalized.contains("unknownhost")
                || normalized.contains("unavailable")) {
            return FallbackReason.NETWORK_DOWN;
        }
        return FallbackReason.API_ERROR;
    }

    long calculateBackoff(int retryAttempt) {
        long backoff = initialBackoffMs * (1L << (retryAttempt - 1));
        return Math.min(backoff, maxBackoffMs);
    }

    // ========================================================================
    // Внутренняя логика загрузки
    // ========================================================================

    private void loadInitialData() throws Exception {
        if (shutdownRequested) {
            return;
        }

        notifyLoadingStarted();

        QuantumNumbersApiResponse response = quantumNumbersApiClient.fetchNumbers();
        if (shutdownRequested) {
            return;
        }

        removePseudoEntriesFromQueue();

        int loadedCount = 0;
        for (int number : response.numbers()) {
            if (shutdownRequested) {
                return;
            }
            randomNumbersQueue.add(new RandomNumberEntry(number, Mode.QUANTUM));
            loadedCount++;
        }

        LOGGER.info("Loaded " + loadedCount + " quantum random numbers. Queue: " + randomNumbersQueue.size());

        synchronized (this) {
            apiRequestCount++;
            initialLoadComplete = true;
            lastError = null;
        }

        notifyRawDataReceived(response.rawData());
        notifyLoadingCompleted();
    }

    private void checkAndLoadMore() {
        if (shutdownRequested) {
            return;
        }

        if (randomNumbersQueue.size() < queueMinSize && apiRequestCount < maxApiRequests && !isLoading) {
            loadInitialDataAsync();
        } else if (randomNumbersQueue.size() < queueMinSize && currentMode == Mode.PSEUDO) {
            fillQueueWithPseudo();
        }
    }

    private void notifyLoadingStarted() {
        if (shutdownRequested) {
            return;
        }

        listeners.forEach(RNLoadListener::onLoadingStarted);
    }

    private void notifyLoadingCompleted() {
        if (shutdownRequested) {
            return;
        }

        listeners.forEach(RNLoadListener::onLoadingCompleted);
    }

    private void notifyError(String errorMessage) {
        if (shutdownRequested) {
            return;
        }

        listeners.forEach(listener -> listener.onError(errorMessage));
    }

    private void notifyRawDataReceived(String rawData) {
        if (shutdownRequested) {
            return;
        }

        listeners.forEach(listener -> listener.onRawDataReceived(rawData));
    }

    private void notifyModeChanged(Mode mode) {
        if (shutdownRequested) {
            return;
        }

        listeners.forEach(listener -> listener.onModeChanged(mode));
    }

    // ========================================================================
    // Notifications
    // ========================================================================

    private void notifyApiAvailability(boolean isAvailable) {
        if (shutdownRequested) {
            return;
        }

        listeners.forEach(listener -> listener.onApiAvailabilityChanged(isAvailable));
    }

    /**
     * Источник случайных чисел.
     */
    public enum Mode {
        /**
         * Квантовые числа от ANU API
         */
        QUANTUM,
        /**
         * Псевдослучайные числа от L128X256MixRandom (fallback)
         */
        PSEUDO
    }

    @FunctionalInterface
    public interface Sleeper {
        void sleep(long ms) throws InterruptedException;
    }

    /**
     * Queue item with source metadata.
     * Needed so we log only TRUE/QUANTUM numbers and never log fallback PSEUDO values.
     */
    private record RandomNumberEntry(int value, Mode sourceMode) {
    }

    public record ProviderSettings(
            String apiUrl, String apiKey, String dataType,
            int arrayLength, int blockSize, int maxApiRequests,
            int connectTimeout, int readTimeout, int queueMinSize,
            int maxRetries, long initialBackoffMs, long maxBackoffMs
    ) {
        public static ProviderSettings fromConfig() {
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
}
