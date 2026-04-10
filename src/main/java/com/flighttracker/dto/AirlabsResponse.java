package com.flighttracker.dto;

import java.util.List;
import lombok.Data;

@Data
public class AirlabsResponse {
    private List<AirlabsFlight> response;
}

