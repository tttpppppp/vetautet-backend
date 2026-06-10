package com.vetautet.application.service.fare.impl;

import com.vetautet.application.dto.PassengerFareRuleRequest;
import com.vetautet.application.dto.PassengerFareRuleResponse;
import com.vetautet.application.dto.PassengerFareRuleUpsertRequest;
import com.vetautet.application.service.fare.PassengerFareRuleAppService;
import com.vetautet.domain.model.PassengerFareRule;
import com.vetautet.domain.service.PassengerFareRuleDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PassengerFareRuleAppServiceImpl implements PassengerFareRuleAppService {

    private final PassengerFareRuleDomainService passengerFareRuleDomainService;

    public PassengerFareRuleAppServiceImpl(PassengerFareRuleDomainService passengerFareRuleDomainService) {
        this.passengerFareRuleDomainService = passengerFareRuleDomainService;
    }

    @Override
    public List<PassengerFareRuleResponse> getAllRules() {
        return passengerFareRuleDomainService.getAllRules().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PassengerFareRuleResponse> getActiveRules() {
        return passengerFareRuleDomainService.getActiveRules().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<PassengerFareRuleResponse> upsertRules(PassengerFareRuleUpsertRequest request) {
        List<PassengerFareRule> rules = request == null || request.getRules() == null
                ? List.of()
                : request.getRules().stream().map(this::toDomain).collect(Collectors.toList());
        return passengerFareRuleDomainService.upsertRules(rules).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PassengerFareRule toDomain(PassengerFareRuleRequest request) {
        return PassengerFareRule.builder()
                .id(request.getId())
                .passengerType(request.getPassengerType())
                .label(request.getLabel())
                .minAge(request.getMinAge())
                .maxAge(request.getMaxAge())
                .discountPercent(request.getDiscountPercent())
                .fareMultiplier(request.getFareMultiplier())
                .verificationRequired(request.getVerificationRequired())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder())
                .status(request.getStatus())
                .build();
    }

    private PassengerFareRuleResponse toResponse(PassengerFareRule rule) {
        return PassengerFareRuleResponse.builder()
                .id(rule.getId())
                .passengerType(rule.getPassengerType())
                .label(rule.getLabel())
                .minAge(rule.getMinAge())
                .maxAge(rule.getMaxAge())
                .discountPercent(rule.getDiscountPercent())
                .fareMultiplier(rule.getFareMultiplier())
                .discountLabel(discountLabel(rule.getDiscountPercent()))
                .verificationRequired(rule.getVerificationRequired())
                .description(rule.getDescription())
                .sortOrder(rule.getSortOrder())
                .status(rule.getStatus())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    private String discountLabel(BigDecimal discountPercent) {
        BigDecimal value = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        if (value.signum() <= 0) {
            return null;
        }
        return "-" + value.stripTrailingZeros().toPlainString() + "%";
    }
}
