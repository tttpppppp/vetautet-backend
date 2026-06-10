package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.PassengerFareRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PassengerFareRuleJpaRepository extends JpaRepository<PassengerFareRuleEntity, Long> {
    List<PassengerFareRuleEntity> findAllByOrderBySortOrderAscIdAsc();
    List<PassengerFareRuleEntity> findByStatusOrderBySortOrderAscIdAsc(String status);
    Optional<PassengerFareRuleEntity> findByPassengerType(String passengerType);
    Optional<PassengerFareRuleEntity> findByPassengerTypeAndStatus(String passengerType, String status);
}
