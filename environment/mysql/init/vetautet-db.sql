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
    `service_date` DATE DEFAULT NULL,
    `estimated_departure_time` DATETIME DEFAULT NULL,
    `estimated_arrival_time` DATETIME DEFAULT NULL,
    `actual_departure_time` DATETIME DEFAULT NULL,
    `actual_arrival_time` DATETIME DEFAULT NULL,
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
    `order_number` VARCHAR(64) NOT NULL UNIQUE,
    `storage_month` CHAR(6) NOT NULL,
    `trip_type` ENUM('ONE_WAY', 'ROUND_TRIP') NOT NULL DEFAULT 'ONE_WAY',
    `user_id` BIGINT NOT NULL,
    `original_price` DECIMAL(15,2) NOT NULL DEFAULT 0,
    `promo_code` VARCHAR(50) DEFAULT NULL,
    `discount_amount` DECIMAL(15,2) NOT NULL DEFAULT 0,
    `total_price` DECIMAL(15,2) NOT NULL,
    `contact_name` VARCHAR(150) DEFAULT NULL,
    `contact_email` VARCHAR(150) DEFAULT NULL,
    `contact_phone` VARCHAR(30) DEFAULT NULL,
    `contact_id_card` VARCHAR(512) DEFAULT NULL,
    `status` ENUM('PENDING', 'PAID', 'CONFIRMED', 'EXPIRED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED') NOT NULL DEFAULT 'PENDING',
    `expired_at` DATETIME NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME DEFAULT NULL,
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_bookings_storage_month` (`storage_month`),
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
    `direction` ENUM('OUTBOUND', 'RETURN') NOT NULL DEFAULT 'OUTBOUND',
    `departure_station_id` BIGINT DEFAULT NULL,
    `arrival_station_id` BIGINT DEFAULT NULL,
    `segment_ids` VARCHAR(255) DEFAULT NULL,
    `segment_price` DECIMAL(15,2) DEFAULT NULL,
    `passenger_name` VARCHAR(100) NOT NULL,
    `passenger_id_card` VARCHAR(512) NOT NULL,
    `passenger_type` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_booking_details_ticket_id` (`ticket_id`),
    KEY `idx_booking_details_direction` (`booking_id`, `direction`),
    KEY `idx_booking_details_route` (`departure_station_id`, `arrival_station_id`),
    CONSTRAINT `fk_booking_details_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`),
    CONSTRAINT `fk_booking_details_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `tickets` (`id`),
    CONSTRAINT `fk_booking_details_departure_station` FOREIGN KEY (`departure_station_id`) REFERENCES `stations` (`id`),
    CONSTRAINT `fk_booking_details_arrival_station` FOREIGN KEY (`arrival_station_id`) REFERENCES `stations` (`id`)
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

-- 17. Benchmark Test Orders
CREATE TABLE IF NOT EXISTS `test_orders` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `request_id` VARCHAR(80) NOT NULL,
    `user_ref` BIGINT NOT NULL,
    `ticket_ref` BIGINT NULL,
    `quantity` INT NOT NULL,
    `amount` DECIMAL(18,2) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `source` VARCHAR(32) NOT NULL,
    `note` VARCHAR(255) NULL,
    `kafka_key` VARCHAR(80) NOT NULL,
    `received_at` DATETIME NOT NULL,
    `processed_at` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL,
    `updated_at` DATETIME NOT NULL,
    UNIQUE KEY `uk_test_orders_request_id` (`request_id`),
    KEY `idx_test_orders_user_ref` (`user_ref`),
    KEY `idx_test_orders_ticket_ref` (`ticket_ref`),
    KEY `idx_test_orders_status` (`status`),
    KEY `idx_test_orders_processed_at` (`processed_at`),
    KEY `idx_test_orders_created_at` (`created_at`)
) ENGINE = InnoDB;

-- 18. Notifications
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
) ENGINE = InnoDB;

-- 19. Ticket Prices
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
-- 20. Trip Stops / Itinerary
CREATE TABLE IF NOT EXISTS `trip_stops` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trip_id` BIGINT NOT NULL,
    `station_id` BIGINT NOT NULL,
    `stop_order` INT NOT NULL,
    `scheduled_arrival_time` DATETIME DEFAULT NULL,
    `scheduled_departure_time` DATETIME DEFAULT NULL,
    `estimated_arrival_time` DATETIME DEFAULT NULL,
    `estimated_departure_time` DATETIME DEFAULT NULL,
    `actual_arrival_time` DATETIME DEFAULT NULL,
    `actual_departure_time` DATETIME DEFAULT NULL,
    `distance_from_origin_km` DECIMAL(10,2) DEFAULT 0,
    `status` ENUM('SCHEDULED', 'ARRIVING', 'ARRIVED', 'DEPARTED', 'DELAYED', 'SKIPPED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    `platform` VARCHAR(20) DEFAULT NULL,
    `note` VARCHAR(255) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_trip_stop_order` (`trip_id`, `stop_order`),
    UNIQUE KEY `uk_trip_station` (`trip_id`, `station_id`),
    INDEX `idx_trip_stops_station` (`station_id`),
    CONSTRAINT `fk_trip_stops_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`),
    CONSTRAINT `fk_trip_stops_station` FOREIGN KEY (`station_id`) REFERENCES `stations` (`id`)
) ENGINE = InnoDB;

-- 21. Trip Segments between consecutive stops
CREATE TABLE IF NOT EXISTS `trip_segments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trip_id` BIGINT NOT NULL,
    `from_stop_id` BIGINT NOT NULL,
    `to_stop_id` BIGINT NOT NULL,
    `segment_order` INT NOT NULL,
    `distance_km` DECIMAL(10,2) DEFAULT 0,
    `status` ENUM('SCHEDULED', 'RUNNING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_trip_segment_order` (`trip_id`, `segment_order`),
    INDEX `idx_trip_segments_from_stop` (`from_stop_id`),
    INDEX `idx_trip_segments_to_stop` (`to_stop_id`),
    CONSTRAINT `fk_trip_segments_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`),
    CONSTRAINT `fk_trip_segments_from_stop` FOREIGN KEY (`from_stop_id`) REFERENCES `trip_stops` (`id`),
    CONSTRAINT `fk_trip_segments_to_stop` FOREIGN KEY (`to_stop_id`) REFERENCES `trip_stops` (`id`)
) ENGINE = InnoDB;

