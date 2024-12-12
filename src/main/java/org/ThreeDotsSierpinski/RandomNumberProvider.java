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
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Класс для получения случайных чисел из внешнего API и предоставления их приложению.
 */
public class RandomNumberProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    // Параметры API из конфигурации
    private static final String API_URL = Config.getString("api.url");
    private static final int MAX_API_REQUESTS = Config.getInt("api.max.requests");
    private static final int CONNECT_TIMEOUT = Config.getInt("api.connect.timeout");
    private static final int READ_TIMEOUT = Config.getInt("api.read.timeout");

    private final BlockingQueue<Integer> randomNumbersQueue; // Очередь для хранения случайных чисел
    private final ObjectMapper objectMapper; // Объект для обработки JSON
    private int apiRequestCount = 0; // Счетчик количества выполненных API-запросов
    private final Object lock = new Object(); // Блокировка для синхронизации вызовов loadInitialData

    private final ExecutorService executorService; // Пул потоков для асинхронных задач
    private volatile boolean isLoading = false; // Флаг загрузки данных

    /**
     * Конструктор для класса RandomNumberProvider.
     * Инициализирует очередь случайных чисел и объект для обработки JSON,
     * затем загружает начальные данные.
     */
    public RandomNumberProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>(); // Инициализация потокобезопасной очереди
        objectMapper = new ObjectMapper(); // Инициализация ObjectMapper для обработки JSON
        executorService = Executors.newSingleThreadExecutor(); // Инициализация пула потоков с одним потоком
        loadInitialDataAsync(); // Асинхронная загрузка начальных данных
    }

    /**
     * Метод для асинхронной загрузки случайных чисел из API.
     * Использует ExecutorService для выполнения загрузки в фоновом потоке.
     */
    private void loadInitialDataAsync() {
        synchronized (lock) {
            if (isLoading || apiRequestCount >= MAX_API_REQUESTS) {
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    LOGGER.warning("Достигнуто максимальное количество запросов к API: " + MAX_API_REQUESTS);
                }
                return; // Предотвращает одновременные вызовы
            }
            isLoading = true; // Устанавливаем флаг загрузки
            executorService.submit(this::loadInitialData); // Отправляем задачу в пул потоков
        }
    }

    /**
     * Метод для загрузки случайных чисел из API.
     * Выполняется в фоновом потоке.
     */
    private void loadInitialData() {
        try {
            int n = 1024; // Количество случайных байтов для загрузки
            String requestUrl = API_URL + "?length=" + n + "&format=HEX"; // Формирование URL запроса

            LOGGER.info("Отправка запроса: " + requestUrl);

            URI uri = new URI(requestUrl); // Создание URI из строки запроса
            URL url = uri.toURL(); // Преобразование URI в URL
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Открытие соединения
            conn.setRequestMethod("GET"); // Установка метода запроса
            conn.setConnectTimeout(CONNECT_TIMEOUT); // Установка таймаута подключения из конфигурации
            conn.setReadTimeout(READ_TIMEOUT); // Установка таймаута чтения из конфигурации

            // Чтение ответа
            String responseBody = getResponseBody(conn);
            LOGGER.info("Получен ответ: " + responseBody);

            // Парсинг JSON-ответа
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("qrn")) {
                String hexData = rootNode.get("qrn").asText(); // Извлечение HEX-строки с числами
                int length = rootNode.get("length").asInt(); // Извлечение длины данных

                // Преобразование HEX-строки в массив байтов
                byte[] byteArray = hexStringToByteArray(hexData);

                // Преобразование байтов в целые числа
                for (byte b : byteArray) {
                    int num = b & 0xFF; // Преобразование беззнакового байта в int (0 до 255)
                    try {
                        randomNumbersQueue.put(num); // Добавление числа в очередь
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Восстановление статуса прерывания
                        LOGGER.log(Level.WARNING, "Поток был прерван при добавлении числа в очередь: " + num, e);
                    }
                }
                LOGGER.info("Загружено " + length + " случайных чисел.");
                synchronized (lock) {
                    apiRequestCount++; // Увеличение счетчика запросов к API
                }
                LOGGER.info("Количество запросов к API: " + apiRequestCount);
            } else if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").asText();
                LOGGER.severe("Ошибка при получении случайных чисел: " + errorMsg);
            } else {
                LOGGER.warning("Неожиданный ответ от сервера.");
            }

            conn.disconnect(); // Отключение соединения
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Некорректный URL: " + API_URL, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Не удалось получить данные из QRNG API.", e);
        } finally {
            synchronized (lock) {
                isLoading = false; // Сбрасываем флаг загрузки
            }
            // Проверяем, нужно ли загрузить еще данные
            if (randomNumbersQueue.size() < 1000 && apiRequestCount < MAX_API_REQUESTS) {
                loadInitialDataAsync();
            }
        }
    }

    /**
     * Вспомогательный метод для чтения тела ответа из соединения.
     *
     * @param conn Объект HttpURLConnection
     * @return Тело ответа в виде строки
     * @throws IOException Если произошла ошибка ввода-вывода
     */
    private static @NotNull String getResponseBody(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String responseLine;

        while ((responseLine = in.readLine()) != null) {
            response.append(responseLine.trim()); // Чтение и добавление строки к ответу
        }
        in.close(); // Закрытие потока ввода

        return response.toString();
    }

    /**
     * Преобразует HEX-строку в массив байтов.
     *
     * @param s HEX-строка для преобразования
     * @return Массив байтов
     * @throws IllegalArgumentException Если HEX-строка некорректна
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Некорректная длина HEX-строки.");
        }
        byte[] data = new byte[len / 2];
        for(int i = 0; i < len; i += 2){
            int high = Character.digit(s.charAt(i),16);
            int low = Character.digit(s.charAt(i+1),16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Обнаружен некорректный символ в HEX-строке.");
            }
            data[i / 2] = (byte) ((high << 4) + low); // Преобразование двух символов в один байт
        }
        return data;
    }

    /**
     * Получает следующее случайное число из очереди.
     *
     * @return Случайное число от 0 до 255.
     * @throws NoSuchElementException Если нет доступных случайных чисел или достигнут предел запросов.
     */
    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS); // Попытка получить число с ожиданием 5 секунд
            if (nextNumber == null) {
                synchronized (lock) {
                    if (apiRequestCount >= MAX_API_REQUESTS) {
                        throw new NoSuchElementException("Достигнуто максимальное количество запросов к API и нет доступных случайных чисел.");
                    }
                }
                loadInitialDataAsync(); // Асинхронная загрузка новых данных
                nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
                if (nextNumber == null) {
                    throw new NoSuchElementException("Нет доступных случайных чисел.");
                }
            }
            // Если осталось мало чисел и лимит запросов не достигнут
            if (randomNumbersQueue.size() < 1000) {
                loadInitialDataAsync(); // Асинхронная загрузка дополнительных данных
            }
            return nextNumber; // Возврат случайного числа
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстановление статуса прерывания
            throw new NoSuchElementException("Ожидание случайного числа было прервано.");
        }
    }

    /**
     * Метод для корректного завершения работы ExecutorService.
     * Должен вызываться при завершении работы приложения.
     */
    public void shutdown() {
        executorService.shutdown(); // Запрос на завершение работы пула потоков
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Принудительное завершение, если не успели корректно завершиться
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.severe("ExecutorService не завершился.");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOGGER.info("ExecutorService успешно завершен.");
    }

    /**
     * Генерирует случайное число в заданном диапазоне [min, max].
     *
     * @param min Нижняя граница диапазона
     * @param max Верхняя граница диапазона
     * @return Случайное число в диапазоне [min, max]
     */
    public long getNextRandomNumberInRange(long min, long max) {
        int randomNum = getNextRandomNumber(); // Получение числа от 0 до 255
        int randomNum2 = getNextRandomNumber(); // Получение второго случайного числа для увеличения энтропии

        // Комбинируем два случайных числа для получения большего разброса
        int combined = (randomNum << 8) | randomNum2; // Объединение двух 8-битных чисел в одно 16-битное

        double normalized = combined / (double) 65535; // Нормализация числа к диапазону [0.0, 1.0]
        long range = max - min; // Вычисление диапазона
        return min + (long) (normalized * range); // Масштабирование числа к заданному диапазону
    }
}
