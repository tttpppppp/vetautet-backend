ALTER TABLE `notifications`
    MODIFY `type` ENUM(
        'BOOKING_CONFIRMED',
        'BOOKING_CANCELLED',
        'BOOKING_EXPIRED',
        'BOOKING_FAILED',
        'PAYMENT_SUCCESS',
        'PAYMENT_FAILED',
        'SYSTEM'
    ) NOT NULL;
