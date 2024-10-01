package org.ThreeDotsSierpinski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RandomNumberProvider {
    private static final String API_URL = "https://api.random.org/json-rpc/4/invoke";
    private final BlockingQueue<Integer> randomNumbersQueue;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    // Конструктор
    public RandomNumberProvider() {
        apiKey = System.getenv("RANDOM_ORG_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API ключ не найден!");
        }

        randomNumbersQueue = new LinkedBlockingQueue<>();
        objectMapper = new ObjectMapper();
        loadInitialData();
    }

    // Метод для загрузки данных
    private void loadInitialData() {
        // Формирование JSON-запроса
        JSONObject params = new JSONObject();
        params.put("apiKey", apiKey);
        params.put("n", 100);
        params.put("min", -99999999);
        params.put("max", 100000000);
        params.put("replacement", true);

        JSONObject requestJson = new JSONObject();
        requestJson.put("jsonrpc", "2.0");
        requestJson.put("method", "generateIntegers");
        requestJson.put("params", params);
        requestJson.put("id", 6004);

        String jsonRequest = requestJson.toString();

        System.out.println("Отправляемый JSON-запрос:");
        System.out.println(jsonRequest);

        // Настройка HTTP-клиента
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(StandardCookieSpec.IGNORE).build())
                .build()) {

            HttpPost httpPost = new HttpPost(API_URL);
            httpPost.setEntity(new StringEntity(jsonRequest, ContentType.APPLICATION_JSON));

            // Установка заголовков
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "Apache-HttpClient");

            // Логирование заголовков запроса
            System.out.println("Заголовки запроса:");
            for (Header header : httpPost.getHeaders()) {
                System.out.println(header.getName() + ": " + header.getValue());
            }

            // Отправка запроса
            System.out.println("Отправка запроса к RANDOM.ORG API");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            try {
                int responseCode = response.getCode();
                System.out.println("--------------------------------------------------------------------");
                System.out.println("Получен ответ с кодом: " + responseCode);

                String responseBody = "";
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }

                System.out.println("Тело ответа: " + responseBody);
                System.out.println("--------------------------------------------------------------------");

                if (responseCode == 200) {
                    int[] data = parseJsonResponse(responseBody);
                    for (int num : data) {
                        randomNumbersQueue.offer(num);
                    }
                } else {
                    System.err.println("Неожиданный код ответа: " + responseCode);
                    System.err.println("Сообщение об ошибке: " + responseBody);
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            } finally {
                response.close();
            }

        } catch (IOException e) {
            System.err.println("Не удалось получить данные от RANDOM.ORG API");
            e.printStackTrace();
        }
    }

    // Метод для парсинга случайных чисел из ответа
    private int[] parseJsonResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultNode = rootNode.path("result").path("random").path("data");
            if (resultNode.isArray()) {
                return objectMapper.convertValue(resultNode, int[].class);
            } else {
                System.err.println("Поле 'data' отсутствует или не является массивом");
            }
        } catch (IOException e) {
            System.err.println("Не удалось разобрать JSON ответ");
            e.printStackTrace();
        }
        return new int[]{}; // Возвращаем пустой массив в случае ошибки
    }

    // Метод для получения следующего случайного числа
    public int getNextRandomNumber() {
        try {
            Integer nextNumber = randomNumbersQueue.poll(5, TimeUnit.SECONDS);
            if (nextNumber == null) {
                throw new NoSuchElementException("Нет доступных случайных чисел");
            }
            // Если осталось мало чисел в очереди, загружаем новые
            if (randomNumbersQueue.size() < 1000) {
                loadInitialData();
            }
            return nextNumber;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NoSuchElementException("Ожидание случайного числа было прервано");
        }
    }

    // Метод для получения статистики использования API
    public void getUsage() {
        // Формирование JSON-запроса
        JSONObject params = new JSONObject();
        params.put("apiKey", apiKey);

        JSONObject requestJson = new JSONObject();
        requestJson.put("jsonrpc", "2.0");
        requestJson.put("method", "getUsage");
        requestJson.put("params", params);
        requestJson.put("id", 6005);

        String jsonRequest = requestJson.toString();

        System.out.println("Отправляемый JSON-запрос для getUsage:");
        System.out.println(jsonRequest);

        // Настройка HTTP-клиента
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(StandardCookieSpec.IGNORE).build())
                .build()) {

            HttpPost httpPost = new HttpPost(API_URL);
            httpPost.setEntity(new StringEntity(jsonRequest, ContentType.APPLICATION_JSON));

            // Установка заголовков
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("User-Agent", "Apache-HttpClient");

            // Логирование заголовков запроса
            System.out.println("Заголовки запроса:");
            for (Header header : httpPost.getHeaders()) {
                System.out.println(header.getName() + ": " + header.getValue());
            }

            // Отправка запроса
            System.out.println("Отправка запроса на проверку использования API");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            try {
                int responseCode = response.getCode();
                System.out.println("--------------------------------------------------------------------");
                System.out.println("Получен ответ с кодом: " + responseCode);

                String responseBody = "";
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }

                System.out.println("Тело ответа: " + responseBody);
                System.out.println("--------------------------------------------------------------------");

                if (responseCode == 200) {
                    parseUsageResponse(responseBody);
                } else {
                    System.err.println("Неожиданный код ответа: " + responseCode);
                    System.err.println("Сообщение об ошибке: " + responseBody);
                }
            } catch (ParseException e) {
                throw new RuntimeException(e);
            } finally {
                response.close();
            }

        } catch (IOException e) {
            System.err.println("Не удалось получить данные об использовании API");
            e.printStackTrace();
        }
    }

    // Метод для парсинга ответа об использовании API
    private void parseUsageResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultNode = rootNode.path("result");
            if (!resultNode.isMissingNode()) {
                int requestsLeft = resultNode.path("requestsLeft").asInt();
                int bitsLeft = resultNode.path("bitsLeft").asInt();
                System.out.printf("Запросов осталось: %d, Битов осталось: %d%n", requestsLeft, bitsLeft);
            } else {
                System.err.println("Ошибка: Не удалось получить информацию об использовании API");
            }
        } catch (IOException e) {
            System.err.println("Ошибка при разборе ответа об использовании API");
            e.printStackTrace();
        }
    }

    // Метод для получения выборки случайных чисел
    public int[] getRandomNumbersSample(int sampleSize) {
        int[] sample = new int[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            sample[i] = getNextRandomNumber();
        }
        return sample;
    }
}
