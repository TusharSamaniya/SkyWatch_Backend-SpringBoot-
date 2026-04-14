package com.flighttracker.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flighttracker.dto.AirlabsFlight;
import com.flighttracker.dto.AirlabsResponse;
import com.flighttracker.service.AirlabsService;
import com.flighttracker.service.GeminiService;

import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/flights")
@CrossOrigin(origins = "*")
public class FlightController {
	
	@Autowired
    private GeminiService geminiService;
	
	@Autowired
    private AirlabsService airlabsService; 

	@GetMapping("/live")
    public List<AirlabsFlight> getLiveFlights(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String state) {
        
        AirlabsResponse response = airlabsService.getLiveFlights(country, state);
        
        if (response != null && response.getResponse() != null) {
            return response.getResponse(); 
        }
        return Collections.emptyList();
    }
	@GetMapping("/{callsign}")
    public Map<String, Object> getSpecificFlight(@PathVariable String callsign) {
        Map<String, Object> combinedData = new HashMap<>();

        AirlabsFlight radarData = airlabsService.getFlightByCallsign(callsign);
        combinedData.put("radar", radarData);

        com.flighttracker.dto.AirlabsSchedule scheduleData = airlabsService.getFlightSchedule(callsign);
        combinedData.put("schedule", scheduleData);

        // NEW: Fetch the photo using the registration number we got from the radar data
        if (radarData != null && radarData.getRegistration() != null) {
            String photoUrl = airlabsService.getAircraftPhoto(radarData.getRegistration());
            combinedData.put("photo", photoUrl);
        }

        return combinedData; 
    }
	
	@GetMapping("/{callsign}/story")
    public Map<String, String> getFlightStory(
            @PathVariable String callsign,
            @RequestParam(defaultValue = "Unknown") String dep,
            @RequestParam(defaultValue = "Unknown") String arr,
            @RequestParam(defaultValue = "Commercial") String aircraft,
            @RequestParam(defaultValue = "0") int alt) {
        
        // Ask the Gemini Service for the story
        String story = geminiService.generateFlightStory(callsign, dep, arr, aircraft, alt);
        
        // Return it as a neat JSON object to React
        Map<String, String> response = new HashMap<>();
        response.put("story", story);
        return response;
    }
	
	
	// NEW: Endpoint to get Arrivals and Departures when an airport is clicked
    @GetMapping("/airports/{iata}/schedules")
    public Map<String, List<JsonNode>> getAirportSchedules(@PathVariable String iata) {
        return airlabsService.getAirportSchedules(iata);
    }
}