package com.flighttracker.dto;

import java.util.List;

import lombok.Data;

@Data
public class OpenSkyResponse {
	
	private int time;
	
	private List<List<Object>> states;

}
