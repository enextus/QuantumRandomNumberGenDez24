package org.ThreeDotsSierpinski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

/**
 * Класс для получения случайных чисел из внешнего API и предоставления их приложению.
 *
 * The RandomNumberProvider class retrieves random numbers from an external API and provides them to the application.
 */
public class RandomNumberProvider {
    private static final String API_URL = "https://lfdr.de/qrng_api/qrng"; // URL API для получения случайных чисел
    // API URL for fetching random numbers
    private static final int MAX_API_REQUESTS = 50; // Максимальное количество запросов к API
    // Maximum number of API requests
    private final BlockingQueue<Integer> randomNumbersQueue; // Очередь для хранения случайных чисел
    // Queue for storing random numbers
    private final ObjectMapper objectMapper; // Объект для обработки JSON
    // Object for handling JSON
    private int apiRequestCount = 0; // Счетчик сделанных запросов к API
    // Counter for the number of API requests made

    /**
     * Конструктор класса RandomNumberProvider.
     * Инициализирует очередь случайных чисел и объект для обработки JSON,
     * затем загружает начальные данные.
     *
     * Constructor for the RandomNumberProvider class.
     * Initializes the random numbers queue and the JSON handler object,
     * then loads the initial data.
     */
    public RandomNumberProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>(); // Инициализация потокобезопасной очереди
        // Initializing a thread-safe queue
        objectMapper = new ObjectMapper(); // Инициализация ObjectMapper для обработки JSON
        // Initializing ObjectMapper for JSON processing
        loadInitialData(); // Загрузка начальных данных
        // Loading initial data
    }

    /**
     * Метод для загрузки случайных чисел из API.
     * Проверяет, достигнуто ли максимальное количество запросов,
     * формирует запрос к API, обрабатывает ответ и добавляет числа в очередь.
     *
     * Method to load random numbers from the API.
     * Checks if the maximum number of requests has been reached,
     * forms an API request, processes the response, and adds numbers to the queue.
     */
    private void loadInitialData() {
        // Проверяем, достигли ли мы максимального количества запросов
        // Checking if the maximum number of requests has been reached
        if (apiRequestCount >= MAX_API_REQUESTS) {
            System.err.println("Достигнуто максимальное количество запросов к API: " + MAX_API_REQUESTS);
            // Maximum number of API requests reached
            return;
        }

        int n = 1024; // Количество случайных байтов для загрузки
        // Number of random bytes to load
        String requestUrl = API_URL + "?length=" + n + "&format=HEX"; // Формирование URL запроса
        // Forming the request URL

        System.out.println("Отправляемый запрос: " + requestUrl);
        // Sending request

        try {
            URI uri = new URI(requestUrl); // Создание URI из строки запроса
            // Creating URI from the request string
            URL url = uri.toURL(); // Преобразование URI в URL
            // Converting URI to URL
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Открытие соединения
            // Opening connection
            conn.setRequestMethod("GET"); // Установка метода запроса

            // Чтение ответа
            // Reading the response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String responseLine;

            while ((responseLine = in.readLine()) != null) {
                response.append(responseLine.trim()); // Чтение и добавление строки к ответу
                // Reading and appending the line to the response
            }
            in.close(); // Закрытие потока чтения
            // Closing the input stream

            String responseBody = response.toString();
            System.out.println("Получен ответ: " + responseBody);
            // Response received

            // Парсинг JSON-ответа
            // Parsing the JSON response
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("qrn")) {
                String hexData = rootNode.get("qrn").asText(); // Извлечение HEX-строки с числами
                // Extracting HEX string with numbers
                int length = rootNode.get("length").asInt(); // Извлечение длины данных
                // Extracting data length

                // Преобразование HEX-строки в массив байтов
                // Converting HEX string to byte array
                byte[] byteArray = hexStringToByteArray(hexData);

                // Преобразование байтов в целые числа
                // Converting bytes to integers
                for (byte b : byteArray) {
                    int num = b & 0xFF; // Преобразование беззнакового байта в int (от 0 до 255)
                    // Converting unsigned byte to int (0 to 255)
                    try {
                        randomNumbersQueue.put(num); // Добавление числа в очередь
                        // Adding number to the queue
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Восстановление состояния прерывания
                        System.err.println("Поток был прерван при добавлении числа в очередь: " + num);
                        // Thread was interrupted while adding number to the queue
                    }
                }
                System.out.println("Загружено " + length + " случайных чисел.");
                // Loaded [length] random numbers

                apiRequestCount++; // Увеличение счетчика запросов
                // Incrementing the API request counter
                System.out.println("Количество запросов к API: " + apiRequestCount);
                // Number of API requests: [apiRequestCount]

            } else if (rootNode.has("error")) {
                System.err.println("Ошибка при получении случайных чисел: " + rootNode.get("error").asText());
                // Error while fetching random numbers: [error message]
            } else {
                System.err.println("Неожиданный ответ от сервера.");
                // Unexpected response from the server
            }

            conn.disconnect(); // Закрытие соединения
            // Disconnecting the connection
        } catch (URISyntaxException e) {
            System.err.println("Некорректный URL: " + requestUrl);
            // Incorrect URL
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Не удалось получить данные от QRNG API");
            // Failed to fetch data from QRNG API
            e.printStackTrace();
        }
    }

    /**
     * Метод для преобразования HEX-строки в массив байтов.
     *
     * Converts a HEX string to a byte array.
     *
     * @param s HEX-строка для преобразования
     *          HEX string to convert
     * @return Массив байтов
     *         Byte array
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for(int i = 0; i < len; i += 2){
            data[i / 2] = (byte) ((Character.digit(s.charAt(i),16) << 4)
                    + Character.digit(s.charAt(i+1),16)); // Преобразование двух символов в один байт
            // Converting two characters into one byte
        }
        return data;
    }

    /**
     * Получает следующее случайное число из очереди.
     *
     * Gets the next random number from the queue.
     *
     * @return Случайное число от 0 до 255.
     *         A random number between 0 and 255.
     * @throws NoSuchElementException Если нет доступных случайных чисел или достигнут лимит запросов.
     *                                If there are no available random numbers or the request limit has been reached.
     */
    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS); // Попытка получить число с ожиданием 5 секунд
            // Attempting to retrieve a number with a 5-second wait
            if (nextNumber == null) {
                // Если достигли максимального количества запросов и очередь пуста
                // If the maximum number of requests has been reached and the queue is empty
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    throw new NoSuchElementException("Достигнуто максимальное количество запросов к API и нет доступных случайных чисел");
                    // "Maximum number of API requests reached and no available random numbers"
                } else {
                    // Если лимит не достигнут, но очередь пуста, пытаемся загрузить новые данные
                    // If the limit has not been reached but the queue is empty, attempt to load new data
                    loadInitialData();
                    // After loading, attempt to retrieve the number again
                    nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
                    if (nextNumber == null) {
                        throw new NoSuchElementException("Нет доступных случайных чисел");
                        // "No available random numbers"
                    }
                }
            }
            // Если осталось мало чисел и лимит не достигнут
            // If there are few numbers left and the limit has not been reached
            if (randomNumbersQueue.size() < 1000 && apiRequestCount < MAX_API_REQUESTS) {
                loadInitialData(); // Загрузка дополнительных данных
                // Loading additional data
            }
            return nextNumber; // Возвращение случайного числа
            // Returning the random number
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстановление состояния прерывания
            throw new NoSuchElementException("Ожидание случайного числа было прервано");
            // "Waiting for a random number was interrupted"
        }
    }

    /**
     * Получает случайное число в заданном диапазоне.
     *
     * Gets a random number within a specified range.
     *
     * @param min Нижняя граница диапазона.
     *            The lower bound of the range.
     * @param max Верхняя граница диапазона.
     *            The upper bound of the range.
     * @return Случайное число типа long в диапазоне [min, max].
     *         A random long number in the range [min, max].
     */
    public long getNextRandomNumberInRange(long min, long max) {
        int randomNum = getNextRandomNumber(); // Получаем число от 0 до 255
        // Getting a number between 0 and 255
        double normalized = randomNum / 255.0; // Нормализация числа до диапазона [0.0, 1.0]
        // Normalizing the number to the range [0.0, 1.0]
        long range = max - min; // Вычисление диапазона
        // Calculating the range
        long scaledNum = min + (long)(normalized * range); // Масштабирование числа до заданного диапазона
        // Scaling the number to the specified range
        return scaledNum; // Возвращение масштабированного числа
        // Returning the scaled number
    }
}
