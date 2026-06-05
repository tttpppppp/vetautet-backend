USE vetautet;

CREATE TABLE IF NOT EXISTS `promotions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(160) NOT NULL,
    `description` TEXT,
    `code` VARCHAR(50) NOT NULL UNIQUE,
    `discount_type` VARCHAR(30) NOT NULL COMMENT 'percent, amount, serviceFee',
    `discount_value` DECIMAL(15,2) NOT NULL,
    `max_discount_amount` DECIMAL(15,2) DEFAULT NULL,
    `min_order_amount` DECIMAL(15,2) DEFAULT NULL,
    `starts_at` DATE NOT NULL,
    `ends_at` DATE NOT NULL,
    `conditions` TEXT,
    `route` VARCHAR(160) DEFAULT NULL,
    `categories` VARCHAR(255) DEFAULT NULL COMMENT 'Comma separated FE category keys',
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
