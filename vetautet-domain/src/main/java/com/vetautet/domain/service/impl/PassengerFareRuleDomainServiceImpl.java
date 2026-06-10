package com.vetautet.domain.service.impl;

import com.vetautet.domain.model.PassengerFareRule;
import com.vetautet.domain.repository.PassengerFareRuleRepository;
import com.vetautet.domain.service.PassengerFareRuleDomainService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PassengerFareRuleDomainServiceImpl implements PassengerFareRuleDomainService {

    private final PassengerFareRuleRepository passengerFareRuleRepository;

    public PassengerFareRuleDomainServiceImpl(PassengerFareRuleRepository passengerFareRuleRepository) {
        this.passengerFareRuleRepository = passengerFareRuleRepository;
    }

    @Override
    public List<PassengerFareRule> getAllRules() {
        return passengerFareRuleRepository.findAll();
    }

    @Override
    public List<PassengerFareRule> getActiveRules() {
        return passengerFareRuleRepository.findActive();
    }

    @Override
    public Optional<PassengerFareRule> getActiveRule(String passengerType) {
        String normalizedType = normalizePassengerType(passengerType);
        return normalizedType == null ? Optional.empty() : passengerFareRuleRepository.findActiveByPassengerType(normalizedType);
    }

    @Override
    public List<PassengerFareRule> upsertRules(List<PassengerFareRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new RuntimeException("Passenger fare rules are required");
        }
        List<PassengerFareRule> normalized = rules.stream()
                .map(this::normalizeRule)
                .sorted(Comparator.comparing(PassengerFareRule::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        return passengerFareRuleRepository.saveAll(normalized);
    }

    private PassengerFareRule normalizeRule(PassengerFareRule rule) {
        String passengerType = normalizePassengerType(rule.getPassengerType());
        if (passengerType == null) {
            throw new RuntimeException("Passenger type is required");
        }

        BigDecimal discountPercent = rule.getDiscountPercent() == null
                ? BigDecimal.ZERO
                : rule.getDiscountPercent().max(BigDecimal.ZERO);
        BigDecimal fareMultiplier = rule.getFareMultiplier();
        if (fareMultiplier == null) {
            fareMultiplier = BigDecimal.ONE.subtract(discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        }
        if (fareMultiplier.signum() < 0) {
            throw new RuntimeException("Fare multiplier must be zero or positive");
        }

        return PassengerFareRule.builder()
                .id(rule.getId())
                .passengerType(passengerType)
                .label(hasText(rule.getLabel()) ? rule.getLabel().trim() : passengerType)
                .minAge(rule.getMinAge())
                .maxAge(rule.getMaxAge())
                .discountPercent(discountPercent)
                .fareMultiplier(fareMultiplier)
                .verificationRequired(Boolean.TRUE.equals(rule.getVerificationRequired()))
                .description(rule.getDescription())
                .sortOrder(rule.getSortOrder() == null ? 0 : rule.getSortOrder())
                .status(hasText(rule.getStatus()) ? rule.getStatus().trim().toUpperCase(Locale.ROOT) : "ACTIVE")
                .build();
    }

    private String normalizePassengerType(String passengerType) {
        return hasText(passengerType) ? passengerType.trim().toUpperCase(Locale.ROOT) : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
