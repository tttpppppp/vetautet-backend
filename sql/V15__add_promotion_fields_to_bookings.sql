USE vetautet;

ALTER TABLE `bookings`
    ADD COLUMN IF NOT EXISTS `original_price` DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER `user_id`,
    ADD COLUMN IF NOT EXISTS `promo_code` VARCHAR(50) DEFAULT NULL AFTER `original_price`,
    ADD COLUMN IF NOT EXISTS `discount_amount` DECIMAL(15,2) NOT NULL DEFAULT 0 AFTER `promo_code`;

UPDATE `bookings`
SET `original_price` = `total_price`
WHERE `original_price` = 0;

ALTER TABLE `bookings`
    ADD INDEX IF NOT EXISTS `idx_bookings_promo_code` (`promo_code`);
