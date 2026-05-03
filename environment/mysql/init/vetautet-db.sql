CREATE DATABASE IF NOT EXISTS vetautet
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE vetautet;

-- =============================================================================
-- PART 0: RBAC (Role-Based Access Control)
-- =============================================================================

-- 1. Bảng Quyền hạn (Permissions) - Các hành động cụ thể
CREATE TABLE IF NOT EXISTS `permissions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(100) NOT NULL UNIQUE COMMENT 'VD: TICKET_CREATE, USER_DELETE, TRIP_MANAGE',
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- 2. Bảng Vai trò (Roles)
CREATE TABLE IF NOT EXISTS `roles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'ADMIN, STAFF, CUSTOMER',
    `name` VARCHAR(100) NOT NULL,
    `description` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- 3. Bảng gán Quyền cho Vai trò (Role - Permission Mapping)
CREATE TABLE IF NOT EXISTS `role_permissions` (
    `role_id` BIGINT NOT NULL,
    `permission_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `permission_id`),
    CONSTRAINT `fk_role_perm_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_role_perm_perm` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB;


-- =============================================================================
-- PART 1: FLASH SALE / SPECIAL EVENTS
-- =============================================================================

CREATE TABLE IF NOT EXISTS `activities` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `name` VARCHAR(100) NOT NULL COMMENT 'Activity name',
    `description` TEXT COMMENT 'Description',
    `start_time` DATETIME NOT NULL COMMENT 'Start time of the activity',
    `end_time` DATETIME NOT NULL COMMENT 'End time of the activity',
    `status` INT NOT NULL DEFAULT 0 COMMENT '0: inactive, 1: active',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB COMMENT = 'Bảng quản lý đợt mở bán đặc biệt';

CREATE TABLE IF NOT EXISTS `activity_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `activity_id` BIGINT NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `stock_initial` INT NOT NULL DEFAULT 0,
    `stock_available` INT NOT NULL DEFAULT 0,
    `is_stock_prepared` TINYINT(1) NOT NULL DEFAULT 0,
    `price_original` DECIMAL(15,2) NOT NULL,
    `price_flash` DECIMAL(15,2) NOT NULL,
    `sale_start_time` DATETIME NOT NULL,
    `sale_end_time` DATETIME NOT NULL,
    `status` INT NOT NULL DEFAULT 0,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    KEY `idx_activity_id` (`activity_id`),
    CONSTRAINT `fk_activity_items_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`)
) ENGINE = InnoDB COMMENT = 'Bảng chi tiết các loại vé trong đợt flash sale';


-- =============================================================================
-- PART 2: CORE TRAIN BOOKING SYSTEM
-- =============================================================================

-- 4. Loại toa tàu (Carriage Types)
CREATE TABLE IF NOT EXISTS `carriage_types` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT 'SOFT_SEAT, HARD_SEAT, SLEEPER_6, SLEEPER_4',
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- 5. Users
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(100) UNIQUE NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `phone` VARCHAR(20),
    `address` VARCHAR(255),
    `nationality` VARCHAR(100),
    `reward_points` INT DEFAULT 0,
    `membership_rank` VARCHAR(50) DEFAULT 'BRONZE',
    `is_identity_verified` TINYINT(1) DEFAULT 0,
    `is_email_verified` TINYINT(1) DEFAULT 0,
    `image_url` VARCHAR(500),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    INDEX `idx_email` (`email`),
    INDEX `idx_phone` (`phone`)
) ENGINE = InnoDB;

-- 6. Bảng gán Vai trò cho Người dùng (User - Role Mapping)
CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB;

-- 7. Refresh Tokens
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `token` VARCHAR(255) NOT NULL UNIQUE,
    `expired_at` DATETIME NOT NULL,
    `revoked` TINYINT(1) DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_token` (`user_id`, `token`),
    INDEX `idx_expired_at` (`expired_at`),
    CONSTRAINT `fk_refresh_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE = InnoDB;

-- 8. Stations
CREATE TABLE IF NOT EXISTS `stations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `code` VARCHAR(20) UNIQUE NOT NULL,
    `location` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL
) ENGINE = InnoDB;

-- 9. Trains
CREATE TABLE IF NOT EXISTS `trains` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(20) UNIQUE NOT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'SE_TN',
    `description` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL
) ENGINE = InnoDB;

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
) ENGINE = InnoDB;

-- 10. Trips
CREATE TABLE IF NOT EXISTS `trips` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `train_id` BIGINT NOT NULL,
    `departure_station_id` BIGINT NOT NULL,
    `arrival_station_id` BIGINT NOT NULL,
    `departure_time` DATETIME NOT NULL,
    `arrival_time` DATETIME NOT NULL,
    `duration` INT COMMENT 'Minutes',
    `status` ENUM('SCHEDULED', 'RUNNING', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    INDEX `idx_trip_search` (`departure_station_id`, `arrival_station_id`, `departure_time`),
    INDEX `idx_trip_status` (`status`, `departure_time`),
    CONSTRAINT `fk_trips_train` FOREIGN KEY (`train_id`) REFERENCES `trains` (`id`),
    CONSTRAINT `fk_trips_dep_station` FOREIGN KEY (`departure_station_id`) REFERENCES `stations` (`id`),
    CONSTRAINT `fk_trips_arr_station` FOREIGN KEY (`arrival_station_id`) REFERENCES `stations` (`id`)
) ENGINE = InnoDB;

-- 11. Carriages
CREATE TABLE IF NOT EXISTS `carriages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `train_id` BIGINT NOT NULL,
    `name` VARCHAR(20) NOT NULL,
    `type_id` BIGINT NOT NULL,
    `seat_layout` JSON, 
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    INDEX `idx_train_type` (`train_id`, `type_id`),
    CONSTRAINT `fk_carriages_train` FOREIGN KEY (`train_id`) REFERENCES `trains` (`id`),
    CONSTRAINT `fk_carriages_type` FOREIGN KEY (`type_id`) REFERENCES `carriage_types` (`id`)
) ENGINE = InnoDB;

