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
    private static final String API_URL = "https://qrng.anu.edu.au/API/jsonI.php";
    private final HttpClient httpClient;
    private final List<Integer> integerList;
    private final ObjectMapper objectMapper;
    private final String apiKey = "YatAtygYlm1guUo5z22GU7C9GHmYsXAp1rwXAU52";  // Убедитесь, что ключ корректный

    public RandomNumberProvider() {
        httpClient = HttpClient.newHttpClient();
        integerList = new CopyOnWriteArrayList<>();
        objectMapper = new ObjectMapper(); // Jackson object mapper для парсинга JSON
        loadInitialData();
    }

    private void loadInitialData() {
        String requestUrl = API_URL + "?length=10&type=uint8"; // Параметры запроса
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Accept", "application/json")
                .header("Authorization", "Apikey " + apiKey) // Проверьте, требуется ли этот заголовок
                .GET()
                .build();

        System.out.println("Отправка запроса к API: " + requestUrl);

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Получен ответ с кодом: " + response.statusCode());
            System.out.println("Заголовки ответа: " + response.headers().map());

            if (response.statusCode() == 200 && response.headers().firstValue("Content-Type").orElse("").contains("application/json")) {
                int[] data = parseJsonResponse(response.body());
                System.out.println("Получено случайных чисел: " + data.length);
                for (int num : data) {
                    integerList.add(num);
                }
            } else {
                System.err.println("Непредвиденный ответ или тип контента: " + response.headers().firstValue("Content-Type").orElse("unknown"));
                System.err.println("Тело ответа: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Не удалось получить данные от API");
            e.printStackTrace();
        }
    }

    private int[] parseJsonResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode numbersNode = rootNode.path("data");
            if (numbersNode.isArray()) {
                return objectMapper.convertValue(numbersNode, int[].class);
            } else {
                System.err.println("Поле 'data' отсутствует или не является массивом");
            }
        } catch (IOException e) {
            System.err.println("Не удалось разобрать JSON ответ");
            e.printStackTrace();
        }
        return new int[] {};  // Возвращаем пустой массив в случае ошибки
    }

    public int getNextRandomNumber() {
        if (integerList.isEmpty()) {
            loadInitialData();
        }
        if (integerList.isEmpty()) {
            throw new NoSuchElementException("Нет доступных случайных чисел");
        }
        return integerList.removeFirst();
    }
}
