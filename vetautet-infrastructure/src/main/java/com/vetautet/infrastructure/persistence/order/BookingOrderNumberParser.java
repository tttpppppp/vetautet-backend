package com.vetautet.infrastructure.persistence.order;

import org.springframework.stereotype.Component;

@Component
public class BookingOrderNumberParser {

    public String extractStorageMonth(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("orderNumber must not be blank");
        }

        if (orderNumber.startsWith("ORD-") && orderNumber.length() >= 12) {
            String yyyymmdd = orderNumber.substring(4, 12);
            if (yyyymmdd.matches("\\d{8}")) {
                return yyyymmdd.substring(0, 6);
            }
        }

        if (orderNumber.matches("\\d{12,}")) {
            String yymmdd = orderNumber.substring(0, 6);
            return "20" + yymmdd.substring(0, 2) + yymmdd.substring(2, 4);
        }

        throw new IllegalArgumentException("Unsupported order number format: " + orderNumber);
    }
}
