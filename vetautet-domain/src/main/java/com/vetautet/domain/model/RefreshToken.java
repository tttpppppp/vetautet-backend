package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    private Long id;
    private Long userId;
    private String token;
    private LocalDateTime expiredAt;
    private boolean revoked;
    private LocalDateTime createdAt;
}
