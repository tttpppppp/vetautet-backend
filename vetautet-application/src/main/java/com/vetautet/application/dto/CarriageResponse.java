package com.vetautet.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarriageResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String carriageNumber; // Số hiệu toa (ví dụ: T1, T2)
    private String carriageTypeName; // Tên loại toa (ví dụ: 4-Berth VIP)
    private List<TicketResponse> seats;
}
