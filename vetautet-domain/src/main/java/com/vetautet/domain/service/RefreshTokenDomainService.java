package com.vetautet.domain.service;

import com.vetautet.domain.model.RefreshToken;

public interface RefreshTokenDomainService {
    RefreshToken getByToken(String token);
    RefreshToken save(RefreshToken refreshToken);
    void revokeAllByUserId(Long userId);
}
