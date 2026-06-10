USE vetautet;

CREATE TABLE IF NOT EXISTS `promotion_passenger_rules` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `promotion_id` BIGINT NOT NULL,
    `passenger_type` VARCHAR(30) NOT NULL COMMENT 'ADULT, CHILD, SENIOR, STUDENT',
    `label` VARCHAR(100) NOT NULL,
    `min_age` INT DEFAULT NULL,
    `max_age` INT DEFAULT NULL,
    `discount_type` VARCHAR(30) NOT NULL DEFAULT 'percent',
    `discount_value` DECIMAL(15,2) NOT NULL DEFAULT 0,
    `max_discount_amount` DECIMAL(15,2) DEFAULT NULL,
    `verification_required` BOOLEAN NOT NULL DEFAULT FALSE,
    `description` TEXT,
    `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_promotion_passenger_rules_promotion`
        FOREIGN KEY (`promotion_id`) REFERENCES `promotions` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_promotion_passenger_type` (`promotion_id`, `passenger_type`),
    INDEX `idx_promotion_passenger_rules_type` (`passenger_type`),
    INDEX `idx_promotion_passenger_rules_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO `promotions`
(`title`, `description`, `code`, `discount_type`, `discount_value`, `max_discount_amount`, `min_order_amount`, `starts_at`, `ends_at`, `conditions`, `route`, `categories`, `usage_limit`, `used_count`, `ease_score`, `status`)
VALUES
('Ưu đãi theo đối tượng', 'Giảm giá theo loại hành khách: trẻ em, người cao tuổi và sinh viên.', 'DOITUONG', 'percent', 0, NULL, 0, '2026-01-01', '2027-12-31', 'Áp dụng theo thông tin hành khách hợp lệ khi đặt vé.', NULL, 'age,student,senior,child', NULL, 0, 92, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    conditions = VALUES(conditions),
    categories = VALUES(categories),
    ease_score = VALUES(ease_score),
    status = VALUES(status),
    deleted_at = NULL;

INSERT INTO `promotion_passenger_rules`
(`promotion_id`, `passenger_type`, `label`, `min_age`, `max_age`, `discount_type`, `discount_value`, `verification_required`, `description`, `status`)
SELECT p.id, 'ADULT', 'Người lớn', 10, 59, 'percent', 0, FALSE, 'Từ 10 - 59 tuổi.', 'ACTIVE'
FROM `promotions` p WHERE p.code = 'DOITUONG'
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    min_age = VALUES(min_age),
    max_age = VALUES(max_age),
    discount_type = VALUES(discount_type),
    discount_value = VALUES(discount_value),
    verification_required = VALUES(verification_required),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO `promotion_passenger_rules`
(`promotion_id`, `passenger_type`, `label`, `min_age`, `max_age`, `discount_type`, `discount_value`, `verification_required`, `description`, `status`)
SELECT p.id, 'CHILD', 'Trẻ em', 6, 9, 'percent', 25, FALSE, 'Trẻ em từ 6 - 9 tuổi được giảm 25%. Trẻ dưới 6 tuổi đi kèm người lớn theo quy định.', 'ACTIVE'
FROM `promotions` p WHERE p.code = 'DOITUONG'
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    min_age = VALUES(min_age),
    max_age = VALUES(max_age),
    discount_type = VALUES(discount_type),
    discount_value = VALUES(discount_value),
    verification_required = VALUES(verification_required),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO `promotion_passenger_rules`
(`promotion_id`, `passenger_type`, `label`, `min_age`, `max_age`, `discount_type`, `discount_value`, `verification_required`, `description`, `status`)
SELECT p.id, 'SENIOR', 'Người cao tuổi', 60, NULL, 'percent', 15, TRUE, 'Công dân Việt Nam từ 60 tuổi được giảm 15%.', 'ACTIVE'
FROM `promotions` p WHERE p.code = 'DOITUONG'
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    min_age = VALUES(min_age),
    max_age = VALUES(max_age),
    discount_type = VALUES(discount_type),
    discount_value = VALUES(discount_value),
    verification_required = VALUES(verification_required),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO `promotion_passenger_rules`
(`promotion_id`, `passenger_type`, `label`, `min_age`, `max_age`, `discount_type`, `discount_value`, `verification_required`, `description`, `status`)
SELECT p.id, 'STUDENT', 'Sinh viên', NULL, NULL, 'percent', 10, TRUE, 'Sinh viên có thẻ sinh viên hợp lệ được giảm 10%.', 'ACTIVE'
FROM `promotions` p WHERE p.code = 'DOITUONG'
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    min_age = VALUES(min_age),
    max_age = VALUES(max_age),
    discount_type = VALUES(discount_type),
    discount_value = VALUES(discount_value),
    verification_required = VALUES(verification_required),
    description = VALUES(description),
    status = VALUES(status);
