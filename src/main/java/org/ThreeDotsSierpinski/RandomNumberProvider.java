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
        String requestUrl = API_URL + "?length=10&type=uint8"; // Modify these parameters as needed
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header("Accept", "application/json")
                .header("Authorization", "Apikey " + apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.headers().firstValue("Content-Type").orElse("").contains("application/json")) {
                int[] data = parseJsonResponse(response.body());
                for (int num : data) {
                    integerList.add(num);
                }
            } else {
                System.err.println("Unexpected response or content type received: " + response.headers().firstValue("Content-Type").orElse("unknown"));
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to retrieve data");
            e.printStackTrace();
        }
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

    public int getNextRandomNumber() {
        if (integerList.isEmpty()) {
            loadInitialData();
        }
        return integerList.isEmpty() ? 0 : integerList.remove(0);
    }
}
