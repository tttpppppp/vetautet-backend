package com.vetautet.application.service.fare;

import com.vetautet.application.dto.PassengerFareRuleResponse;
import com.vetautet.application.dto.PassengerFareRuleUpsertRequest;

import java.util.List;

public interface PassengerFareRuleAppService {
    List<PassengerFareRuleResponse> getAllRules();
    List<PassengerFareRuleResponse> getActiveRules();
    List<PassengerFareRuleResponse> upsertRules(PassengerFareRuleUpsertRequest request);
}
