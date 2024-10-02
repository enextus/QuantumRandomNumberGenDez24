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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Class for fetching random numbers from an external API and providing them to the application.
 * <p>
 * Класс для получения случайных чисел из внешнего API и предоставления их приложению.
 */
public class RandomNumberProvider {
    private static final Logger LOGGER = LoggerConfig.getLogger();

    private static final String API_URL = "https://lfdr.de/qrng_api/qrng"; // API URL for fetching random numbers
    private static final int MAX_API_REQUESTS = 50; // Maximum number of API requests
    private final BlockingQueue<Integer> randomNumbersQueue; // Queue for storing random numbers
    private final ObjectMapper objectMapper; // Object for handling JSON
    private int apiRequestCount = 0; // Counter for the number of API requests made

    /**
     * Constructor for the RandomNumberProvider class.
     * Initializes the random numbers queue and the JSON handler object,
     * then loads the initial data.
     * <p>
     * Конструктор класса RandomNumberProvider.
     * Инициализирует очередь случайных чисел и объект для обработки JSON,
     * затем загружает начальные данные.
     */
    public RandomNumberProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>(); // Initializing a thread-safe queue
        objectMapper = new ObjectMapper(); // Initializing ObjectMapper for JSON processing
        loadInitialData(); // Loading initial data
    }

    /**
     * Method to load random numbers from the API.
     * Checks if the maximum number of requests has been reached,
     * forms an API request, processes the response, and adds numbers to the queue.
     * <p>
     * Метод для загрузки случайных чисел из API.
     * Проверяет, достигнуто ли максимальное количество запросов,
     * формирует запрос к API, обрабатывает ответ и добавляет числа в очередь.
     */
    private void loadInitialData() {
        // Checking if the maximum number of requests has been reached
        if (apiRequestCount >= MAX_API_REQUESTS) {
            LOGGER.warning("Maximum number of API requests reached: " + MAX_API_REQUESTS);
            return;
        }

        int n = 1024; // Number of random bytes to load
        String requestUrl = API_URL + "?length=" + n + "&format=HEX"; // Forming the request URL

        LOGGER.info("Sending request: " + requestUrl);

        try {
            URI uri = new URI(requestUrl); // Creating URI from the request string
            URL url = uri.toURL(); // Converting URI to URL
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Opening connection
            conn.setRequestMethod("GET"); // Setting the request method

            // Reading the response
            String responseBody = getResponseBody(conn);
            LOGGER.info("Received response: " + responseBody);

            // Parsing the JSON response
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("qrn")) {
                String hexData = rootNode.get("qrn").asText(); // Extracting HEX string with numbers
                int length = rootNode.get("length").asInt(); // Extracting data length

                // Converting HEX string to byte array
                byte[] byteArray = hexStringToByteArray(hexData);

                // Converting bytes to integers
                for (byte b : byteArray) {
                    int num = b & 0xFF; // Converting unsigned byte to int (0 to 255)
                    try {
                        randomNumbersQueue.put(num); // Adding number to the queue
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restoring the interrupt status
                        LOGGER.log(Level.WARNING, "Thread was interrupted while adding number to the queue: " + num, e);
                    }
                }
                LOGGER.info("Loaded " + length + " random numbers.");
                apiRequestCount++; // Incrementing the API request counter
                LOGGER.info("Number of API requests: " + apiRequestCount);
            } else if (rootNode.has("error")) {
                String errorMsg = rootNode.get("error").asText();
                LOGGER.severe("Error while fetching random numbers: " + errorMsg);
            } else {
                LOGGER.warning("Unexpected response from the server.");
            }

            conn.disconnect(); // Disconnecting the connection
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Incorrect URL: " + requestUrl, e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch data from QRNG API.", e);
        }
    }

    /**
     * Helper method to read the response body from the connection.
     *
     * @param conn The HttpURLConnection object
     * @return The response body as a String
     * @throws IOException If an I/O error occurs
     */
    private static @NotNull String getResponseBody(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String responseLine;

        while ((responseLine = in.readLine()) != null) {
            response.append(responseLine.trim()); // Reading and appending the line to the response
        }
        in.close(); // Closing the input stream

        return response.toString();
    }

    /**
     * Converts a HEX string to a byte array.
     * <p>
     * Метод для преобразования HEX-строки в массив байтов.
     *
     * @param s HEX string to convert
     * @return Byte array
     * @throws IllegalArgumentException If the HEX string is invalid
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
                throw new IllegalArgumentException("Invalid HEX character detected.");
            }
            data[i / 2] = (byte) ((high << 4) + low); // Converting two characters into one byte
        }
        return data;
    }

    /**
     * Gets the next random number from the queue.
     * <p>
     * Получает следующее случайное число из очереди.
     *
     * @return A random number between 0 and 255.
     * @throws NoSuchElementException If there are no available random numbers or the request limit has been reached.
     */
    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS); // Attempting to retrieve a number with a 5-second wait
            if (nextNumber == null) {
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    throw new NoSuchElementException("Maximum number of API requests reached and no available random numbers.");
                } else {
                    loadInitialData(); // Attempting to load new data
                    nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
                    if (nextNumber == null) {
                        throw new NoSuchElementException("No available random numbers.");
                    }
                }
            }
            // If there are few numbers left and the limit has not been reached
            if (randomNumbersQueue.size() < 1000 && apiRequestCount < MAX_API_REQUESTS) {
                loadInitialData(); // Loading additional data
            }
            return nextNumber; // Returning the random number
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restoring the interrupt status
            throw new NoSuchElementException("Waiting for a random number was interrupted.");
        }
    }

    /**
     * Gets a random number within a specified range.
     * <p>
     * Получает случайное число в заданном диапазоне.
     *
     * @param min The lower bound of the range.
     *            Нижняя граница диапазона.
     * @param max The upper bound of the range.
     *            Верхняя граница диапазона.
     * @return A random long number in the range [min, max].
     *         Случайное число типа long в диапазоне [min, max].
     */
    public long getNextRandomNumberInRange(long min, long max) {
        int randomNum = getNextRandomNumber(); // Getting a number between 0 and 255
        double normalized = randomNum / 255.0; // Normalizing the number to the range [0.0, 1.0]
        long range = max - min; // Calculating the range
        long scaledNum = min + (long)(normalized * range); // Scaling the number to the specified range
        return scaledNum; // Returning the scaled number
    }
}