-- 22. Segment fare table
CREATE TABLE IF NOT EXISTS `trip_segment_prices` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `segment_id` BIGINT NOT NULL,
    `carriage_type_id` BIGINT NOT NULL,
    `passenger_type` VARCHAR(30) NOT NULL DEFAULT 'ADULT',
    `price` DECIMAL(15,2) NOT NULL,
    `currency` VARCHAR(10) NOT NULL DEFAULT 'VND',
    `status` ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    `effective_from` DATETIME DEFAULT NULL,
    `effective_to` DATETIME DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_segment_price` (`segment_id`, `carriage_type_id`, `passenger_type`),
    INDEX `idx_trip_segment_prices_type` (`carriage_type_id`),
    CONSTRAINT `fk_trip_segment_prices_segment` FOREIGN KEY (`segment_id`) REFERENCES `trip_segments` (`id`),
    CONSTRAINT `fk_trip_segment_prices_type` FOREIGN KEY (`carriage_type_id`) REFERENCES `carriage_types` (`id`)
) ENGINE = InnoDB;

-- 23. Seat state per segment
CREATE TABLE IF NOT EXISTS `seat_segment_inventory` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trip_id` BIGINT NOT NULL,
    `segment_id` BIGINT NOT NULL,
    `seat_id` BIGINT NOT NULL,
    `status` ENUM('AVAILABLE', 'HOLD', 'BOOKED', 'BLOCKED') NOT NULL DEFAULT 'AVAILABLE',
    `hold_expired_at` DATETIME DEFAULT NULL,
    `booking_detail_id` BIGINT DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_segment_seat` (`segment_id`, `seat_id`),
    INDEX `idx_seat_segment_trip_seat_status` (`trip_id`, `seat_id`, `status`),
    INDEX `idx_seat_segment_booking_detail` (`booking_detail_id`),
    CONSTRAINT `fk_seat_segment_trip` FOREIGN KEY (`trip_id`) REFERENCES `trips` (`id`),
    CONSTRAINT `fk_seat_segment_segment` FOREIGN KEY (`segment_id`) REFERENCES `trip_segments` (`id`),
    CONSTRAINT `fk_seat_segment_seat` FOREIGN KEY (`seat_id`) REFERENCES `seats` (`id`),
    CONSTRAINT `fk_seat_segment_booking_detail` FOREIGN KEY (`booking_detail_id`) REFERENCES `booking_details` (`id`)
) ENGINE = InnoDB;

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
-- Seed trip itinerary, segment prices, and segment seat inventory for existing demo trips.
UPDATE `trips`
SET `service_date` = DATE(`departure_time`),
    `estimated_departure_time` = COALESCE(`estimated_departure_time`, `departure_time`),
    `estimated_arrival_time` = COALESCE(`estimated_arrival_time`, `arrival_time`)
WHERE `service_date` IS NULL
   OR `estimated_departure_time` IS NULL
   OR `estimated_arrival_time` IS NULL;

INSERT INTO `trip_stops`
(`trip_id`, `station_id`, `stop_order`, `scheduled_arrival_time`, `scheduled_departure_time`, `estimated_arrival_time`, `estimated_departure_time`, `distance_from_origin_km`, `status`)
SELECT t.id, t.departure_station_id, 1, NULL, t.departure_time, NULL, t.departure_time, 0, 'SCHEDULED'
FROM trips t
WHERE NOT EXISTS (
    SELECT 1 FROM trip_stops s WHERE s.trip_id = t.id AND s.stop_order = 1
);

INSERT INTO `trip_stops`
(`trip_id`, `station_id`, `stop_order`, `scheduled_arrival_time`, `scheduled_departure_time`, `estimated_arrival_time`, `estimated_departure_time`, `distance_from_origin_km`, `status`)
SELECT t.id,
       mid.id,
       2,
       TIMESTAMPADD(MINUTE, FLOOR(TIMESTAMPDIFF(MINUTE, t.departure_time, t.arrival_time) / 2), t.departure_time),
       TIMESTAMPADD(MINUTE, FLOOR(TIMESTAMPDIFF(MINUTE, t.departure_time, t.arrival_time) / 2) + 15, t.departure_time),
       TIMESTAMPADD(MINUTE, FLOOR(TIMESTAMPDIFF(MINUTE, t.departure_time, t.arrival_time) / 2), t.departure_time),
       TIMESTAMPADD(MINUTE, FLOOR(TIMESTAMPDIFF(MINUTE, t.departure_time, t.arrival_time) / 2) + 15, t.departure_time),
       791,
       'SCHEDULED'
FROM trips t
JOIN stations dep ON dep.id = t.departure_station_id
JOIN stations arr ON arr.id = t.arrival_station_id
JOIN stations mid ON mid.code = 'DAN'
WHERE ((dep.code = 'HAN' AND arr.code = 'SGN') OR (dep.code = 'SGN' AND arr.code = 'HAN'))
  AND NOT EXISTS (
      SELECT 1 FROM trip_stops s WHERE s.trip_id = t.id AND s.stop_order = 2
  );

INSERT INTO `trip_stops`
(`trip_id`, `station_id`, `stop_order`, `scheduled_arrival_time`, `scheduled_departure_time`, `estimated_arrival_time`, `estimated_departure_time`, `distance_from_origin_km`, `status`)
SELECT t.id,
       t.arrival_station_id,
       CASE WHEN ((dep.code = 'HAN' AND arr.code = 'SGN') OR (dep.code = 'SGN' AND arr.code = 'HAN')) THEN 3 ELSE 2 END,
       t.arrival_time,
       NULL,
       t.arrival_time,
       NULL,
       CASE
           WHEN ((dep.code = 'HAN' AND arr.code = 'SGN') OR (dep.code = 'SGN' AND arr.code = 'HAN')) THEN 1726
           WHEN ((dep.code = 'HAN' AND arr.code = 'DAN') OR (dep.code = 'DAN' AND arr.code = 'HAN')) THEN 791
           WHEN ((dep.code = 'DAN' AND arr.code = 'SGN') OR (dep.code = 'SGN' AND arr.code = 'DAN')) THEN 935
           ELSE 0
       END,
       'SCHEDULED'
FROM trips t
JOIN stations dep ON dep.id = t.departure_station_id
JOIN stations arr ON arr.id = t.arrival_station_id
WHERE NOT EXISTS (
    SELECT 1
    FROM trip_stops s
    WHERE s.trip_id = t.id
      AND s.stop_order = CASE WHEN ((dep.code = 'HAN' AND arr.code = 'SGN') OR (dep.code = 'SGN' AND arr.code = 'HAN')) THEN 3 ELSE 2 END
);

INSERT INTO `trip_segments` (`trip_id`, `from_stop_id`, `to_stop_id`, `segment_order`, `distance_km`, `status`)
SELECT s1.trip_id,
       s1.id,
       s2.id,
       s1.stop_order,
       GREATEST(COALESCE(s2.distance_from_origin_km, 0) - COALESCE(s1.distance_from_origin_km, 0), 0),
       'SCHEDULED'
FROM trip_stops s1
JOIN trip_stops s2 ON s2.trip_id = s1.trip_id AND s2.stop_order = s1.stop_order + 1
WHERE NOT EXISTS (
    SELECT 1 FROM trip_segments seg
    WHERE seg.trip_id = s1.trip_id AND seg.segment_order = s1.stop_order
);

INSERT INTO `trip_segment_prices` (`segment_id`, `carriage_type_id`, `passenger_type`, `price`, `currency`, `status`)
SELECT seg.id,
       tp.type_id,
       'ADULT',
       ROUND(tp.price / segment_count.segment_total, 0),
       'VND',
       'ACTIVE'
FROM trip_segments seg
JOIN ticket_prices tp ON tp.trip_id = seg.trip_id
JOIN (
    SELECT trip_id, COUNT(*) AS segment_total
    FROM trip_segments
    GROUP BY trip_id
) segment_count ON segment_count.trip_id = seg.trip_id
ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    currency = VALUES(currency),
    status = VALUES(status);

INSERT INTO `seat_segment_inventory` (`trip_id`, `segment_id`, `seat_id`, `status`)
SELECT seg.trip_id,
       seg.id,
       s.id,
       COALESCE(tk.status, 'AVAILABLE')
FROM trip_segments seg
JOIN trips t ON t.id = seg.trip_id
JOIN carriages c ON c.train_id = t.train_id
JOIN seats s ON s.carriage_id = c.id
LEFT JOIN tickets tk ON tk.trip_id = seg.trip_id AND tk.seat_id = s.id
WHERE NOT EXISTS (
    SELECT 1 FROM seat_segment_inventory inv
    WHERE inv.segment_id = seg.id AND inv.seat_id = s.id
);

-- Demo trips with detailed stops, ordered segments, segment fares, and segment seat inventory.
INSERT INTO `stations` (`name`, `code`, `location`) VALUES
('Ga Vinh', 'VIN', 'Nghệ An'),
('Ga Huế', 'HUE', 'Thừa Thiên Huế'),
('Ga Nha Trang', 'NTR', 'Khánh Hòa')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `location` = VALUES(`location`),
    `deleted_at` = NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_demo_trips`;
