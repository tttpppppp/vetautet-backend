package com.vetautet.application.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String phone;
    private String address;
    private String nationality;
    private String imageUrl;
}
