USE vetautet;

ALTER TABLE `bookings`
    ADD COLUMN IF NOT EXISTS `trip_type` ENUM('ONE_WAY', 'ROUND_TRIP') NOT NULL DEFAULT 'ONE_WAY'
    AFTER `storage_month`;

ALTER TABLE `booking_details`
    ADD COLUMN IF NOT EXISTS `direction` ENUM('OUTBOUND', 'RETURN') NOT NULL DEFAULT 'OUTBOUND'
    AFTER `ticket_id`;

CREATE INDEX IF NOT EXISTS `idx_booking_details_direction`
    ON `booking_details` (`booking_id`, `direction`);