CREATE TEMPORARY TABLE `tmp_demo_trips` (
    `trip_id` BIGINT NOT NULL,
    `train_code` VARCHAR(20) NOT NULL,
    `departure_code` VARCHAR(20) NOT NULL,
    `arrival_code` VARCHAR(20) NOT NULL,
    `departure_time` DATETIME NOT NULL,
    `arrival_time` DATETIME NOT NULL,
    `status` VARCHAR(30) NOT NULL
);

INSERT INTO `tmp_demo_trips`
(`trip_id`, `train_code`, `departure_code`, `arrival_code`, `departure_time`, `arrival_time`, `status`) VALUES
(1, 'SE1', 'HAN', 'SGN', '2026-06-02 08:00:00', '2026-06-03 18:20:00', 'SCHEDULED'),
(2, 'SE3', 'SGN', 'HAN', '2026-06-02 19:30:00', '2026-06-04 05:45:00', 'SCHEDULED'),
(3, 'SE1', 'HAN', 'DAN', '2026-06-03 06:20:00', '2026-06-03 21:05:00', 'SCHEDULED'),
(4, 'SE3', 'DAN', 'SGN', '2026-06-03 07:40:00', '2026-06-04 02:25:00', 'SCHEDULED'),
(5, 'SE1', 'SGN', 'HAN', '2026-06-04 09:10:00', '2026-06-05 19:40:00', 'SCHEDULED');

