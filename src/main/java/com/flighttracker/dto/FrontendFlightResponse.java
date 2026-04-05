package com.flighttracker.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FrontendFlightResponse {
	
	//live data from Opensky
	private String icao24;
	private String callsign;
	private double latitude;
	private double longitude;
	private double altitude;
	private double velocity;
	private double trueTrack;
	
	//route data from Aviationstack
	private String airlineName;
	private String flightNumber;
	private String originAirport;
	private String originIata;
	private String destinationAirport;
	private String destinationIata;
	private String status;

}
