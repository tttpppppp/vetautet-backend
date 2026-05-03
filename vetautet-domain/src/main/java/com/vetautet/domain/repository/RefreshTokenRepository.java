package com.vetautet.domain.repository;

import com.vetautet.domain.model.RefreshToken;
import java.util.Optional;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByToken(String token);
    RefreshToken save(RefreshToken refreshToken);
    void revokeAllByUserId(Long userId);
}
