package com.vetautet.application.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketUpdateRequest {
    private BigDecimal price;
    private String status;
}