INSERT INTO `trips`
(`id`, `train_id`, `departure_station_id`, `arrival_station_id`, `departure_time`, `arrival_time`, `service_date`,
 `estimated_departure_time`, `estimated_arrival_time`, `duration`, `status`)
SELECT dt.trip_id,
       tr.id,
       dep.id,
       arr.id,
       dt.departure_time,
       dt.arrival_time,
       DATE(dt.departure_time),
       dt.departure_time,
       dt.arrival_time,
       TIMESTAMPDIFF(MINUTE, dt.departure_time, dt.arrival_time),
       dt.status
FROM `tmp_demo_trips` dt
JOIN `trains` tr ON tr.code = dt.train_code
JOIN `stations` dep ON dep.code = dt.departure_code
JOIN `stations` arr ON arr.code = dt.arrival_code
ON DUPLICATE KEY UPDATE
    `train_id` = VALUES(`train_id`),
    `departure_station_id` = VALUES(`departure_station_id`),
    `arrival_station_id` = VALUES(`arrival_station_id`),
    `departure_time` = VALUES(`departure_time`),
    `arrival_time` = VALUES(`arrival_time`),
    `service_date` = VALUES(`service_date`),
    `estimated_departure_time` = VALUES(`estimated_departure_time`),
    `estimated_arrival_time` = VALUES(`estimated_arrival_time`),
    `duration` = VALUES(`duration`),
    `status` = VALUES(`status`),
    `deleted_at` = NULL;

DELETE inv
FROM `seat_segment_inventory` inv
JOIN `trip_segments` seg ON seg.id = inv.segment_id
WHERE seg.trip_id IN (SELECT trip_id FROM `tmp_demo_trips`);

DELETE price
FROM `trip_segment_prices` price
JOIN `trip_segments` seg ON seg.id = price.segment_id
WHERE seg.trip_id IN (SELECT trip_id FROM `tmp_demo_trips`);

DELETE FROM `trip_segments`
WHERE `trip_id` IN (SELECT trip_id FROM `tmp_demo_trips`);

DELETE FROM `trip_stops`
WHERE `trip_id` IN (SELECT trip_id FROM `tmp_demo_trips`);

DROP TEMPORARY TABLE IF EXISTS `tmp_demo_trip_stops`;
CREATE TEMPORARY TABLE `tmp_demo_trip_stops` (
    `trip_id` BIGINT NOT NULL,
    `station_code` VARCHAR(20) NOT NULL,
    `stop_order` INT NOT NULL,
    `scheduled_arrival_time` DATETIME NULL,
    `scheduled_departure_time` DATETIME NULL,
    `distance_from_origin_km` DECIMAL(10,2) NOT NULL,
    `status` VARCHAR(30) NOT NULL,
    `platform` VARCHAR(20) NULL,
    `note` VARCHAR(255) NULL
);

