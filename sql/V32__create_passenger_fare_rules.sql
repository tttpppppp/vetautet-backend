CREATE TABLE IF NOT EXISTS `passenger_fare_rules` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `passenger_type` VARCHAR(30) NOT NULL,
    `label` VARCHAR(100) NOT NULL,
    `min_age` INT DEFAULT NULL,
    `max_age` INT DEFAULT NULL,
    `discount_percent` DECIMAL(5,2) NOT NULL DEFAULT 0,
    `fare_multiplier` DECIMAL(8,4) NOT NULL DEFAULT 1,
    `verification_required` BOOLEAN NOT NULL DEFAULT FALSE,
    `description` TEXT DEFAULT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_passenger_fare_rules_type` (`passenger_type`),
    INDEX `idx_passenger_fare_rules_status` (`status`),
    INDEX `idx_passenger_fare_rules_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `passenger_fare_rules`
(`passenger_type`, `label`, `min_age`, `max_age`, `discount_percent`, `fare_multiplier`, `verification_required`, `description`, `sort_order`, `status`)
VALUES
('ADULT', 'Người lớn', 10, 59, 0, 1.0000, FALSE, 'Người lớn từ 10 - 59 tuổi.', 1, 'ACTIVE'),
('CHILD', 'Trẻ em', 6, 9, 25, 0.7500, FALSE, 'Trẻ em từ 6 - 9 tuổi được giảm 25%.', 2, 'ACTIVE'),
('SENIOR', 'Người cao tuổi', 60, NULL, 15, 0.8500, TRUE, 'Công dân Việt Nam từ 60 tuổi được giảm 15%.', 3, 'ACTIVE'),
('STUDENT', 'Sinh viên', NULL, NULL, 10, 0.9000, TRUE, 'Sinh viên có thẻ sinh viên hợp lệ được giảm 10%.', 4, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    `label` = VALUES(`label`),
    `min_age` = VALUES(`min_age`),
    `max_age` = VALUES(`max_age`),
    `discount_percent` = VALUES(`discount_percent`),
    `fare_multiplier` = VALUES(`fare_multiplier`),
    `verification_required` = VALUES(`verification_required`),
    `description` = VALUES(`description`),
    `sort_order` = VALUES(`sort_order`),
    `status` = VALUES(`status`);
