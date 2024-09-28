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
    private final String apiKey = "YatAtygYlm1guUo5z22GU7C9GHmYsXAp1rwXAU52";  // Replace this with your actual API key

    public RandomNumberProvider() {
        httpClient = HttpClient.newHttpClient();
        integerList = new CopyOnWriteArrayList<>();
        objectMapper = new ObjectMapper(); // Jackson object mapper for JSON parsing
        loadInitialData();
    }

    private void loadInitialData() {
        String requestUrl = API_URL + "?length=10&type=uint8";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Accept", "application/json")
                .header("Authorization", "Apikey " + apiKey)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200 && response.headers().firstValue("Content-Type").orElse("").contains("application/json")) {
                        return parseJsonResponse(response.body());
                    } else {
                        throw new RuntimeException("Unexpected response: " + response.statusCode());
                    }
                })
                .thenAccept(data -> {
                    for (int num : data) {
                        integerList.add(num);
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Failed to retrieve data: " + e.getMessage());
                    return null;
                });
    }


    private int[] parseJsonResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode numbersNode = rootNode.path("data");
            if (numbersNode.isArray()) {
                return objectMapper.convertValue(numbersNode, int[].class);
            }
        } catch (IOException e) {
            System.err.println("Failed to parse JSON response");
            e.printStackTrace();
        }
        return new int[] {};  // Return an empty array in case of error
    }

    public int getNextRandomNumber() throws NoSuchElementException {
        if (integerList.isEmpty()) {
            loadInitialData();
        }
        if (integerList.isEmpty()) {
            throw new NoSuchElementException("Нет доступных случайных чисел");
        }
        return integerList.remove(0);
    }

}
