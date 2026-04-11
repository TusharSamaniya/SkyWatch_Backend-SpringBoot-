package com.flighttracker.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.flighttracker.dto.AirlabsFlight;
import com.flighttracker.dto.AirlabsResponse;
import com.flighttracker.dto.AirlabsSchedule;
import com.flighttracker.dto.AirlabsScheduleResponse;

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
            log.info("Fetching live flight data over India from AirLabs...");
            
            AirlabsResponse data = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("api_key", apiKey)
                    .queryParam("bbox", "6.0,68.0,36.0,98.0") // Bounding box for India
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
    
    public AirlabsFlight getFlightByCallsign(String callsign) {
        if (this.currentData == null || this.currentData.getResponse() == null) {
            return null;
        }

        // Use Java Streams to filter the list and find the matching callsign
        return this.currentData.getResponse().stream()
                .filter(flight -> flight.getCallsign() != null && flight.getCallsign().equalsIgnoreCase(callsign))
                .findFirst()
                .orElse(null); // Return null if the plane isn't in the current list
    }
    
 // NEW: Fetches the schedule for a specific flight
    public AirlabsSchedule getFlightSchedule(String flightIcao) {
        try {
            log.info("Fetching live schedule for {}...", flightIcao);
            
            // Notice we use absolute URL to point to /schedules instead of /flights
            String url = "https://airlabs.co/api/v9/schedules?api_key=" + apiKey + "&flight_icao=" + flightIcao;
            
            AirlabsScheduleResponse scheduleData = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(AirlabsScheduleResponse.class)
                .block();

            // AirLabs returns an array. We just want the first matching flight leg.
            if (scheduleData != null && scheduleData.getResponse() != null && !scheduleData.getResponse().isEmpty()) {
                return scheduleData.getResponse().get(0); 
            }
        } catch (Exception e) {
            log.error("Failed to fetch schedule for {}. Error: {}", flightIcao, e.getMessage());
        }
        return null;
    }
    
 // NEW: Fetches the real aircraft photo using the registration number
    public String getAircraftPhoto(String registration) {
        if (registration == null || registration.isEmpty()) return null;
        
        try {
            log.info("Fetching photo for aircraft {}...", registration);
            String url = "https://api.planespotters.net/pub/photos/reg/" + registration;
            
            // We use a new temporary WebClient here because this API doesn't need basic auth/keys
            com.flighttracker.dto.PlanespottersResponse photoData = WebClient.create().get()
                .uri(url)
                .retrieve()
                .bodyToMono(com.flighttracker.dto.PlanespottersResponse.class)
                .block();

            if (photoData != null && photoData.getPhotos() != null && !photoData.getPhotos().isEmpty()) {
                // Return the direct URL to the image
                return photoData.getPhotos().get(0).getThumbnail_large().getSrc();
            }
        } catch (Exception e) {
            log.error("No photo found for {}. Error: {}", registration, e.getMessage());
        }
        return null;
    }
}