package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String address;
    private String nationality;
    private String imageUrl;
    private Integer rewardPoints;
    private String membershipRank;
    private Boolean isIdentityVerified;
    private Boolean isEmailVerified;
    private Set<Role> roles = new HashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
}
