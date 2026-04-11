package com.flighttracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirlabsSchedule {
	
	private String dep_iata;
    private String arr_iata;
    private String dep_time;
    private String arr_time;
    private String dep_estimated;
    private String status;

}
