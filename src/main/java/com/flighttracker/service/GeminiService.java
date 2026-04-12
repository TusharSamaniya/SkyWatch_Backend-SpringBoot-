package com.flighttracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class GeminiService {
	
	@Value("${gemini.api.key}")
	private String geminiApiKey;
		
	private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public GeminiService(WebClient.Builder webClientBuilder) {
        // This is the official Google Gemini 1.5 Flash endpoint
        this.webClient = webClientBuilder.baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent").build();
        this.objectMapper = new ObjectMapper();
    }
    
    public String generateFlightStory(String callsign, String dep, String arr, String aircraft, int altitude) {
        log.info("Asking Gemini to write a story for flight {}...", callsign);

        // 1. The Prompt: We tell the AI exactly what we want it to do
        String prompt = String.format(
            "You are a professional aviation expert writing a short, exciting summary for a flight tracking app. " +
            "Write a 3-sentence paragraph about flight %s. It is a %s aircraft flying from %s to %s, currently cruising at %d feet. " +
            "Make it sound cinematic but factual. Do not use asterisks or bold formatting.",
            callsign, aircraft, dep, arr, altitude
        );

        // 2. The JSON Payload: Formatting the request exactly how Google expects it
        String requestBody = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\":[{\"text\": \"" + prompt + "\"}]\n" +
                "  }]\n" +
                "}";

        try {
            // 3. Make the POST request to the AI
            String responseStr = webClient.post()
                    .uri(uriBuilder -> uriBuilder.queryParam("key", geminiApiKey).build())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 4. Parse the JSON response to extract just the text paragraph
            JsonNode rootNode = objectMapper.readTree(responseStr);
            return rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

        } catch (Exception e) {
            log.error("Gemini AI Error: {}", e.getMessage());
            return "Flight " + callsign + " is currently en route. Live telemetry indicates it is maintaining an altitude of " + altitude + " feet. Radar contact is stable.";
        }
    }

}
