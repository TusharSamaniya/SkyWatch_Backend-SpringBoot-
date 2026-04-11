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
    public List<AirlabsFlight> getLiveFlights() {
        AirlabsResponse response = airlabsService.getLiveFlights();
        if (response != null && response.getResponse() != null) {
            return response.getResponse(); 
        }
        return Collections.emptyList();
    }
    
	/* @GetMapping("/{callsign}")
	public AirlabsFlight getSpecificFlight(@PathVariable String callsign) {
	    AirlabsFlight flight = airlabsService.getFlightByCallsign(callsign);
	    if (flight == null) {
	        // You could also throw a 404 error here, but returning null is fine for now
	        return null; 
	    }
	    return flight;
	}
	
	@GetMapping("/route/{flightIata}")
	public AviationstackResponse.FlightData getFlightRoute(@PathVariable String flightIata) {
	    return aviationstackService.getRouteDetails(flightIata);
	}*/
	
	/*@GetMapping("/{callsign}")
	public Map<String, Object> getSpecificFlight(@PathVariable String callsign) {
	    Map<String, Object> combinedData = new HashMap<>();
	
	    // 1. Get the live radar telemetry from memory
	    AirlabsFlight radarData = airlabsService.getFlightByCallsign(callsign);
	    combinedData.put("radar", radarData);
	
	    // 2. Fetch the live timetable from AirLabs
	    com.flighttracker.dto.AirlabsSchedule scheduleData = airlabsService.getFlightSchedule(callsign);
	    combinedData.put("schedule", scheduleData);
	
	    return combinedData; // Spring Boot automatically converts this Map into a beautiful JSON object!
	}
	*/
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