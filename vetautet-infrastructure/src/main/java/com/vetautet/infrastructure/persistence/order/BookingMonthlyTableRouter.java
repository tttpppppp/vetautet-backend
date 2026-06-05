package com.vetautet.infrastructure.persistence.order;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class BookingMonthlyTableRouter {

    private static final DateTimeFormatter STORAGE_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String BOOKING_TABLE_PREFIX = "bookings_";

    public String resolveTableByStorageMonth(String storageMonth) {
        if (storageMonth == null || !storageMonth.matches("\\d{6}")) {
            throw new IllegalArgumentException("storageMonth must match yyyyMM");
        }
        return BOOKING_TABLE_PREFIX + storageMonth;
    }

    public String resolveCurrentStorageMonth() {
        return YearMonth.now().format(STORAGE_MONTH_FORMAT);
    }

    public String resolveCurrentTable() {
        return resolveTableByStorageMonth(resolveCurrentStorageMonth());
    }
}
