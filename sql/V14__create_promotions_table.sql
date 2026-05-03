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
