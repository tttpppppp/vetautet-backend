package com.vetautet.application.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UserUpdateRequest {
    private String name;
    private String phone;
    private Set<String> roles;
}
