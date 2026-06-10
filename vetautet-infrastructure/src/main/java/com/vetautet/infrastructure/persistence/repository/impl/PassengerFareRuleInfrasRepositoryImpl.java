package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.PassengerFareRule;
import com.vetautet.domain.repository.PassengerFareRuleRepository;
import com.vetautet.infrastructure.persistence.entity.PassengerFareRuleEntity;
import com.vetautet.infrastructure.persistence.repository.PassengerFareRuleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class PassengerFareRuleInfrasRepositoryImpl implements PassengerFareRuleRepository {

    private final PassengerFareRuleJpaRepository passengerFareRuleJpaRepository;

    public PassengerFareRuleInfrasRepositoryImpl(PassengerFareRuleJpaRepository passengerFareRuleJpaRepository) {
        this.passengerFareRuleJpaRepository = passengerFareRuleJpaRepository;
    }

    @Override
    public List<PassengerFareRule> findAll() {
        return passengerFareRuleJpaRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PassengerFareRule> findActive() {
        return passengerFareRuleJpaRepository.findByStatusOrderBySortOrderAscIdAsc("ACTIVE").stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<PassengerFareRule> findByPassengerType(String passengerType) {
        return passengerFareRuleJpaRepository.findByPassengerType(normalizePassengerType(passengerType))
                .map(this::toDomain);
    }

    @Override
    public Optional<PassengerFareRule> findActiveByPassengerType(String passengerType) {
        return passengerFareRuleJpaRepository.findByPassengerTypeAndStatus(normalizePassengerType(passengerType), "ACTIVE")
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public List<PassengerFareRule> saveAll(List<PassengerFareRule> rules) {
        List<PassengerFareRuleEntity> entities = rules.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        return passengerFareRuleJpaRepository.saveAll(entities).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private PassengerFareRuleEntity toEntity(PassengerFareRule rule) {
        PassengerFareRuleEntity entity = passengerFareRuleJpaRepository
                .findByPassengerType(normalizePassengerType(rule.getPassengerType()))
                .orElseGet(PassengerFareRuleEntity::new);
        entity.setPassengerType(normalizePassengerType(rule.getPassengerType()));
        entity.setLabel(rule.getLabel());
        entity.setMinAge(rule.getMinAge());
        entity.setMaxAge(rule.getMaxAge());
        entity.setDiscountPercent(rule.getDiscountPercent());
        entity.setFareMultiplier(rule.getFareMultiplier());
        entity.setVerificationRequired(rule.getVerificationRequired());
        entity.setDescription(rule.getDescription());
        entity.setSortOrder(rule.getSortOrder());
        entity.setStatus(normalizeStatus(rule.getStatus()));
        return entity;
    }

    private PassengerFareRule toDomain(PassengerFareRuleEntity entity) {
        return PassengerFareRule.builder()
                .id(entity.getId())
                .passengerType(entity.getPassengerType())
                .label(entity.getLabel())
                .minAge(entity.getMinAge())
                .maxAge(entity.getMaxAge())
                .discountPercent(entity.getDiscountPercent())
                .fareMultiplier(entity.getFareMultiplier())
                .verificationRequired(entity.getVerificationRequired())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String normalizePassengerType(String passengerType) {
        return passengerType == null ? null : passengerType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase(Locale.ROOT);
    }
}