-- 12. Seats
CREATE TABLE IF NOT EXISTS `seats` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `carriage_id` BIGINT NOT NULL,
    `seat_number` VARCHAR(10) NOT NULL,
    `seat_type` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    INDEX `idx_carriage_seat` (`carriage_id`, `seat_number`),
    CONSTRAINT `fk_seats_carriage` FOREIGN KEY (`carriage_id`) REFERENCES `carriages` (`id`)
) ENGINE = InnoDB;

-- 13. Tickets
CREATE TABLE IF NOT EXISTS `tickets` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trip_id` BIGINT NOT NULL,
    `seat_id` BIGINT NOT NULL,
    `price` DECIMAL(15,2) NOT NULL,
    `status` ENUM('AVAILABLE', 'HOLD', 'BOOKED') NOT NULL DEFAULT 'AVAILABLE',
    `hold_expired_at` DATETIME NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_trip_seat` (`trip_id`, `seat_id`),
    INDEX `idx_status_hold` (`status`, `hold_expired_at`),
    CONSTRAINT `fk_tickets_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`),
    CONSTRAINT `fk_tickets_seat` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`)
) ENGINE = InnoDB;

-- 14. Bookings
CREATE TABLE IF NOT EXISTS `bookings` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `original_price` DECIMAL(15,2) NOT NULL DEFAULT 0,
    `promo_code` VARCHAR(50) DEFAULT NULL,
    `discount_amount` DECIMAL(15,2) NOT NULL DEFAULT 0,
    `total_price` DECIMAL(15,2) NOT NULL,
    `status` ENUM('PENDING', 'PAID', 'CONFIRMED', 'EXPIRED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED') NOT NULL DEFAULT 'PENDING',
    `expired_at` DATETIME NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_bookings_promo_code` (`promo_code`),
    INDEX `idx_expired_at` (`expired_at`),
    INDEX `idx_created_at` (`created_at`),
    CONSTRAINT `fk_bookings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE = InnoDB;

-- 15. Booking Details
CREATE TABLE IF NOT EXISTS `booking_details` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` BIGINT NOT NULL,
    `ticket_id` BIGINT NOT NULL,
    `passenger_name` VARCHAR(100) NOT NULL,
    `passenger_id_card` VARCHAR(50) NOT NULL,
    `passenger_type` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_booking_details_ticket_id` (`ticket_id`),
    CONSTRAINT `fk_booking_details_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
    CONSTRAINT `fk_booking_details_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`)
) ENGINE = InnoDB;

-- 16. Payments
CREATE TABLE IF NOT EXISTS `payments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` BIGINT NOT NULL,
    `method` VARCHAR(50) NOT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `status` ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    `transaction_id` VARCHAR(100) UNIQUE,
    `paid_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_booking_status` (`booking_id`, `status`),
    INDEX `idx_transaction_id` (`transaction_id`),
    INDEX `idx_created_at` (`created_at`),
    CONSTRAINT `fk_payments_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`)
) ENGINE = InnoDB;

-- 17. Notifications
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT,
    `type` ENUM('BOOKING_CONFIRMED', 'BOOKING_CANCELLED', 'BOOKING_EXPIRED', 'PAYMENT_SUCCESS', 'SYSTEM') NOT NULL,
    `reference_id` BIGINT NOT NULL COMMENT 'ID tham chieu, thuong la bookingId',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_notifications_user_id` (`user_id`),
    INDEX `idx_notifications_is_read` (`user_id`, `is_read`),
    INDEX `idx_notifications_created_at` (`created_at`)
) ENGINE = InnoDB;

-- 18. Ticket Prices
CREATE TABLE IF NOT EXISTS `ticket_prices` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trip_id` BIGINT NOT NULL,
    `type_id` BIGINT NOT NULL,
    `price` DECIMAL(15,2) NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_trip_type` (`trip_id`, `type_id`),
    CONSTRAINT `fk_ticket_prices_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`),
    CONSTRAINT `fk_ticket_prices_type` FOREIGN KEY (`type_id`) REFERENCES `carriage_types` (`id`)
) ENGINE = InnoDB;

-- 18. Cấu hình hệ thống (System Configs)
CREATE TABLE IF NOT EXISTS `system_configs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `config_key` VARCHAR(100) NOT NULL UNIQUE,
    `config_value` VARCHAR(255) NOT NULL,
    `description` VARCHAR(255),
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB;


