package com.vetautet.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {
    private String subject;
    private String email;
    private String name;
    private String pictureUrl;
    private Boolean emailVerified;
}