INSERT INTO `tmp_demo_trip_stops`
(`trip_id`, `station_code`, `stop_order`, `scheduled_arrival_time`, `scheduled_departure_time`, `distance_from_origin_km`, `status`, `platform`, `note`) VALUES
(1, 'HAN', 1, NULL, '2026-06-02 08:00:00', 0, 'SCHEDULED', '1', NULL),
(1, 'VIN', 2, '2026-06-02 13:05:00', '2026-06-02 13:15:00', 319, 'SCHEDULED', '2', NULL),
(1, 'HUE', 3, '2026-06-02 20:25:00', '2026-06-02 20:40:00', 688, 'SCHEDULED', '1', NULL),
(1, 'DAN', 4, '2026-06-02 22:50:00', '2026-06-02 23:05:00', 791, 'SCHEDULED', '3', NULL),
(1, 'NTR', 5, '2026-06-03 08:35:00', '2026-06-03 08:50:00', 1315, 'SCHEDULED', '2', NULL),
(1, 'SGN', 6, '2026-06-03 18:20:00', NULL, 1726, 'SCHEDULED', '5', NULL),
(2, 'SGN', 1, NULL, '2026-06-02 19:30:00', 0, 'SCHEDULED', '4', NULL),
(2, 'NTR', 2, '2026-06-03 04:55:00', '2026-06-03 05:10:00', 411, 'SCHEDULED', '2', NULL),
(2, 'DAN', 3, '2026-06-03 14:40:00', '2026-06-03 14:55:00', 935, 'DELAYED', '1', 'Cham 20 phut do uu tien tranh tau'),
(2, 'HUE', 4, '2026-06-03 17:05:00', '2026-06-03 17:20:00', 1038, 'SCHEDULED', '2', NULL),
(2, 'VIN', 5, '2026-06-04 00:30:00', '2026-06-04 00:40:00', 1407, 'SCHEDULED', '1', NULL),
(2, 'HAN', 6, '2026-06-04 05:45:00', NULL, 1726, 'SCHEDULED', '3', NULL),
(3, 'HAN', 1, NULL, '2026-06-03 06:20:00', 0, 'SCHEDULED', '2', NULL),
(3, 'VIN', 2, '2026-06-03 11:20:00', '2026-06-03 11:30:00', 319, 'SCHEDULED', '1', NULL),
(3, 'HUE', 3, '2026-06-03 18:35:00', '2026-06-03 18:50:00', 688, 'SCHEDULED', '2', NULL),
(3, 'DAN', 4, '2026-06-03 21:05:00', NULL, 791, 'SCHEDULED', '4', NULL),
(4, 'DAN', 1, NULL, '2026-06-03 07:40:00', 0, 'SCHEDULED', '1', NULL),
(4, 'NTR', 2, '2026-06-03 16:55:00', '2026-06-03 17:10:00', 524, 'SCHEDULED', '2', NULL),
(4, 'SGN', 3, '2026-06-04 02:25:00', NULL, 935, 'SCHEDULED', '6', NULL),
(5, 'SGN', 1, NULL, '2026-06-04 09:10:00', 0, 'SCHEDULED', '5', NULL),
(5, 'NTR', 2, '2026-06-04 18:20:00', '2026-06-04 18:35:00', 411, 'SCHEDULED', '2', NULL),
(5, 'DAN', 3, '2026-06-05 04:10:00', '2026-06-05 04:25:00', 935, 'SCHEDULED', '1', NULL),
(5, 'HAN', 4, '2026-06-05 19:40:00', NULL, 1726, 'SCHEDULED', '3', NULL);

INSERT INTO `trip_stops`
(`trip_id`, `station_id`, `stop_order`, `scheduled_arrival_time`, `scheduled_departure_time`,
 `estimated_arrival_time`, `estimated_departure_time`, `distance_from_origin_km`, `status`, `platform`, `note`)
SELECT ds.trip_id,
       st.id,
       ds.stop_order,
       ds.scheduled_arrival_time,
       ds.scheduled_departure_time,
       ds.scheduled_arrival_time,
       ds.scheduled_departure_time,
       ds.distance_from_origin_km,
       ds.status,
       ds.platform,
       ds.note
FROM `tmp_demo_trip_stops` ds
JOIN `stations` st ON st.code = ds.station_code
ORDER BY ds.trip_id, ds.stop_order;

INSERT INTO `trip_segments` (`trip_id`, `from_stop_id`, `to_stop_id`, `segment_order`, `distance_km`, `status`)
SELECT s1.trip_id,
       s1.id,
       s2.id,
       s1.stop_order,
       GREATEST(COALESCE(s2.distance_from_origin_km, 0) - COALESCE(s1.distance_from_origin_km, 0), 0),
       'SCHEDULED'
FROM `trip_stops` s1
JOIN `trip_stops` s2 ON s2.trip_id = s1.trip_id AND s2.stop_order = s1.stop_order + 1
WHERE s1.trip_id IN (SELECT trip_id FROM `tmp_demo_trips`)
ORDER BY s1.trip_id, s1.stop_order;

