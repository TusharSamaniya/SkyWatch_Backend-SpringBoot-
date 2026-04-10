package com.flighttracker.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.flighttracker.dto.AirlabsResponse;

import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    @Scheduled(fixedRate = 60000) // AirLabs limit is generous, but 60s is safe
    public void fetchAirlabsData() {
        try {
            log.info("Fetching live flight data over Northern India from AirLabs...");
            
            AirlabsResponse data = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("api_key", apiKey)
                    .queryParam("bbox", "20.0,70.0,35.0,85.0") // Bounding box for India
                    .build())
                .retrieve()
                .bodyToMono(AirlabsResponse.class)
                .block();

            this.currentData = data;
            
            if(data != null && data.getResponse() != null) {
                log.info("Airlabs updated with {} flights", data.getResponse().size());
            }

        } catch (Exception e) {
            log.error("Airlabs Error: {}", e.getMessage());
        }
    }

    // Your controller will call this method!
    public AirlabsResponse getLiveFlights() {
        return this.currentData;
    }
}