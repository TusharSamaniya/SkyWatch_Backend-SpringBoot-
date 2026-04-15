package com.flighttracker.service;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.flighttracker.dto.AirlabsFlight;
import com.flighttracker.dto.AirlabsResponse;
import com.flighttracker.dto.AirlabsSchedule;
import com.flighttracker.dto.AirlabsScheduleResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

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
 // THE GEOGRAPHIC DICTIONARY: Translates Dropdown text into invisible Bounding Boxes
    private String getBoundingBox(String country, String state) {
        
        // Safety Check: If state is empty string, treat it as null (Entire Country)
        if (state != null && state.trim().isEmpty()) {
            state = null;
        }

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
    
 // NEW: Fetch all airports in a specific country (Defaulting to India)
    public List<Map<String, Object>> getAirportsByCountry(String countryCode) {
        log.info("Fetching airports for country: {}", countryCode);
        
        try {
            String url = "https://airlabs.co/api/v9/airports?country_code=" + countryCode + "&api_key=" + apiKey;

            // FIX: Using WebClient instead of RestTemplate to clear the red lines!
            JsonNode rootNode = WebClient.create().get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode responseArray = rootNode.path("response");
            List<Map<String, Object>> airports = new ArrayList<>();

            if (responseArray.isArray()) {
                for (JsonNode node : responseArray) {
                    if (node.hasNonNull("iata_code") && node.hasNonNull("lat") && node.hasNonNull("lng")) {
                        Map<String, Object> airport = new HashMap<>();
                        airport.put("name", node.path("name").asText());
                        airport.put("iata", node.path("iata_code").asText());
                        airport.put("lat", node.path("lat").asDouble());
                        airport.put("lng", node.path("lng").asDouble());
                        airports.add(airport);
                    }
                }
            }
            log.info("Successfully loaded {} commercial airports.", airports.size());
            return airports;

        } catch (Exception e) {
            log.error("Failed to fetch airports: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // NEW: Fetch live arrivals and departures for a specific airport
    public Map<String, List<JsonNode>> getAirportSchedules(String iataCode) {
        log.info("Fetching live schedules for airport: {}", iataCode);
        Map<String, List<JsonNode>> schedules = new HashMap<>();
        
        try {
            // 1. Fetch Departures using WebClient
            String depUrl = "https://airlabs.co/api/v9/schedules?dep_iata=" + iataCode + "&api_key=" + apiKey;
            JsonNode depRoot = WebClient.create().get()
                    .uri(depUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            
            List<JsonNode> departures = new ArrayList<>();
            if (depRoot != null && depRoot.path("response").isArray()) {
                depRoot.path("response").forEach(departures::add);
            }
            schedules.put("departures", departures);

            // 2. Fetch Arrivals using WebClient
            String arrUrl = "https://airlabs.co/api/v9/schedules?arr_iata=" + iataCode + "&api_key=" + apiKey;
            JsonNode arrRoot = WebClient.create().get()
                    .uri(arrUrl)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            
            List<JsonNode> arrivals = new ArrayList<>();
            if (arrRoot != null && arrRoot.path("response").isArray()) {
                arrRoot.path("response").forEach(arrivals::add);
            }
            schedules.put("arrivals", arrivals);

            log.info("Loaded {} departures and {} arrivals for {}", departures.size(), arrivals.size(), iataCode);
            return schedules;

        } catch (Exception e) {
            log.error("Failed to fetch schedules for {}: {}", iataCode, e.getMessage());
            schedules.put("departures", Collections.emptyList());
            schedules.put("arrivals", Collections.emptyList());
            return schedules;
        }
    }
    
 // ==========================================
    // NEW: Route Intelligence - Fetch flight paths between airports
    // ==========================================
    public JsonNode getRoutes(String depIata, String arrIata) {
        log.info("Fetching Route Intelligence: {} -> {}", depIata, arrIata);
        
        try {
            // Build the URL dynamically based on what the user searches for
            StringBuilder urlBuilder = new StringBuilder("https://airlabs.co/api/v9/routes?api_key=" + apiKey);
            
            if (depIata != null && !depIata.trim().isEmpty()) {
                urlBuilder.append("&dep_iata=").append(depIata.toUpperCase());
            }
            if (arrIata != null && !arrIata.trim().isEmpty()) {
                urlBuilder.append("&arr_iata=").append(arrIata.toUpperCase());
            }

            // Fetch the routes using WebClient
            return WebClient.create().get()
                    .uri(urlBuilder.toString())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

        } catch (Exception e) {
            log.error("Failed to fetch Route Intelligence: {}", e.getMessage());
            return null;
        }
    }
    
 // ==========================================
    // NEW: Airline Fleet Tracker - Fetch Airlines DB
    // ==========================================
 // ==========================================
    // NEW: Airline Fleet Tracker - Fetch Airlines DB
    // ==========================================
    public List<Map<String, String>> getActiveAirlines() {
        log.info("Fetching global Airlines DB...");
        
        try {
            String url = "https://airlabs.co/api/v9/airlines?api_key=" + apiKey;

            // FIX: Increase the Spring Boot memory buffer to 16MB to handle this massive file
            ExchangeStrategies strategies = ExchangeStrategies.builder()
                    .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) 
                    .build();

            WebClient largeWebClient = WebClient.builder()
                    .exchangeStrategies(strategies)
                    .build();

            // Use the new largeWebClient instead of standard WebClient.create()
            JsonNode rootNode = largeWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            JsonNode responseArray = rootNode.path("response");
            List<Map<String, String>> airlines = new ArrayList<>();

            if (responseArray.isArray()) {
                for (JsonNode node : responseArray) {
                    // Only grab active airlines that have a valid 2-letter IATA code
                    if (node.hasNonNull("iata_code") && !node.path("iata_code").asText().trim().isEmpty() 
                            && node.hasNonNull("name")) {
                        
                        Map<String, String> airline = new HashMap<>();
                        airline.put("iata", node.path("iata_code").asText());
                        airline.put("name", node.path("name").asText());
                        airlines.add(airline);
                    }
                }
            }
            log.info("Successfully loaded {} active commercial airlines.", airlines.size());
            return airlines;

        } catch (Exception e) {
            log.error("Failed to fetch Airlines DB: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}