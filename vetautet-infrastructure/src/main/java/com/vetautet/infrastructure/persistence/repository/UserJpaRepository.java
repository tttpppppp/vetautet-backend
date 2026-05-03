package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(@Param("email") String email);
    boolean existsByEmail(@Param("email") String email);
}
