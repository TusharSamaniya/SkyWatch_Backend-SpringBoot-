package com.flighttracker.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirlabsResponse {
    private List<AirlabsFlight> response;
}

