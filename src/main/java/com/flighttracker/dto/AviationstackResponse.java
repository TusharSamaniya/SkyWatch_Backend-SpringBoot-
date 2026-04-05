package com.flighttracker.dto;

import java.util.List;

import lombok.Data;

@Data
public class AviationstackResponse {
	
	private List<FlightData> data;
	
	@Data
	public static class FlightData{
		private String flight_date;
        private String flight_status;
        private AirportNode departure;
        private AirportNode arrival;
        private AirlineNode airline;
        private FlightNode flight;
        private AircraftNode aircraft;
	}
	
	@Data
	public static class AirportNode{
		private String airport;
		private String iata;
		private String terminal;
		private String gate;
	}
	
	@Data
	public static class AirlineNode{
		private String name;
		private String iata;
	}
	
	public static class FlightNode{
		private String number;
		private String iata;
	}
	
	@Data
	public static class AircraftNode{
		private String icao24;
	}

}
