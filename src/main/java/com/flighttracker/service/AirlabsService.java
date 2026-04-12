package com.flighttracker.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.flighttracker.dto.AirlabsFlight;
import com.flighttracker.dto.AirlabsResponse;
import com.flighttracker.dto.AirlabsSchedule;
import com.flighttracker.dto.AirlabsScheduleResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AirlabsService {

	private final WebClient webClient;
    // We keep this to remember the LAST fetched data so we can still click individual planes!
    private AirlabsResponse currentData; 

    @Value("${airlabs.api.key}")
    private String apiKey;

    public AirlabsService(WebClient.Builder builder, @Value("${airlabs.api.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
    }

    // THE GEOGRAPHIC DICTIONARY: Translates Dropdown text into invisible Bounding Boxes
    private String getBoundingBox(String country, String state) {
        // 1. INDIA (Default)
        if (country == null || country.equalsIgnoreCase("India")) {
            if ("Delhi".equalsIgnoreCase(state)) return "28.40,76.83,28.88,77.34";
            if ("Maharashtra".equalsIgnoreCase(state)) return "15.60,72.66,22.02,80.89";
            if ("Karnataka".equalsIgnoreCase(state)) return "11.58,74.05,18.44,78.58";
            return "6.0,68.0,36.0,98.0"; // Entire India
        }

        // 2. UNITED STATES
        if (country.equalsIgnoreCase("USA")) {
            if ("New York".equalsIgnoreCase(state)) return "40.49,-79.76,45.01,-71.85";
            if ("California".equalsIgnoreCase(state)) return "32.53,-124.41,42.01,-114.13";
            if ("Texas".equalsIgnoreCase(state)) return "25.83,-106.64,36.50,-93.50";
            return "24.39,-125.00,49.38,-66.93"; // Entire USA
        }

        // 3. UNITED KINGDOM
        if (country.equalsIgnoreCase("UK")) {
            if ("England".equalsIgnoreCase(state)) return "49.88,-5.76,55.81,1.77";
            if ("Scotland".equalsIgnoreCase(state)) return "54.63,-7.66,60.85,-0.72";
            return "49.88,-8.20,60.85,1.77"; // Entire UK
        }

        // 4. AUSTRALIA
        if (country.equalsIgnoreCase("Australia")) {
            if ("New South Wales".equalsIgnoreCase(state)) return "-37.50,140.99,-28.15,153.63";
            if ("Victoria".equalsIgnoreCase(state)) return "-39.15,140.96,-33.98,149.97";
            return "-43.63,113.15,-10.66,153.63"; // Entire Australia
        }

        // 5. CANADA
        if (country.equalsIgnoreCase("Canada")) {
            if ("Ontario".equalsIgnoreCase(state)) return "41.67,-95.15,56.86,-74.31";
            if ("British Columbia".equalsIgnoreCase(state)) return "48.30,-139.06,60.00,-114.03";
            return "41.67,-141.00,83.11,-52.62"; // Entire Canada
        }

        // 6. JAPAN
        if (country.equalsIgnoreCase("Japan")) {
            if ("Tokyo".equalsIgnoreCase(state)) return "35.50,138.94,35.89,139.91";
            if ("Osaka".equalsIgnoreCase(state)) return "34.27,135.09,35.05,135.74";
            return "24.24,122.93,45.55,153.98"; // Entire Japan
        }

        // Failsafe: Always return India if something goes wrong
        return "6.0,68.0,36.0,98.0"; 
    }

    // ON-DEMAND FETCHING: Triggered only when the user clicks "Show Flights" in React
    public AirlabsResponse getLiveFlights(String country, String state) {
        try {
            String bbox = getBoundingBox(country, state);
            log.info("React requested flights for {} / {}. Using Bbox: {}", country, state, bbox);
            
            AirlabsResponse data = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("api_key", apiKey)
                    .queryParam("bbox", bbox) 
                    .build())
                .retrieve()
                .bodyToMono(AirlabsResponse.class)
                .block();

            this.currentData = data; 
            
            if(data != null && data.getResponse() != null) {
                log.info("Found {} flights in this region.", data.getResponse().size());
            }
            return data;

        } catch (Exception e) {
            log.error("Airlabs Fetch Error: {}", e.getMessage());
            return null;
        }
    }

    // EXISTING: Gets radar details from memory
    public AirlabsFlight getFlightByCallsign(String callsign) {
        if (this.currentData == null || this.currentData.getResponse() == null) {
            return null;
        }

        // Use Java Streams to filter the list and find the matching callsign
        return this.currentData.getResponse().stream()
                .filter(flight -> flight.getCallsign() != null && flight.getCallsign().equalsIgnoreCase(callsign))
                .findFirst()
                .orElse(null); 
    }
    
    // EXISTING: Fetches the schedule for a specific flight
    public AirlabsSchedule getFlightSchedule(String flightIcao) {
        try {
            log.info("Fetching live schedule for {}...", flightIcao);
            
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
    
    // EXISTING: Fetches the real aircraft photo using the registration number
    public String getAircraftPhoto(String registration) {
        if (registration == null || registration.isEmpty()) return null;
        
        try {
            log.info("Fetching photo for aircraft {}...", registration);
            String url = "https://api.planespotters.net/pub/photos/reg/" + registration;
            
            com.flighttracker.dto.PlanespottersResponse photoData = WebClient.create().get()
                .uri(url)
                .retrieve()
                .bodyToMono(com.flighttracker.dto.PlanespottersResponse.class)
                .block();

            if (photoData != null && photoData.getPhotos() != null && !photoData.getPhotos().isEmpty()) {
                return photoData.getPhotos().get(0).getThumbnail_large().getSrc();
            }
        } catch (Exception e) {
            log.error("No photo found for {}. Error: {}", registration, e.getMessage());
        }
        return null;
    }
}