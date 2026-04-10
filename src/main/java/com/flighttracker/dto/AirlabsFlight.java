package com.flighttracker.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class AirlabsFlight {
	
	@JsonAlias("flight_icao") 
    private String callsign;     

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
