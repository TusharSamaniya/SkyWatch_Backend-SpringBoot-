package com.flighttracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.flighttracker.dto.OpenSkyResponse;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OpenSkyService {

    private WebClient webClient;
    private OpenSkyResponse currentFlightData;

    public OpenSkyService(WebClient.Builder webClientBuilder,
                          @Value("${opensky.api.url}") String apiUrl,
                          @Value("${opensky.api.username}") String username,
                          @Value("${opensky.api.password}") String password) {

        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .defaultHeaders(headers -> headers.setBasicAuth(username, password))
                .build();
    }

    @PostConstruct
    @Scheduled(fixedRate = 10000)
    public void fetchLiveFlightFromAPI() {
        try {
            log.info("Fetching live flight data from OpenSky...");

            OpenSkyResponse newData = webClient.get()
                    .retrieve()
                    .bodyToMono(OpenSkyResponse.class)
                    .block();

            this.currentFlightData = newData;

            log.info("Successfully updated map with {} flights.",
                    newData.getStates() != null ? newData.getStates().size() : 0);

        } catch (Exception e) {
            log.error("Failed to fetch from OpenSky. Keeping last data. Error: {}", e.getMessage());
        }
    }

    public OpenSkyResponse getLiveFlights() {
        return this.currentFlightData;
    }
}
