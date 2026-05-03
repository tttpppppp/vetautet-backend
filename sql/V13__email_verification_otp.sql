USE vetautet;

ALTER TABLE `users`
    ADD COLUMN IF NOT EXISTS `is_email_verified` TINYINT(1) DEFAULT 0 AFTER `is_identity_verified`;

UPDATE `users`
SET `is_email_verified` = 1
WHERE `id` IN (1, 2, 3);

CREATE TABLE IF NOT EXISTS `email_verification_otps` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `otp` VARCHAR(6) NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `used` TINYINT(1) NOT NULL DEFAULT 0,
    `used_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_email_verification_otps_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_email_verification_otps_email_used` (`email`, `used`),
    INDEX `idx_email_verification_otps_user_used` (`user_id`, `used`),
    INDEX `idx_email_verification_otps_expires_at` (`expires_at`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4;
