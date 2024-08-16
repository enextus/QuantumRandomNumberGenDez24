package org.ThreeDotsSierpinski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RandomNumberProvider {
    private static final String API_URL = "https://quantum-random.com/quantum";
    private final HttpClient httpClient;
    private final List<Integer> integerList;
    private final ObjectMapper objectMapper;

    public RandomNumberProvider() {
        httpClient = HttpClient.newHttpClient();
        integerList = new CopyOnWriteArrayList<>();
        objectMapper = new ObjectMapper(); // Jackson object mapper for JSON parsing
        loadInitialData();
    }

    private void loadInitialData() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body().substring(0, Math.min(500, response.body().length()))); // Print first 500 characters of the response

            if (response.headers().firstValue("Content-Type").orElse("").contains("application/json")) {
                int[] data = parseJsonResponse(response.body());
                for (int num : data) {
                    integerList.add(num);
                }
            } else {
                System.err.println("Unexpected content type received: " + response.headers().firstValue("Content-Type").orElse("unknown"));
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int[] parseJsonResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode numbersNode = rootNode.path("data").path("numbers");
            if (numbersNode.isArray()) {
                return objectMapper.convertValue(numbersNode, int[].class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new int[] {};  // Return empty array in case of an error or if the path is not found
    }

    public int getNextRandomNumber() {
        if (integerList.isEmpty()) {
            loadInitialData();
        }
        return integerList.isEmpty() ? 0 : integerList.remove(0);
    }
}
