package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.RefreshToken;
import com.vetautet.domain.repository.RefreshTokenRepository;
import com.vetautet.infrastructure.persistence.mapper.PersistenceMapper;
import com.vetautet.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public class RefreshTokenInfrasRepositoryImpl implements RefreshTokenRepository {

    @Autowired
    private RefreshTokenJpaRepository jpaRepository;
    
    @Autowired
    private PersistenceMapper mapper;

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(refreshToken)));
    }

    @Override
    public @Transactional void revokeAllByUserId(Long userId) {
        jpaRepository.revokeAllByUserId(userId);
    }
}
