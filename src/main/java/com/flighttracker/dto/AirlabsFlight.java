package com.flighttracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class AirlabsFlight {
	
	@JsonAlias("flight_icao") // AirLabs name (Incoming)
    private String callsign;  // Output to React as "callsign"

    @JsonAlias("lat")         
    private Double latitude;     

    @JsonAlias("lng")
    private Double longitude;

    @JsonAlias("alt")
    private Double altitude;

    @JsonAlias("speed")
    private Double velocity;
    
    @JsonAlias("dir")
    private Double trueTrack;

}
