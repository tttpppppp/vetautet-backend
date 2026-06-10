package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassengerFareRuleUpsertRequest {
    private List<PassengerFareRuleRequest> rules;
}