DROP TEMPORARY TABLE IF EXISTS `tmp_demo_segment_prices`;
CREATE TEMPORARY TABLE `tmp_demo_segment_prices` (
    `trip_id` BIGINT NOT NULL,
    `segment_order` INT NOT NULL,
    `carriage_type_code` VARCHAR(50) NOT NULL,
    `passenger_type` VARCHAR(30) NOT NULL,
    `price` DECIMAL(15,2) NOT NULL
);

INSERT INTO `tmp_demo_segment_prices`
(`trip_id`, `segment_order`, `carriage_type_code`, `passenger_type`, `price`) VALUES
(1, 1, 'SOFT_SEAT', 'ADULT', 130000), (1, 1, 'SLEEPER_4', 'ADULT', 260000),
(1, 2, 'SOFT_SEAT', 'ADULT', 210000), (1, 2, 'SLEEPER_4', 'ADULT', 420000),
(1, 3, 'SOFT_SEAT', 'ADULT', 75000),  (1, 3, 'SLEEPER_4', 'ADULT', 150000),
(1, 4, 'SOFT_SEAT', 'ADULT', 320000), (1, 4, 'SLEEPER_4', 'ADULT', 620000),
(1, 5, 'SOFT_SEAT', 'ADULT', 360000), (1, 5, 'SLEEPER_4', 'ADULT', 720000),
(2, 1, 'HARD_SEAT', 'ADULT', 310000),
(2, 2, 'HARD_SEAT', 'ADULT', 300000),
(2, 3, 'HARD_SEAT', 'ADULT', 80000),
(2, 4, 'HARD_SEAT', 'ADULT', 220000),
(2, 5, 'HARD_SEAT', 'ADULT', 170000),
(3, 1, 'SOFT_SEAT', 'ADULT', 125000), (3, 1, 'SLEEPER_4', 'ADULT', 240000),
(3, 2, 'SOFT_SEAT', 'ADULT', 210000), (3, 2, 'SLEEPER_4', 'ADULT', 410000),
(3, 3, 'SOFT_SEAT', 'ADULT', 80000),  (3, 3, 'SLEEPER_4', 'ADULT', 155000),
(4, 1, 'HARD_SEAT', 'ADULT', 330000),
(4, 2, 'HARD_SEAT', 'ADULT', 370000),
(5, 1, 'SOFT_SEAT', 'ADULT', 390000), (5, 1, 'SLEEPER_4', 'ADULT', 720000),
(5, 2, 'SOFT_SEAT', 'ADULT', 330000), (5, 2, 'SLEEPER_4', 'ADULT', 650000),
(5, 3, 'SOFT_SEAT', 'ADULT', 780000), (5, 3, 'SLEEPER_4', 'ADULT', 1350000);

DELETE tp
FROM `ticket_prices` tp
JOIN `tmp_demo_trips` dt ON dt.trip_id = tp.trip_id
LEFT JOIN (
    SELECT dsp.trip_id, ct.id AS type_id
    FROM `tmp_demo_segment_prices` dsp
    JOIN `carriage_types` ct ON ct.code = dsp.carriage_type_code
    GROUP BY dsp.trip_id, ct.id
) allowed_price ON allowed_price.trip_id = tp.trip_id AND allowed_price.type_id = tp.type_id
WHERE allowed_price.type_id IS NULL;

INSERT INTO `ticket_prices` (`trip_id`, `type_id`, `price`)
SELECT dsp.trip_id,
       ct.id,
       SUM(dsp.price)
FROM `tmp_demo_segment_prices` dsp
JOIN `carriage_types` ct ON ct.code = dsp.carriage_type_code
GROUP BY dsp.trip_id, ct.id
ON DUPLICATE KEY UPDATE
    `price` = VALUES(`price`);

INSERT INTO `trip_segment_prices` (`segment_id`, `carriage_type_id`, `passenger_type`, `price`, `currency`, `status`)
SELECT seg.id,
       ct.id,
       dsp.passenger_type,
       dsp.price,
       'VND',
       'ACTIVE'
FROM `tmp_demo_segment_prices` dsp
JOIN `trip_segments` seg ON seg.trip_id = dsp.trip_id AND seg.segment_order = dsp.segment_order
JOIN `carriage_types` ct ON ct.code = dsp.carriage_type_code
ON DUPLICATE KEY UPDATE
    `price` = VALUES(`price`),
    `currency` = VALUES(`currency`),
    `status` = VALUES(`status`);

DELETE tk
FROM `tickets` tk
JOIN `trips` t ON t.id = tk.trip_id
JOIN `seats` s ON s.id = tk.seat_id
JOIN `carriages` c ON c.id = s.carriage_id
LEFT JOIN `booking_details` bd ON bd.ticket_id = tk.id
WHERE tk.trip_id IN (SELECT trip_id FROM `tmp_demo_trips`)
  AND c.train_id <> t.train_id
  AND bd.id IS NULL;

