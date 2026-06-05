USE vetautet;

ALTER TABLE `booking_details`
    ADD COLUMN IF NOT EXISTS `departure_station_id` BIGINT DEFAULT NULL AFTER `ticket_id`,
    ADD COLUMN IF NOT EXISTS `arrival_station_id` BIGINT DEFAULT NULL AFTER `departure_station_id`,
    ADD COLUMN IF NOT EXISTS `segment_ids` VARCHAR(255) DEFAULT NULL AFTER `arrival_station_id`,
    ADD COLUMN IF NOT EXISTS `segment_price` DECIMAL(15,2) DEFAULT NULL AFTER `segment_ids`;

CREATE INDEX IF NOT EXISTS `idx_booking_details_route`
    ON `booking_details` (`departure_station_id`, `arrival_station_id`);
