USE vetautet;

CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT,
    `type` ENUM('BOOKING_CONFIRMED', 'BOOKING_CANCELLED', 'BOOKING_EXPIRED', 'BOOKING_FAILED', 'PAYMENT_SUCCESS', 'PAYMENT_FAILED', 'SYSTEM') NOT NULL,
    `reference_id` BIGINT NOT NULL COMMENT 'ID tham chieu, thuong la bookingId',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_notifications_user_id` (`user_id`),
    INDEX `idx_notifications_is_read` (`user_id`, `is_read`),
    INDEX `idx_notifications_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `bookings` (`id`, `order_number`, `storage_month`, `user_id`, `original_price`, `discount_amount`, `total_price`, `status`, `expired_at`, `created_at`, `updated_at`)
SELECT 1, 'ORD-20260501-000001', '202605', 3, 1000000, 0, 1000000, 'CONFIRMED', '2026-05-01 07:00:00', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `bookings` WHERE `id` = 1);

INSERT INTO `booking_details` (`booking_id`, `ticket_id`, `passenger_name`, `passenger_id_card`, `passenger_type`)
SELECT 1, 1, 'Nguyen Van Khach', '079000000001', 'ADULT'
WHERE NOT EXISTS (SELECT 1 FROM `booking_details` WHERE `booking_id` = 1 AND `ticket_id` = 1);

INSERT INTO `booking_details` (`booking_id`, `ticket_id`, `passenger_name`, `passenger_id_card`, `passenger_type`)
SELECT 1, 2, 'Nguyen Van Khach', '079000000001', 'ADULT'
WHERE NOT EXISTS (SELECT 1 FROM `booking_details` WHERE `booking_id` = 1 AND `ticket_id` = 2);

UPDATE `tickets`
SET `status` = 'BOOKED', `hold_expired_at` = NULL
WHERE `id` IN (1, 2);

INSERT INTO `payments` (`id`, `booking_id`, `method`, `amount`, `status`, `transaction_id`, `paid_at`, `created_at`)
SELECT 1, 1, 'VNPAY', 1000000, 'SUCCESS', 'DEMO-VNPAY-0001', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `payments` WHERE `id` = 1);

INSERT INTO `notifications` (`id`, `user_id`, `title`, `content`, `type`, `reference_id`, `is_read`, `created_at`)
SELECT 1, 3, 'Dat ve thanh cong #1',
       'Don hang #1 da duoc thanh toan bang VNPAY. Ghe A1, A2 da duoc dat thanh cong.',
       'BOOKING_CONFIRMED', 1, 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `notifications` WHERE `id` = 1);