INSERT INTO `tickets` (`trip_id`, `seat_id`, `price`, `status`)
SELECT t.id,
       s.id,
       tp.price,
       CASE
           WHEN t.id = 1 AND s.seat_number IN ('A3', 'A4', 'G8') THEN 'BOOKED'
           WHEN t.id = 2 AND s.seat_number IN ('C2', 'C7', 'C12') THEN 'BOOKED'
           WHEN t.id = 3 AND s.seat_number IN ('A1', 'G1') THEN 'HOLD'
           WHEN t.id = 4 AND s.seat_number IN ('C4', 'C5', 'C6') THEN 'BOOKED'
           WHEN t.id = 5 AND s.seat_number IN ('A9', 'G2', 'G3') THEN 'BOOKED'
           ELSE 'AVAILABLE'
       END
FROM `trips` t
JOIN `carriages` c ON c.train_id = t.train_id
JOIN `seats` s ON s.carriage_id = c.id
JOIN `ticket_prices` tp ON tp.trip_id = t.id AND tp.type_id = c.type_id
WHERE t.id IN (SELECT trip_id FROM `tmp_demo_trips`)
ON DUPLICATE KEY UPDATE
    `price` = VALUES(`price`),
    `status` = IF(`tickets`.`status` = 'AVAILABLE', VALUES(`status`), `tickets`.`status`);

INSERT INTO `seat_segment_inventory` (`trip_id`, `segment_id`, `seat_id`, `status`)
SELECT seg.trip_id,
       seg.id,
       s.id,
       CASE
           WHEN seg.trip_id = 1 AND seg.segment_order IN (2, 3) AND s.seat_number IN ('A3', 'A4', 'G8') THEN 'BOOKED'
           WHEN seg.trip_id = 2 AND seg.segment_order IN (1, 2, 3) AND s.seat_number IN ('C2', 'C7', 'C12') THEN 'BOOKED'
           WHEN seg.trip_id = 3 AND seg.segment_order = 1 AND s.seat_number IN ('A1', 'G1') THEN 'HOLD'
           WHEN seg.trip_id = 4 AND s.seat_number IN ('C4', 'C5', 'C6') THEN 'BOOKED'
           WHEN seg.trip_id = 5 AND seg.segment_order = 2 AND s.seat_number IN ('A9', 'G2', 'G3') THEN 'BLOCKED'
           ELSE 'AVAILABLE'
       END
FROM `trip_segments` seg
JOIN `trips` t ON t.id = seg.trip_id
JOIN `carriages` c ON c.train_id = t.train_id
JOIN `seats` s ON s.carriage_id = c.id
LEFT JOIN `tickets` tk ON tk.trip_id = seg.trip_id AND tk.seat_id = s.id
WHERE seg.trip_id IN (SELECT trip_id FROM `tmp_demo_trips`)
ON DUPLICATE KEY UPDATE
    `status` = IF(`seat_segment_inventory`.`status` = 'AVAILABLE', VALUES(`status`), `seat_segment_inventory`.`status`);

DROP TEMPORARY TABLE IF EXISTS `tmp_demo_segment_prices`;
DROP TEMPORARY TABLE IF EXISTS `tmp_demo_trip_stops`;
DROP TEMPORARY TABLE IF EXISTS `tmp_demo_trips`;

INSERT INTO `activity_items` (`activity_id`, `name`, `stock_initial`, `stock_available`, `price_original`, `price_flash`, `sale_start_time`, `sale_end_time`, `status`) VALUES 
(1, 'Vé Tết Chặng HN-SG', 100, 100, 1500000, 999000, '2026-01-01 12:00:00', '2026-01-01 13:00:00', 1);

-- Dev seed display labels.
UPDATE stations SET name = 'Ga Hà Nội', location = 'Hà Nội' WHERE code = 'HAN';
UPDATE stations SET name = 'Ga Đà Nẵng', location = 'Đà Nẵng' WHERE code = 'DAN';
UPDATE stations SET name = 'Ga Sài Gòn', location = 'TP. Hồ Chí Minh' WHERE code = 'SGN';
UPDATE stations SET name = 'Ga Vinh', location = 'Nghệ An' WHERE code = 'VIN';
UPDATE stations SET name = 'Ga Huế', location = 'Thừa Thiên Huế' WHERE code = 'HUE';
UPDATE stations SET name = 'Ga Nha Trang', location = 'Khánh Hòa' WHERE code = 'NTR';

UPDATE trains SET description = 'Tàu hỏa Thống Nhất Bắc Nam' WHERE code = 'SE1';
UPDATE trains SET description = 'Tàu nhanh chất lượng cao' WHERE code = 'SE3';
UPDATE trains SET description = 'Tàu nhanh Bắc Nam bổ sung' WHERE code = 'SE5';
UPDATE trains SET description = 'Tàu thống nhất đêm bổ sung' WHERE code = 'TN1';
UPDATE trains SET description = 'Tàu địa phương kết nối đô thị' WHERE code LIKE 'SUB%';
UPDATE trains SET category = 'SE_TN' WHERE code IN ('SE1', 'SE5', 'TN1');
UPDATE trains SET category = 'HIGH_QUALITY' WHERE code = 'SE3';
UPDATE trains SET category = 'SUBURBAN' WHERE code LIKE 'SUB%';

