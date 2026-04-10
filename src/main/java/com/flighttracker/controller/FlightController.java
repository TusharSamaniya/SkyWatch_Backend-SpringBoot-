package com.flighttracker.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flighttracker.dto.AirlabsFlight;
import com.flighttracker.dto.AirlabsResponse;
import com.flighttracker.dto.AviationstackResponse;
import com.flighttracker.service.AirlabsService;
import com.flighttracker.service.AviationstackService;

@RestController
@RequestMapping("/api/flights")
public class FlightController {
	
	@Autowired
	private AviationstackService aviationstackService;
	
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
	
	@GetMapping("/route/{flightIata}")
    public AviationstackResponse.FlightData getFlightRoute(@PathVariable String flightIata) {
        return aviationstackService.getRouteDetails(flightIata);
    }
}