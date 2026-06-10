package com.vetautet.domain.service;

import com.vetautet.domain.model.PassengerFareRule;

import java.util.List;
import java.util.Optional;

public interface PassengerFareRuleDomainService {
    List<PassengerFareRule> getAllRules();
    List<PassengerFareRule> getActiveRules();
    Optional<PassengerFareRule> getActiveRule(String passengerType);
    List<PassengerFareRule> upsertRules(List<PassengerFareRule> rules);
}
