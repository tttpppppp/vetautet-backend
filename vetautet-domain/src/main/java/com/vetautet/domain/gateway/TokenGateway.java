package com.vetautet.domain.gateway;

public interface TokenGateway {
    String generateAccessToken(String email, Long userId);
    String generateRefreshToken(String email, Long userId);
    String extractUsername(String token);
    boolean isTokenValid(String token, String email);
}
