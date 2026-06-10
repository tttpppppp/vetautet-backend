package com.vetautet.domain.repository;

import com.vetautet.domain.model.PassengerFareRule;

import java.util.List;
import java.util.Optional;

public interface PassengerFareRuleRepository {
    List<PassengerFareRule> findAll();
    List<PassengerFareRule> findActive();
    Optional<PassengerFareRule> findByPassengerType(String passengerType);
    Optional<PassengerFareRule> findActiveByPassengerType(String passengerType);
    List<PassengerFareRule> saveAll(List<PassengerFareRule> rules);
}
