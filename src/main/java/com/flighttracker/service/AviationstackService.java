package com.flighttracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.flighttracker.dto.AviationstackResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AviationstackService {
	
	private final WebClient webClient;
	private final String accessKey;
	
	public AviationstackService(WebClient.Builder webClientBuilder,
			@Value("${aviationstack.api.url}") String apiUrl,
			@Value("${aviationstack.api.key}") String accessKey) {
		
		this.webClient = webClientBuilder.baseUrl(apiUrl).build();
        this.accessKey = accessKey;
		
	}
	
public AviationstackResponse.FlightData getRouteDetails(String flightIata) {
        
        if (flightIata == null || flightIata.trim().isEmpty()) {
            return null;
        }

        try {
            log.info("Requesting route details for flight: {}", flightIata.trim());

            AviationstackResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("access_key", accessKey)
                            .queryParam("flight_iata", flightIata.trim())
                            .build())
                    .retrieve()
                    .bodyToMono(AviationstackResponse.class)
                    .block();

            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                log.info("Successfully found route for {}", flightIata.trim());
                return response.getData().get(0);
            } else {
                log.warn("Aviationstack found no data for flight {}", flightIata.trim());
            }

        } catch (Exception e) {
            log.error("Failed to connect to Aviationstack for flight {}. Error: {}", flightIata, e.getMessage());
        }

        return null;
    }

}
