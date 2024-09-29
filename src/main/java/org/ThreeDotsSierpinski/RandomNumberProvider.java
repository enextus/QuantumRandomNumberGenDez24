package org.ThreeDotsSierpinski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

public class RandomNumberProvider {
    private static final String API_URL = "https://api.random.org/json-rpc/4/invoke"; // API URL RANDOM.ORG
    private final HttpClient httpClient;
    private final List<Integer> integerList;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    // Конструктор инициализирует ключ API из переменной окружения
    public RandomNumberProvider() {
        apiKey = System.getenv("RANDOM_ORG_API_KEY"); // Извлекаем API ключ из переменной окружения
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API ключ не найден! Убедитесь, что переменная окружения RANDOM_ORG_API_KEY установлена.");
        }

        httpClient = HttpClient.newHttpClient();
        integerList = new CopyOnWriteArrayList<>();
        objectMapper = new ObjectMapper(); // Jackson object mapper для парсинга JSON
        loadInitialData(); // Загружаем начальные данные при инициализации
    }

    // Метод для получения случайных чисел
    private void loadInitialData() {
        String jsonRequest = """
        {
            "jsonrpc": "2.0",
            "method": "generateIntegers",
            "params": {
                "apiKey": "%s",
                "n": 10,
                "min": 1,
                "max": 10,
                "replacement": true,
                "base": 10,
                "pregeneratedRandomization": null
            },
            "id": 6004
        }
        """.formatted(apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        System.out.println("Отправка запроса к RANDOM.ORG API");

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Получен ответ с кодом: " + response.statusCode());
            System.out.println("Тело ответа: " + response.body());

            if (response.statusCode() == 200) {
                int[] data = parseJsonResponse(response.body());
                System.out.println("Получено случайных чисел: " + data.length);
                for (int num : data) {
                    integerList.add(num);
                }
            } else {
                System.err.println("Неожиданный код ответа: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
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
        return new int[] {}; // Возвращаем пустой массив в случае ошибки
    }

    // Метод для получения следующего случайного числа
    public synchronized int getNextRandomNumber() {
        if (integerList.isEmpty()) {
            loadInitialData();
        }
        if (integerList.isEmpty()) {
            throw new NoSuchElementException("Нет доступных случайных чисел");
        }
        return integerList.remove(0);
    }

    // Метод для получения статистики использования API
    public void getUsage() {
        String jsonRequest = """
        {
            "jsonrpc": "2.0",
            "method": "getUsage",
            "params": {
                "apiKey": "%s"
            },
            "id": 6005
        }
        """.formatted(apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        System.out.println("Отправка запроса на проверку использования API");

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Получен ответ с кодом: " + response.statusCode());
            if (response.statusCode() == 200) {
                parseUsageResponse(response.body());
            } else {
                System.err.println("Неожиданный код ответа: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
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
}
