package com.vetautet.domain.service.impl;

import com.vetautet.domain.model.RefreshToken;
import com.vetautet.domain.repository.RefreshTokenRepository;
import com.vetautet.domain.service.RefreshTokenDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenDomainServiceImpl implements RefreshTokenDomainService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Override
    public RefreshToken getByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token khong ton tai"));
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
