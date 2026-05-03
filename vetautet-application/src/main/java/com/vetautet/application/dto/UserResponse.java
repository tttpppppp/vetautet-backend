package com.vetautet.application.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String imageUrl;
    private String nationality;
    private Integer rewardPoints;
    private String membershipRank;
    private Boolean isIdentityVerified;
    private Boolean isEmailVerified;
    private Integer tripsCount;
    private java.time.LocalDateTime createdAt;
    private Set<String> roles;
}
