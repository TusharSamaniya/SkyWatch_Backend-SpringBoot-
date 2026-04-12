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

@RestController
@RequestMapping("/api/flights")
@CrossOrigin(origins = "*")
public class FlightController {
	
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
}