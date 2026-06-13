package org.ThreeDotsSierpinski.rng;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ThreeDotsSierpinski.config.LoggerConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Small ANU Quantum Numbers API client used by RNProvider.
 */
final class QuantumNumbersApiClient {

    private static final Logger LOGGER = LoggerConfig.getLogger();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String DATA_NODE = "data";
    private static final String MESSAGE_NODE = "message";
    private static final int MAX_API_LENGTH = 1024;
    private static final int MAX_API_BLOCK_SIZE = 1024;

    private final RNProvider.ProviderSettings settings;
    private final HttpClient httpClient;

    QuantumNumbersApiClient(RNProvider.ProviderSettings settings) {
        this.settings = settings;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(settings.connectTimeout()))
                .build();
    }

    QuantumNumbersApiResponse fetchNumbers() throws IOException, InterruptedException {
        String requestUrl = buildRequestUrl();
        LOGGER.info("Sending request: " + requestUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .header(API_KEY_HEADER, settings.apiKey())
                .timeout(Duration.ofMillis(settings.readTimeout()))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();

        if (statusCode != 200) {
            String errorBody = response.body();
            LOGGER.severe("HTTP error: " + statusCode + " - " + errorBody);

            if (statusCode == 429) {
                throw new RateLimitException(errorBody);
            }
            throw new IOException("HTTP error code: " + statusCode + " - " + errorBody);
        }

        String responseBody = response.body();
        LOGGER.info("Received response: "
                + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");

        JsonNode rootNode = OBJECT_MAPPER.readTree(responseBody);

        if (rootNode.has(DATA_NODE)) {
            JsonNode dataNode = rootNode.get(DATA_NODE);

            if (!dataNode.isArray()) {
                throw new IOException("Invalid response format: 'data' is not an array.");
            }

            return new QuantumNumbersApiResponse(responseBody, parseNumbers(dataNode));
        }

        if (rootNode.has(MESSAGE_NODE)) {
            throw new IOException("API Error: " + rootNode.get(MESSAGE_NODE).asText());
        }

        throw new IOException("Unexpected response from server.");
    }

    private String buildRequestUrl() {
        StringBuilder url = new StringBuilder(settings.apiUrl());
        url.append("?length=").append(Math.min(settings.arrayLength(), MAX_API_LENGTH));
        url.append("&type=").append(settings.dataType());
        if ("hex16".equals(settings.dataType())) {
            url.append("&size=").append(Math.min(settings.blockSize(), MAX_API_BLOCK_SIZE));
        }
        return url.toString();
    }

    private List<Integer> parseNumbers(JsonNode dataNode) {
        List<Integer> numbers = new ArrayList<>();
        for (JsonNode element : dataNode) {
            if ("hex16".equals(settings.dataType())) {
                numbers.add(Integer.parseInt(element.asText(), 16));
            } else {
                numbers.add(element.asInt());
            }
        }
        return numbers;
    }
}

record QuantumNumbersApiResponse(String rawData, List<Integer> numbers) {
}

class RateLimitException extends IOException {
    RateLimitException(String message) {
        super(message);
    }
}
