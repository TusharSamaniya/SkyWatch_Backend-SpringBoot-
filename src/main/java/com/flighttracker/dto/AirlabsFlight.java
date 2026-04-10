package com.flighttracker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AirlabsFlight {
	
	@JsonProperty("flight_icao") 
    private String callsign;     

    @JsonProperty("lat")         
    private Double latitude;     

    @JsonProperty("lng")
    private Double longitude;

    @JsonProperty("alt")
    private Double altitude;

    @JsonProperty("speed")
    private Double velocity;
    
    @JsonProperty("dir")
    private Double trueTrack;

}