-- =============================================================================
-- SEED DATA
-- =============================================================================

INSERT INTO `permissions` (`code`, `name`, `description`) VALUES 
('TICKET_BOOK', 'Sử dụng chức năng đặt vé', 'Khách hàng có quyền đặt vé'),
('TICKET_CANCEL', 'Hủy vé', 'Hủy vé đã đặt'),
('TRIP_MANAGE', 'Quản lý lịch trình', 'Admin/Staff tạo và sửa chuyến tàu'),
('USER_MANAGE', 'Quản lý người dùng', 'Chỉ Admin có quyền này');

INSERT INTO `roles` (`code`, `name`, `description`) VALUES 
('ADMIN', 'Quản trị hệ thống', 'Toàn quyền điều hành'),
('STAFF', 'Nhân viên nhà ga', 'Quản lý vé và chuyến đi'),
('CUSTOMER', 'Khách hàng', 'Đăng ký, đặt vé và thanh toán');

-- Gán quyền mẫu (Ví dụ ADMIN có tất cả quyền - Giả lập logic)
-- Thực tế sẽ dùng ID sau khi INSERT, ở đây giả sử ID theo thứ tự 1,2,3...
INSERT INTO `role_permissions` (`role_id`, `permission_id`) VALUES 
(1, 1), (1, 2), (1, 3), (1, 4), -- ADMIN
(2, 1), (2, 2), (2, 3),         -- STAFF
(3, 1), (3, 2);                 -- CUSTOMER

INSERT INTO `carriage_types` (`code`, `name`, `description`) VALUES 
('SOFT_SEAT', 'Ghế mềm điều hòa', 'Chỗ ngồi bọc da thoải mái'),
('HARD_SEAT', 'Ghế cứng', 'Chỗ ngồi gỗ tiết kiệm'),
('SLEEPER_6', 'Giường nằm khoang 6', 'Khoang 6 giường nằm tầng'),
('SLEEPER_4', 'Giường nằm khoang 4', 'Khoang 4 giường nằm chất lượng cao');

INSERT INTO `stations` (`name`, `code`, `location`) VALUES 
('Ga Hà Nội', 'HAN', 'Hà Nội'),
('Ga Đà Nẵng', 'DAN', 'Đà Nẵng'),
('Ga Sài Gòn', 'SGN', 'TP. Hồ Chí Minh');

INSERT INTO `trains` (`code`, `category`, `description`) VALUES 
('SE1', 'SE_TN', 'Tàu hỏa Thống Nhất Bắc - Nam'),
('SE3', 'HIGH_QUALITY', 'Tàu nhanh chất lượng cao');

INSERT INTO `system_configs` (`config_key`, `config_value`, `description`) VALUES 
('TICKET_HOLD_TIME_MINUTES', '15', 'Thời gian giữ ghế tạm thời (phút)'),
('MAX_TICKETS_PER_BOOKING', '4', 'Số lượng vé tối đa mỗi lần đặt');

INSERT INTO `activities` (`id`, `name`, `description`, `start_time`, `end_time`, `status`) VALUES 
(1, 'Mở bán vé Tết 2026', 'Chiến dịch bán vé tàu cao điểm Tết Nguyên Đán 2026', '2026-01-01 00:00:00', '2026-02-01 23:59:59', 1);


-- =============================================================================
-- DỮ LIỆU MẪU MỞ RỘNG (EXTENDED SEED DATA)
-- =============================================================================

