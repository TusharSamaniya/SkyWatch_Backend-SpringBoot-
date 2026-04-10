package com.flighttracker.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.flighttracker.dto.AirlabsResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AirlabsService {

    private final WebClient webClient;
    private AirlabsResponse currentData;

    @Value("${airlabs.api.key}")
    private String apiKey;

    public AirlabsService(WebClient.Builder builder, @Value("${airlabs.api.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
    }

    @Scheduled(fixedRate = 60000) // AirLabs free tier is strict, use 60s
    public void fetchAirlabsData() {
        try {
            AirlabsResponse data = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("api_key", apiKey)
                    // Bounding box for North India
                    .queryParam("bbox", "20.0,70.0,35.0,85.0") 
                    .build())
                .retrieve()
                .bodyToMono(AirlabsResponse.class)
                .block();

            this.currentData = data;
            log.info("Airlabs updated with {} flights", data.getResponse().size());
        } catch (Exception e) {
            log.error("Airlabs Error: {}", e.getMessage());
        }
    }
}