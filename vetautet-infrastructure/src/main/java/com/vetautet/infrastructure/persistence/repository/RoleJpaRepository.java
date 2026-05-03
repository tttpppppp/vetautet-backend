package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByCode(String code);
}