UPDATE carriage_types SET name = 'Ghế mềm điều hòa', description = 'Chỗ ngồi bọc da thoải mái' WHERE code = 'SOFT_SEAT';
UPDATE carriage_types SET name = 'Ghế cứng', description = 'Chỗ ngồi tiết kiệm' WHERE code = 'HARD_SEAT';
UPDATE carriage_types SET name = 'Giường nằm khoang 6', description = 'Khoang 6 giường nằm tầng' WHERE code = 'SLEEPER_6';
UPDATE carriage_types SET name = 'Giường nằm khoang 4', description = 'Khoang 4 giường nằm chất lượng cao' WHERE code = 'SLEEPER_4';

UPDATE carriages SET name = 'Toa 1 - Ghế mềm' WHERE id = 1;
UPDATE carriages SET name = 'Toa 2 - Giường nằm' WHERE id = 2;
UPDATE carriages SET name = 'Toa 1 - Ghế cứng' WHERE id = 3;

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
('Vé Tết sum vầy', 'Ưu đãi sớm cho các chuyến tàu Tết trên trục Bắc - Nam.', 'TETSUMVAY', 'percent', 18, 250000, 500000, '2026-05-01', '2027-01-20', 'Áp dụng cho đơn từ 2 vé, đặt trước ngày khởi hành tối thiểu 7 ngày.', 'Ga Sài Gòn -> Ga Hà Nội', 'tet,popularRoute', 1000, 0, 84, 'ACTIVE'),
('Sinh viên lên tàu', 'Tiết kiệm cho sinh viên khi đặt vé ghế ngồi hoặc giường nằm.', 'SVRAIL', 'percent', 15, 150000, 200000, '2026-05-01', '2026-08-31', 'Cần xuất trình thẻ sinh viên còn hiệu lực khi lên tàu.', 'Ga Hà Nội -> Vinh', 'student', 1500, 0, 78, 'ACTIVE'),
('Đi về tiết kiệm hơn', 'Giảm trực tiếp cho hành trình khứ hồi trong cùng một đơn.', 'KHUTHOI', 'amount', 120000, NULL, 500000, '2026-04-15', '2026-07-15', 'Áp dụng khi đặt tối thiểu 1 vé chiều đi và 1 vé chiều về.', NULL, 'roundTrip', 800, 0, 88, 'ACTIVE'),
('Thanh toán online cuối tuần', 'Miễn phí dịch vụ khi thanh toán bằng ví điện tử hoặc thẻ nội địa.', 'PAYONLINE', 'serviceFee', 35000, NULL, 0, '2026-05-01', '2026-05-08', 'Áp dụng từ thứ Sáu đến Chủ nhật cho đơn thanh toán online.', NULL, 'onlinePayment,expiring', 1200, 0, 96, 'ACTIVE'),
('Gia đình chọn khoang', 'Ưu đãi cho nhóm gia đình đặt khoang 4 hoặc khoang 6.', 'GIADINH', 'amount', 200000, NULL, 1000000, '2026-05-10', '2026-09-30', 'Đơn từ 4 hành khách, áp dụng cho khoang 4 hoặc khoang 6.', 'Ga Đà Nẵng -> Ga Sài Gòn', 'group', 500, 0, 72, 'ACTIVE'),
('Tuyến biển miền Trung', 'Giảm mạnh cho tuyến Sài Gòn - Đà Nẵng trong mùa du lịch.', 'BIENXANH', 'percent', 20, 220000, 300000, '2026-05-01', '2026-06-30', 'Áp dụng cho vé từ thứ Hai đến thứ Năm, không cộng dồn ưu đãi.', 'Ga Sài Gòn -> Ga Đà Nẵng', 'popularRoute', 700, 0, 81, 'ACTIVE'),
('Chốt vé tháng 5', 'Ưu đãi ngắn ngày cho các chuyến còn nhiều ghế trống.', 'MAYLAST', 'amount', 80000, NULL, 300000, '2026-05-01', '2026-05-06', 'Số lượng mã có hạn, áp dụng cho đơn từ 300.000đ.', NULL, 'expiring,onlinePayment', 400, 0, 90, 'ACTIVE'),
('Nhóm bạn mùa hè', 'Càng đông càng tiết kiệm cho nhóm từ 6 hành khách.', 'NHOMHE', 'percent', 12, 180000, 600000, '2026-05-15', '2026-08-15', 'Áp dụng cho đơn từ 6 vé cùng chuyến, cùng hạng ghế.', NULL, 'group', 600, 0, 69, 'ACTIVE')
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
