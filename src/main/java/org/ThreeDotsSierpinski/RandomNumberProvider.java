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

public class RandomNumberProvider {
    private static final String API_URL = "https://lfdr.de/qrng_api/qrng";
    private static final int MAX_API_REQUESTS = 50; // Ограничение до 50 запросов
    private final BlockingQueue<Integer> randomNumbersQueue;
    private final ObjectMapper objectMapper;
    private int apiRequestCount = 0; // Счетчик запросов к API

    // Конструктор
    public RandomNumberProvider() {
        randomNumbersQueue = new LinkedBlockingQueue<>();
        objectMapper = new ObjectMapper();
        loadInitialData();
    }

    // Метод для загрузки данных
    private void loadInitialData() {
        // Проверяем, достигли ли мы максимального количества запросов
        if (apiRequestCount >= MAX_API_REQUESTS) {
            System.err.println("Достигнуто максимальное количество запросов к API: " + MAX_API_REQUESTS);
            return;
        }

        int n = 1024; // Количество случайных байтов для загрузки
        String requestUrl = API_URL + "?length=" + n + "&format=HEX";

        System.out.println("Отправляемый запрос: " + requestUrl);

        try {
            URI uri = new URI(requestUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Чтение ответа
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String responseLine;

            while ((responseLine = in.readLine()) != null) {
                response.append(responseLine.trim());
            }
            in.close();

            String responseBody = response.toString();
            System.out.println("Получен ответ: " + responseBody);

            // Парсинг JSON-ответа
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.has("qrn")) {
                String hexData = rootNode.get("qrn").asText();
                int length = rootNode.get("length").asInt();

                // Преобразование HEX-строки в массив байтов
                byte[] byteArray = hexStringToByteArray(hexData);

                // Преобразование байтов в целые числа
                for (byte b : byteArray) {
                    int num = b & 0xFF; // Преобразование беззнакового байта в int (от 0 до 255)
                    try {
                        randomNumbersQueue.put(num);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Поток был прерван при добавлении числа в очередь: " + num);
                    }
                }
                System.out.println("Загружено " + length + " случайных чисел.");

                apiRequestCount++;
                System.out.println("Количество запросов к API: " + apiRequestCount);

            } else if (rootNode.has("error")) {
                System.err.println("Ошибка при получении случайных чисел: " + rootNode.get("error").asText());
            } else {
                System.err.println("Неожиданный ответ от сервера.");
            }

            conn.disconnect();
        } catch (URISyntaxException e) {
            System.err.println("Некорректный URL: " + requestUrl);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Не удалось получить данные от QRNG API");
            e.printStackTrace();
        }
    }

    // Метод для преобразования HEX-строки в массив байтов
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for(int i = 0; i < len; i += 2){
            data[i / 2] = (byte) ((Character.digit(s.charAt(i),16) << 4)
                    + Character.digit(s.charAt(i+1),16));
        }
        return data;
    }

    /**
     * Получает следующее случайное число из очереди.
     *
     * @return Случайное число от 0 до 255.
     * @throws NoSuchElementException Если нет доступных случайных чисел или достигнут лимит запросов.
     */
    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
            if (nextNumber == null) {
                // Если достигли максимального количества запросов и очередь пуста
                if (apiRequestCount >= MAX_API_REQUESTS) {
                    throw new NoSuchElementException("Достигнуто максимальное количество запросов к API и нет доступных случайных чисел");
                } else {
                    // Если лимит не достигнут, но очередь пуста, пытаемся загрузить новые данные
                    loadInitialData();
                    // После загрузки пытаемся снова получить число
                    nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
                    if (nextNumber == null) {
                        throw new NoSuchElementException("Нет доступных случайных чисел");
                    }
                }
            }
            // Если осталось мало чисел и лимит не достигнут
            if (randomNumbersQueue.size() < 1000 && apiRequestCount < MAX_API_REQUESTS) {
                loadInitialData();
            }
            return nextNumber;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Ожидание случайного числа было прервано");
        }
    }

    /**
     * Получает случайное число в заданном диапазоне.
     *
     * @param min Нижняя граница диапазона.
     * @param max Верхняя граница диапазона.
     * @return Случайное число типа long в диапазоне [min, max].
     */
    public long getNextRandomNumberInRange(long min, long max) {
        int randomNum = getNextRandomNumber(); // Получаем число от 0 до 255
        double normalized = randomNum / 255.0;
        long range = max - min;
        long scaledNum = min + (long)(normalized * range);
        return scaledNum;
    }
}