-- 1. Người dùng mẫu
-- Password 'admin123' (đã hash BCrypt)
INSERT INTO `users` (`id`, `name`, `email`, `password`, `phone`) VALUES 
(1, 'Hệ thống Admin', 'admin@vetautet.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', '0912345678'),
(2, 'Nhân viên bán vé', 'staff@vetautet.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', '0987654321'),
(3, 'Nguyễn Văn Khách', 'khachhang@gmail.com', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', '0900112233');

UPDATE `users` SET `is_email_verified` = 1 WHERE `id` IN (1, 2, 3);

-- 2. Gán Vai trò cho Người dùng
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES 
(1, 1), -- Admin
(2, 2), -- Staff
(3, 3); -- Customer

-- 3. Toa tàu cho tàu SE1 (ID 1) và SE3 (ID 2)
INSERT INTO `carriages` (`id`, `train_id`, `name`, `type_id`, `seat_layout`) VALUES 
(1, 1, 'Toa 1 - Ghế Mềm', 1, '{"rows": 10, "cols": 5}'),
(2, 1, 'Toa 2 - Giường Nằm', 4, '{"rows": 25, "cols": 2}'),
(3, 2, 'Toa 1 - Ghế Cứng', 2, '{"rows": 10, "cols": 5}');

-- 4. Ghế ngồi mẫu cho Toa 1 (5 hàng x 4 cột = 20 ghế)
-- Sinh 50 ghế cho mỗi toa
INSERT INTO `seats` (`carriage_id`, `seat_number`, `seat_type`) VALUES 
(1, 'A1', 'SOFT_SEAT'),
(1, 'A2', 'SOFT_SEAT'),
(1, 'A3', 'SOFT_SEAT'),
(1, 'A4', 'SOFT_SEAT'),
(1, 'A5', 'SOFT_SEAT'),
(1, 'A6', 'SOFT_SEAT'),
(1, 'A7', 'SOFT_SEAT'),
(1, 'A8', 'SOFT_SEAT'),
(1, 'A9', 'SOFT_SEAT'),
(1, 'A10', 'SOFT_SEAT'),
(1, 'A11', 'SOFT_SEAT'),
(1, 'A12', 'SOFT_SEAT'),
(1, 'A13', 'SOFT_SEAT'),
(1, 'A14', 'SOFT_SEAT'),
(1, 'A15', 'SOFT_SEAT'),
(1, 'A16', 'SOFT_SEAT'),
(1, 'A17', 'SOFT_SEAT'),
(1, 'A18', 'SOFT_SEAT'),
(1, 'A19', 'SOFT_SEAT'),
(1, 'A20', 'SOFT_SEAT'),
(1, 'A21', 'SOFT_SEAT'),
(1, 'A22', 'SOFT_SEAT'),
(1, 'A23', 'SOFT_SEAT'),
(1, 'A24', 'SOFT_SEAT'),
(1, 'A25', 'SOFT_SEAT'),
(1, 'A26', 'SOFT_SEAT'),
(1, 'A27', 'SOFT_SEAT'),
(1, 'A28', 'SOFT_SEAT'),
(1, 'A29', 'SOFT_SEAT'),
(1, 'A30', 'SOFT_SEAT'),
(1, 'A31', 'SOFT_SEAT'),
(1, 'A32', 'SOFT_SEAT'),
(1, 'A33', 'SOFT_SEAT'),
(1, 'A34', 'SOFT_SEAT'),
(1, 'A35', 'SOFT_SEAT'),
(1, 'A36', 'SOFT_SEAT'),
(1, 'A37', 'SOFT_SEAT'),
(1, 'A38', 'SOFT_SEAT'),
(1, 'A39', 'SOFT_SEAT'),
(1, 'A40', 'SOFT_SEAT'),
(1, 'A41', 'SOFT_SEAT'),
(1, 'A42', 'SOFT_SEAT'),
(1, 'A43', 'SOFT_SEAT'),
(1, 'A44', 'SOFT_SEAT'),
(1, 'A45', 'SOFT_SEAT'),
(1, 'A46', 'SOFT_SEAT'),
(1, 'A47', 'SOFT_SEAT'),
(1, 'A48', 'SOFT_SEAT'),
(1, 'A49', 'SOFT_SEAT'),
(1, 'A50', 'SOFT_SEAT'),
(2, 'G1', 'SLEEPER'),
(2, 'G2', 'SLEEPER'),
(2, 'G3', 'SLEEPER'),
(2, 'G4', 'SLEEPER'),
(2, 'G5', 'SLEEPER'),
(2, 'G6', 'SLEEPER'),
(2, 'G7', 'SLEEPER'),
(2, 'G8', 'SLEEPER'),
(2, 'G9', 'SLEEPER'),
(2, 'G10', 'SLEEPER'),
(2, 'G11', 'SLEEPER'),
(2, 'G12', 'SLEEPER'),
(2, 'G13', 'SLEEPER'),
(2, 'G14', 'SLEEPER'),
(2, 'G15', 'SLEEPER'),
(2, 'G16', 'SLEEPER'),
(2, 'G17', 'SLEEPER'),
(2, 'G18', 'SLEEPER'),
(2, 'G19', 'SLEEPER'),
(2, 'G20', 'SLEEPER'),
(2, 'G21', 'SLEEPER'),
(2, 'G22', 'SLEEPER'),
(2, 'G23', 'SLEEPER'),
(2, 'G24', 'SLEEPER'),
(2, 'G25', 'SLEEPER'),
(2, 'G26', 'SLEEPER'),
(2, 'G27', 'SLEEPER'),
(2, 'G28', 'SLEEPER'),
(2, 'G29', 'SLEEPER'),
(2, 'G30', 'SLEEPER'),
(2, 'G31', 'SLEEPER'),
(2, 'G32', 'SLEEPER'),
(2, 'G33', 'SLEEPER'),
(2, 'G34', 'SLEEPER'),
(2, 'G35', 'SLEEPER'),
(2, 'G36', 'SLEEPER'),
(2, 'G37', 'SLEEPER'),
(2, 'G38', 'SLEEPER'),
(2, 'G39', 'SLEEPER'),
(2, 'G40', 'SLEEPER'),
(2, 'G41', 'SLEEPER'),
(2, 'G42', 'SLEEPER'),
(2, 'G43', 'SLEEPER'),
(2, 'G44', 'SLEEPER'),
(2, 'G45', 'SLEEPER'),
(2, 'G46', 'SLEEPER'),
(2, 'G47', 'SLEEPER'),
(2, 'G48', 'SLEEPER'),
(2, 'G49', 'SLEEPER'),
(2, 'G50', 'SLEEPER'),
(3, 'C1', 'HARD_SEAT'),
(3, 'C2', 'HARD_SEAT'),
(3, 'C3', 'HARD_SEAT'),
(3, 'C4', 'HARD_SEAT'),
(3, 'C5', 'HARD_SEAT'),
(3, 'C6', 'HARD_SEAT'),
(3, 'C7', 'HARD_SEAT'),
(3, 'C8', 'HARD_SEAT'),
(3, 'C9', 'HARD_SEAT'),
(3, 'C10', 'HARD_SEAT'),
(3, 'C11', 'HARD_SEAT'),
(3, 'C12', 'HARD_SEAT'),
(3, 'C13', 'HARD_SEAT'),
(3, 'C14', 'HARD_SEAT'),
(3, 'C15', 'HARD_SEAT'),
(3, 'C16', 'HARD_SEAT'),
(3, 'C17', 'HARD_SEAT'),
(3, 'C18', 'HARD_SEAT'),
(3, 'C19', 'HARD_SEAT'),
(3, 'C20', 'HARD_SEAT'),
(3, 'C21', 'HARD_SEAT'),
(3, 'C22', 'HARD_SEAT'),
(3, 'C23', 'HARD_SEAT'),
(3, 'C24', 'HARD_SEAT'),
(3, 'C25', 'HARD_SEAT'),
(3, 'C26', 'HARD_SEAT'),
(3, 'C27', 'HARD_SEAT'),
(3, 'C28', 'HARD_SEAT'),
(3, 'C29', 'HARD_SEAT'),
(3, 'C30', 'HARD_SEAT'),
(3, 'C31', 'HARD_SEAT'),
(3, 'C32', 'HARD_SEAT'),
(3, 'C33', 'HARD_SEAT'),
(3, 'C34', 'HARD_SEAT'),
(3, 'C35', 'HARD_SEAT'),
(3, 'C36', 'HARD_SEAT'),
(3, 'C37', 'HARD_SEAT'),
(3, 'C38', 'HARD_SEAT'),
(3, 'C39', 'HARD_SEAT'),
(3, 'C40', 'HARD_SEAT'),
(3, 'C41', 'HARD_SEAT'),
(3, 'C42', 'HARD_SEAT'),
(3, 'C43', 'HARD_SEAT'),
(3, 'C44', 'HARD_SEAT'),
(3, 'C45', 'HARD_SEAT'),
(3, 'C46', 'HARD_SEAT'),
(3, 'C47', 'HARD_SEAT'),
(3, 'C48', 'HARD_SEAT'),
(3, 'C49', 'HARD_SEAT'),
(3, 'C50', 'HARD_SEAT');

-- 5. Chuyến đi mẫu (Trip)
-- Hà Nội -> Sài Gòn (SE1)
INSERT INTO `trips` (`id`, `train_id`, `departure_station_id`, `arrival_station_id`, `departure_time`, `arrival_time`, `status`) VALUES 
(1, 1, 1, 3, '2026-04-15 08:00:00', '2026-04-16 10:00:00', 'SCHEDULED'),
(2, 2, 3, 1, '2026-04-15 20:00:00', '2026-04-16 22:00:00', 'SCHEDULED');

-- 6. Cấu hình giá vé cho Trip 1
INSERT INTO `ticket_prices` (`trip_id`, `type_id`, `price`) VALUES 
(1, 1, 500000), -- SE1 - Ghế mềm: 500k
(1, 4, 1200000), -- SE1 - Giường nằm: 1.2tr
(2, 2, 300000);  -- SE3 - Ghế cứng: 300k

-- 7. Trạng thái Vé thực tế (Tickets) - Kết nối Trip và Seat
-- Sinh vé cho 50 ghế mỗi toa
INSERT INTO `tickets` (`trip_id`, `seat_id`, `price`, `status`) VALUES 
(1, 1, 500000, 'AVAILABLE'),
(1, 2, 500000, 'AVAILABLE'),
(1, 3, 500000, 'AVAILABLE'),
(1, 4, 500000, 'AVAILABLE'),
(1, 5, 500000, 'AVAILABLE'),
(1, 6, 500000, 'AVAILABLE'),
(1, 7, 500000, 'AVAILABLE'),
(1, 8, 500000, 'AVAILABLE'),
(1, 9, 500000, 'AVAILABLE'),
(1, 10, 500000, 'AVAILABLE'),
(1, 11, 500000, 'AVAILABLE'),
(1, 12, 500000, 'AVAILABLE'),
(1, 13, 500000, 'AVAILABLE'),
(1, 14, 500000, 'AVAILABLE'),
(1, 15, 500000, 'AVAILABLE'),
(1, 16, 500000, 'AVAILABLE'),
(1, 17, 500000, 'AVAILABLE'),
(1, 18, 500000, 'AVAILABLE'),
(1, 19, 500000, 'AVAILABLE'),
(1, 20, 500000, 'AVAILABLE'),
(1, 21, 500000, 'AVAILABLE'),
(1, 22, 500000, 'AVAILABLE'),
(1, 23, 500000, 'AVAILABLE'),
(1, 24, 500000, 'AVAILABLE'),
(1, 25, 500000, 'AVAILABLE'),
(1, 26, 500000, 'AVAILABLE'),
(1, 27, 500000, 'AVAILABLE'),
(1, 28, 500000, 'AVAILABLE'),
(1, 29, 500000, 'AVAILABLE'),
(1, 30, 500000, 'AVAILABLE'),
(1, 31, 500000, 'AVAILABLE'),
(1, 32, 500000, 'AVAILABLE'),
(1, 33, 500000, 'AVAILABLE'),
(1, 34, 500000, 'AVAILABLE'),
(1, 35, 500000, 'AVAILABLE'),
(1, 36, 500000, 'AVAILABLE'),
(1, 37, 500000, 'AVAILABLE'),
(1, 38, 500000, 'AVAILABLE'),
(1, 39, 500000, 'AVAILABLE'),
(1, 40, 500000, 'AVAILABLE'),
(1, 41, 500000, 'AVAILABLE'),
(1, 42, 500000, 'AVAILABLE'),
(1, 43, 500000, 'AVAILABLE'),
(1, 44, 500000, 'AVAILABLE'),
(1, 45, 500000, 'AVAILABLE'),
(1, 46, 500000, 'AVAILABLE'),
(1, 47, 500000, 'AVAILABLE'),
(1, 48, 500000, 'AVAILABLE'),
(1, 49, 500000, 'AVAILABLE'),
(1, 50, 500000, 'AVAILABLE'),
(1, 51, 1200000, 'AVAILABLE'),
(1, 52, 1200000, 'AVAILABLE'),
(1, 53, 1200000, 'AVAILABLE'),
(1, 54, 1200000, 'AVAILABLE'),
(1, 55, 1200000, 'AVAILABLE'),
(1, 56, 1200000, 'AVAILABLE'),
(1, 57, 1200000, 'AVAILABLE'),
(1, 58, 1200000, 'AVAILABLE'),
(1, 59, 1200000, 'AVAILABLE'),
(1, 60, 1200000, 'AVAILABLE'),
(1, 61, 1200000, 'AVAILABLE'),
(1, 62, 1200000, 'AVAILABLE'),
(1, 63, 1200000, 'AVAILABLE'),
(1, 64, 1200000, 'AVAILABLE'),
(1, 65, 1200000, 'AVAILABLE'),
(1, 66, 1200000, 'AVAILABLE'),
(1, 67, 1200000, 'AVAILABLE'),
(1, 68, 1200000, 'AVAILABLE'),
(1, 69, 1200000, 'AVAILABLE'),
(1, 70, 1200000, 'AVAILABLE'),
(1, 71, 1200000, 'AVAILABLE'),
(1, 72, 1200000, 'AVAILABLE'),
(1, 73, 1200000, 'AVAILABLE'),
(1, 74, 1200000, 'AVAILABLE'),
(1, 75, 1200000, 'AVAILABLE'),
(1, 76, 1200000, 'AVAILABLE'),
(1, 77, 1200000, 'AVAILABLE'),
(1, 78, 1200000, 'AVAILABLE'),
(1, 79, 1200000, 'AVAILABLE'),
(1, 80, 1200000, 'AVAILABLE'),
(1, 81, 1200000, 'AVAILABLE'),
(1, 82, 1200000, 'AVAILABLE'),
(1, 83, 1200000, 'AVAILABLE'),
(1, 84, 1200000, 'AVAILABLE'),
(1, 85, 1200000, 'AVAILABLE'),
(1, 86, 1200000, 'AVAILABLE'),
(1, 87, 1200000, 'AVAILABLE'),
(1, 88, 1200000, 'AVAILABLE'),
(1, 89, 1200000, 'AVAILABLE'),
(1, 90, 1200000, 'AVAILABLE'),
(1, 91, 1200000, 'AVAILABLE'),
(1, 92, 1200000, 'AVAILABLE'),
(1, 93, 1200000, 'AVAILABLE'),
(1, 94, 1200000, 'AVAILABLE'),
(1, 95, 1200000, 'AVAILABLE'),
(1, 96, 1200000, 'AVAILABLE'),
(1, 97, 1200000, 'AVAILABLE'),
(1, 98, 1200000, 'AVAILABLE'),
(1, 99, 1200000, 'AVAILABLE'),
(1, 100, 1200000, 'AVAILABLE'),
(2, 101, 300000, 'AVAILABLE'),
(2, 102, 300000, 'AVAILABLE'),
(2, 103, 300000, 'AVAILABLE'),
(2, 104, 300000, 'AVAILABLE'),
(2, 105, 300000, 'AVAILABLE'),
(2, 106, 300000, 'AVAILABLE'),
(2, 107, 300000, 'AVAILABLE'),
(2, 108, 300000, 'AVAILABLE'),
(2, 109, 300000, 'AVAILABLE'),
(2, 110, 300000, 'AVAILABLE'),
(2, 111, 300000, 'AVAILABLE'),
(2, 112, 300000, 'AVAILABLE'),
(2, 113, 300000, 'AVAILABLE'),
(2, 114, 300000, 'AVAILABLE'),
(2, 115, 300000, 'AVAILABLE'),
(2, 116, 300000, 'AVAILABLE'),
(2, 117, 300000, 'AVAILABLE'),
(2, 118, 300000, 'AVAILABLE'),
(2, 119, 300000, 'AVAILABLE'),
(2, 120, 300000, 'AVAILABLE'),
(2, 121, 300000, 'AVAILABLE'),
(2, 122, 300000, 'AVAILABLE'),
(2, 123, 300000, 'AVAILABLE'),
(2, 124, 300000, 'AVAILABLE'),
(2, 125, 300000, 'AVAILABLE'),
(2, 126, 300000, 'AVAILABLE'),
(2, 127, 300000, 'AVAILABLE'),
(2, 128, 300000, 'AVAILABLE'),
(2, 129, 300000, 'AVAILABLE'),
(2, 130, 300000, 'AVAILABLE'),
(2, 131, 300000, 'AVAILABLE'),
(2, 132, 300000, 'AVAILABLE'),
(2, 133, 300000, 'AVAILABLE'),
(2, 134, 300000, 'AVAILABLE'),
(2, 135, 300000, 'AVAILABLE'),
(2, 136, 300000, 'AVAILABLE'),
(2, 137, 300000, 'AVAILABLE'),
(2, 138, 300000, 'AVAILABLE'),
(2, 139, 300000, 'AVAILABLE'),
(2, 140, 300000, 'AVAILABLE'),
(2, 141, 300000, 'AVAILABLE'),
(2, 142, 300000, 'AVAILABLE'),
(2, 143, 300000, 'AVAILABLE'),
(2, 144, 300000, 'AVAILABLE'),
(2, 145, 300000, 'AVAILABLE'),
(2, 146, 300000, 'AVAILABLE'),
(2, 147, 300000, 'AVAILABLE'),
(2, 148, 300000, 'AVAILABLE'),
(2, 149, 300000, 'AVAILABLE'),
(2, 150, 300000, 'AVAILABLE');

-- 8. Chi tiết đợt Flash Sale
INSERT INTO `activity_items` (`activity_id`, `name`, `stock_initial`, `stock_available`, `price_original`, `price_flash`, `sale_start_time`, `sale_end_time`, `status`) VALUES 
(1, 'Vé Tết Chặng HN-SG', 100, 100, 1500000, 999000, '2026-01-01 12:00:00', '2026-01-01 13:00:00', 1);

-- Dev seed normalization: keep homepage labels readable even when imported from shells with a non-UTF8 code page.
UPDATE stations SET name = 'Ga Ha Noi', location = 'Ha Noi' WHERE code = 'HAN';
UPDATE stations SET name = 'Ga Da Nang', location = 'Da Nang' WHERE code = 'DAN';
UPDATE stations SET name = 'Ga Sai Gon', location = 'TP. Ho Chi Minh' WHERE code = 'SGN';

UPDATE trains SET description = 'Tau hoa Thong Nhat Bac Nam' WHERE code = 'SE1';
UPDATE trains SET description = 'Tau nhanh chat luong cao' WHERE code = 'SE3';
UPDATE trains SET category = 'SE_TN' WHERE code IN ('SE1', 'SE5', 'TN1');
UPDATE trains SET category = 'HIGH_QUALITY' WHERE code = 'SE3';
UPDATE trains SET category = 'SUBURBAN' WHERE code LIKE 'SUB%';

UPDATE carriage_types SET name = 'Ghe mem dieu hoa', description = 'Cho ngoi boc da thoai mai' WHERE code = 'SOFT_SEAT';
UPDATE carriage_types SET name = 'Ghe cung', description = 'Cho ngoi tiet kiem' WHERE code = 'HARD_SEAT';
UPDATE carriage_types SET name = 'Giuong nam khoang 6', description = 'Khoang 6 giuong nam tang' WHERE code = 'SLEEPER_6';
UPDATE carriage_types SET name = 'Giuong nam khoang 4', description = 'Khoang 4 giuong nam chat luong cao' WHERE code = 'SLEEPER_4';

UPDATE carriages SET name = 'Toa 1 - Ghe Mem' WHERE id = 1;
UPDATE carriages SET name = 'Toa 2 - Giuong Nam' WHERE id = 2;
UPDATE carriages SET name = 'Toa 1 - Ghe Cung' WHERE id = 3;

CREATE TABLE IF NOT EXISTS `promotions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(160) NOT NULL,
    `description` TEXT,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `discount_type` VARCHAR(30) NOT NULL,
    `discount_value` DECIMAL(15,2) NOT NULL,
    `max_discount_amount` DECIMAL(15,2) DEFAULT NULL,
    `min_order_amount` DECIMAL(15,2) DEFAULT NULL,
    `starts_at` DATE NOT NULL,
    `ends_at` DATE NOT NULL,
    `conditions` TEXT,
    `route` VARCHAR(160) DEFAULT NULL,
    `categories` VARCHAR(255) DEFAULT NULL,
    `usage_limit` INT DEFAULT NULL,
    `used_count` INT NOT NULL DEFAULT 0,
    `ease_score` INT NOT NULL DEFAULT 70,
    `status` VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    INDEX `idx_promotions_code` (`code`),
    INDEX `idx_promotions_status_dates` (`status`, `starts_at`, `ends_at`),
    INDEX `idx_promotions_route` (`route`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO `promotions`
(`title`, `description`, `code`, `discount_type`, `discount_value`, `max_discount_amount`, `min_order_amount`, `starts_at`, `ends_at`, `conditions`, `route`, `categories`, `usage_limit`, `used_count`, `ease_score`, `status`)
VALUES
('Ve Tet sum vay', 'Uu dai som cho cac chuyen tau Tet tren truc Bac - Nam.', 'TETSUMVAY', 'percent', 18, 250000, 500000, '2026-05-01', '2027-01-20', 'Ap dung cho don tu 2 ve, dat truoc ngay khoi hanh toi thieu 7 ngay.', 'Ga Sai Gon -> Ga Ha Noi', 'tet,popularRoute', 1000, 0, 84, 'ACTIVE'),
('Sinh vien len tau', 'Tiet kiem cho sinh vien khi dat ve ghe ngoi hoac giuong nam.', 'SVRAIL', 'percent', 15, 150000, 200000, '2026-05-01', '2026-08-31', 'Can xuat trinh the sinh vien con hieu luc khi len tau.', 'Ga Ha Noi -> Vinh', 'student', 1500, 0, 78, 'ACTIVE'),
('Di ve tiet kiem hon', 'Giam truc tiep cho hanh trinh khu hoi trong cung mot don.', 'KHUTHOI', 'amount', 120000, NULL, 500000, '2026-04-15', '2026-07-15', 'Ap dung khi dat toi thieu 1 ve chieu di va 1 ve chieu ve.', NULL, 'roundTrip', 800, 0, 88, 'ACTIVE'),
('Thanh toan online cuoi tuan', 'Mien phi dich vu khi thanh toan bang vi dien tu hoac the noi dia.', 'PAYONLINE', 'serviceFee', 35000, NULL, 0, '2026-05-01', '2026-05-08', 'Ap dung tu thu Sau den Chu nhat cho don thanh toan online.', NULL, 'onlinePayment,expiring', 1200, 0, 96, 'ACTIVE'),
('Gia dinh chon khoang', 'Uu dai cho nhom gia dinh dat khoang 4 hoac khoang 6.', 'GIADINH', 'amount', 200000, NULL, 1000000, '2026-05-10', '2026-09-30', 'Don tu 4 hanh khach, ap dung cho khoang 4 hoac khoang 6.', 'Ga Da Nang -> Ga Sai Gon', 'group', 500, 0, 72, 'ACTIVE'),
('Tuyen bien mien Trung', 'Giam manh cho tuyen Sai Gon - Da Nang trong mua du lich.', 'BIENXANH', 'percent', 20, 220000, 300000, '2026-05-01', '2026-06-30', 'Ap dung cho ve tu thu Hai den thu Nam, khong cong don uu dai.', 'Ga Sai Gon -> Ga Da Nang', 'popularRoute', 700, 0, 81, 'ACTIVE'),
('Chot ve thang 5', 'Uu dai ngan ngay cho cac chuyen con nhieu ghe trong.', 'MAYLAST', 'amount', 80000, NULL, 300000, '2026-05-01', '2026-05-06', 'So luong ma co han, ap dung cho don tu 300.000d.', NULL, 'expiring,onlinePayment', 400, 0, 90, 'ACTIVE'),
('Nhom ban mua he', 'Cang dong cang tiet kiem cho nhom tu 6 hanh khach.', 'NHOMHE', 'percent', 12, 180000, 600000, '2026-05-15', '2026-08-15', 'Ap dung cho don tu 6 ve cung chuyen, cung hang ghe.', NULL, 'group', 600, 0, 69, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    discount_type = VALUES(discount_type),
    discount_value = VALUES(discount_value),
    max_discount_amount = VALUES(max_discount_amount),
    min_order_amount = VALUES(min_order_amount),
    starts_at = VALUES(starts_at),
    ends_at = VALUES(ends_at),
    conditions = VALUES(conditions),
    route = VALUES(route),
    categories = VALUES(categories),
    usage_limit = VALUES(usage_limit),
    ease_score = VALUES(ease_score),
    status = VALUES(status),
    deleted_at = NULL;
