package com.vetautet.application.dto;

import lombok.Data;

@Data
public class TrainRequest {
    private String code;
    private String category;
    private String description;
}
