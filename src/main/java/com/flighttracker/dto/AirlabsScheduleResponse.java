package com.flighttracker.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirlabsScheduleResponse {
	
	private List<AirlabsSchedule> response;

}
