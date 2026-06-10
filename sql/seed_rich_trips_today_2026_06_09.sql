USE vetautet;

-- Rich demo inventory for the current test window:
-- 2026-06-09 (today), 2026-06-10 (tomorrow), 2026-06-11 (the day after).
-- The script is idempotent and can be re-run on the local database.

INSERT INTO `stations` (`name`, `code`, `location`) VALUES
('Ga Hà Nội', 'HAN', 'Hà Nội'),
('Ga Sài Gòn', 'SGN', 'TP. Hồ Chí Minh'),
('Ga Đà Nẵng', 'DAN', 'Đà Nẵng'),
('Ga Vinh', 'VIN', 'Nghệ An'),
('Ga Huế', 'HUE', 'Thừa Thiên Huế'),
('Ga Nha Trang', 'NTR', 'Khánh Hòa'),
('Ga Hải Phòng', 'HPH', 'Hải Phòng'),
('Ga Hải Dương', 'HDU', 'Hải Dương'),
('Ga Lào Cai', 'LCA', 'Lào Cai'),
('Ga Yên Bái', 'YBI', 'Yên Bái'),
('Ga Thái Nguyên', 'TNG', 'Thái Nguyên'),
('Ga Đồng Đăng', 'DDN', 'Lạng Sơn'),
('Ga Kép', 'KEP', 'Bắc Giang'),
('Ga Hạ Long', 'HLG', 'Quảng Ninh'),
('Ga Đồng Hới', 'DHO', 'Quảng Bình'),
('Ga Quy Nhơn', 'QNH', 'Bình Định'),
('Ga Tuy Hòa', 'THO', 'Phú Yên'),
('Ga Phan Thiết', 'PTH', 'Bình Thuận'),
('Ga Bình Thuận', 'BTH', 'Bình Thuận'),
('Ga Đà Lạt', 'DLA', 'Lâm Đồng'),
('Ga Trại Mát', 'TRA', 'Lâm Đồng'),
('Ga Ninh Bình', 'NBI', 'Ninh Bình'),
('Ga Thanh Hóa', 'THA', 'Thanh Hóa')
ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `location` = VALUES(`location`),
    `deleted_at` = NULL;

INSERT INTO `trains` (`code`, `category`, `description`) VALUES
('SE1', 'SE_TN', 'Tau Bac Nam Ha Noi - Sai Gon'),
('SE3', 'SE_TN', 'Tau Bac Nam Ha Noi - Sai Gon'),
('SE5', 'SE_TN', 'Tau Bac Nam Ha Noi - Sai Gon'),
('SE7', 'SE_TN', 'Tau Bac Nam Ha Noi - Sai Gon'),
('TN1', 'SE_TN', 'Tau Thong Nhat Bac Nam'),
('SE19', 'HIGH_QUALITY', 'Tau Ha Noi - Da Nang'),
('NA1', 'SE_TN', 'Tau Ha Noi - Vinh - Dong Hoi - Hue'),
('VD31', 'SE_TN', 'Tau Ha Noi - Dong Hoi'),
('DH41', 'SE_TN', 'Tau Ha Noi - Hue'),
('SP1', 'HIGH_QUALITY', 'Tau Ha Noi - Lao Cai'),
('SP3', 'HIGH_QUALITY', 'Tau Ha Noi - Lao Cai'),
('LC1', 'HIGH_QUALITY', 'Tau Ha Noi - Lao Cai'),
('LC3', 'HIGH_QUALITY', 'Tau Ha Noi - Lao Cai'),
('YB1', 'SUBURBAN', 'Tau Ha Noi - Yen Bai - Lao Cai'),
('HP1', 'SUBURBAN', 'Tau Ha Noi - Hai Phong'),
('LP3', 'SUBURBAN', 'Tau Ha Noi - Hai Phong'),
('LP5', 'SUBURBAN', 'Tau Ha Noi - Hai Phong'),
('LP7', 'SUBURBAN', 'Tau Ha Noi - Hai Phong'),
('QT91', 'SUBURBAN', 'Tau Ha Noi - Thai Nguyen'),
('DD3', 'SUBURBAN', 'Tau Ha Noi - Dong Dang'),
('R157', 'SUBURBAN', 'Tau Kep - Ha Long'),
('SE21', 'HIGH_QUALITY', 'Tau Sai Gon - Hue'),
('SE25', 'HIGH_QUALITY', 'Tau Sai Gon - Quy Nhon'),
('SNT1', 'HIGH_QUALITY', 'Tau Sai Gon - Nha Trang'),
('N11', 'HIGH_QUALITY', 'Tau Sai Gon - Nha Trang - Tuy Hoa'),
('SPT1', 'SUBURBAN', 'Tau Sai Gon - Phan Thiet'),
('PT3', 'SUBURBAN', 'Tau Sai Gon - Phan Thiet')
ON DUPLICATE KEY UPDATE
    `category` = VALUES(`category`),
    `description` = VALUES(`description`),
    `deleted_at` = NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_numbers`;
CREATE TEMPORARY TABLE `tmp_seed_numbers` (`n` INT NOT NULL PRIMARY KEY);
INSERT INTO `tmp_seed_numbers` (`n`) VALUES
(1),(2),(3),(4),(5),(6),(7),(8),(9),(10),
(11),(12),(13),(14),(15),(16),(17),(18),(19),(20),
(21),(22),(23),(24),(25),(26),(27),(28),(29),(30),
(31),(32),(33),(34),(35),(36),(37),(38),(39),(40),
(41),(42),(43),(44),(45),(46),(47),(48),(49),(50);

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_train_codes`;
CREATE TEMPORARY TABLE `tmp_seed_train_codes` (`train_code` VARCHAR(20) NOT NULL PRIMARY KEY);
INSERT INTO `tmp_seed_train_codes` (`train_code`) VALUES
('SE1'),('SE3'),('SE5'),('SE7'),('TN1'),('SE19'),('NA1'),('VD31'),('DH41'),
('SP1'),('SP3'),('LC1'),('LC3'),('YB1'),('HP1'),('LP3'),('LP5'),('LP7'),
('QT91'),('DD3'),('R157'),('SE21'),('SE25'),('SNT1'),('N11'),('SPT1'),('PT3');

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_carriage_templates`;
CREATE TEMPORARY TABLE `tmp_seed_carriage_templates` (
    `type_code` VARCHAR(50) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `seat_prefix` VARCHAR(5) NOT NULL
);
INSERT INTO `tmp_seed_carriage_templates` (`type_code`, `name`, `seat_prefix`) VALUES
('SOFT_SEAT', 'Toa 1 - Ghe mem dieu hoa', 'A'),
('HARD_SEAT', 'Toa 2 - Ghe cung dieu hoa', 'C'),
('SLEEPER_4', 'Toa 3 - Giuong nam khoang 4', 'G');

INSERT INTO `carriages` (`train_id`, `name`, `type_id`, `seat_layout`)
SELECT tr.id,
       tpl.name,
       ct.id,
       JSON_OBJECT('rows', 10, 'cols', 5)
FROM `tmp_seed_train_codes` tc
JOIN `trains` tr ON tr.code = tc.train_code
JOIN `tmp_seed_carriage_templates` tpl
JOIN `carriage_types` ct ON ct.code = tpl.type_code
WHERE NOT EXISTS (
    SELECT 1
    FROM `carriages` c
    WHERE c.train_id = tr.id
      AND c.type_id = ct.id
      AND c.deleted_at IS NULL
);

INSERT INTO `seats` (`carriage_id`, `seat_number`, `seat_type`)
SELECT c.id,
       CONCAT(tpl.seat_prefix, n.n),
       ct.code
FROM `tmp_seed_train_codes` tc
JOIN `trains` tr ON tr.code = tc.train_code
JOIN `carriages` c ON c.train_id = tr.id AND c.deleted_at IS NULL
JOIN `carriage_types` ct ON ct.id = c.type_id
JOIN `tmp_seed_carriage_templates` tpl ON tpl.type_code = ct.code
JOIN `tmp_seed_numbers` n ON n.n <= 50
WHERE NOT EXISTS (
    SELECT 1
    FROM `seats` s
    WHERE s.carriage_id = c.id
      AND s.seat_number = CONCAT(tpl.seat_prefix, n.n)
      AND s.deleted_at IS NULL
);

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_rich_trips`;
CREATE TEMPORARY TABLE `tmp_seed_rich_trips` (
    `route_key` VARCHAR(50) NOT NULL,
    `train_code` VARCHAR(20) NOT NULL,
    `departure_code` VARCHAR(20) NOT NULL,
    `arrival_code` VARCHAR(20) NOT NULL,
    `departure_time` DATETIME NOT NULL,
    `arrival_time` DATETIME NOT NULL,
    `status` VARCHAR(30) NOT NULL
);

INSERT INTO `tmp_seed_rich_trips`
(`route_key`, `train_code`, `departure_code`, `arrival_code`, `departure_time`, `arrival_time`, `status`) VALUES
-- 2026-06-09
('HAN_HPH', 'HP1', 'HAN', 'HPH', '2026-06-09 06:00:00', '2026-06-09 08:25:00', 'SCHEDULED'),
('HAN_HPH', 'LP3', 'HAN', 'HPH', '2026-06-09 09:25:00', '2026-06-09 12:00:00', 'SCHEDULED'),
('HAN_HPH', 'LP5', 'HAN', 'HPH', '2026-06-09 15:15:00', '2026-06-09 18:00:00', 'SCHEDULED'),
('HAN_VIN', 'SE1', 'HAN', 'VIN', '2026-06-09 08:00:00', '2026-06-09 13:05:00', 'SCHEDULED'),
('HAN_VIN', 'NA1', 'HAN', 'VIN', '2026-06-09 22:15:00', '2026-06-10 03:30:00', 'SCHEDULED'),
('HAN_DAN', 'SE19', 'HAN', 'DAN', '2026-06-09 19:30:00', '2026-06-10 10:50:00', 'SCHEDULED'),
('HAN_SGN', 'SE1', 'HAN', 'SGN', '2026-06-09 06:00:00', '2026-06-10 16:20:00', 'SCHEDULED'),
('HAN_LCA', 'SP1', 'HAN', 'LCA', '2026-06-09 21:35:00', '2026-06-10 05:35:00', 'SCHEDULED'),
('HAN_TNG', 'QT91', 'HAN', 'TNG', '2026-06-09 06:40:00', '2026-06-09 08:55:00', 'SCHEDULED'),
('HAN_DDN', 'DD3', 'HAN', 'DDN', '2026-06-09 07:30:00', '2026-06-09 12:10:00', 'SCHEDULED'),
('HAN_DHO', 'VD31', 'HAN', 'DHO', '2026-06-09 19:45:00', '2026-06-10 06:20:00', 'SCHEDULED'),
('HAN_HUE', 'DH41', 'HAN', 'HUE', '2026-06-09 20:20:00', '2026-06-10 09:00:00', 'SCHEDULED'),
('KEP_HLG', 'R157', 'KEP', 'HLG', '2026-06-09 13:00:00', '2026-06-09 15:00:00', 'SCHEDULED'),
('SGN_HUE', 'SE21', 'SGN', 'HUE', '2026-06-09 07:10:00', '2026-06-10 04:30:00', 'SCHEDULED'),
('SGN_PTH', 'SPT1', 'SGN', 'PTH', '2026-06-09 06:10:00', '2026-06-09 10:20:00', 'SCHEDULED'),
('SGN_PTH', 'PT3', 'SGN', 'PTH', '2026-06-09 14:20:00', '2026-06-09 18:35:00', 'SCHEDULED'),
('SGN_NTR', 'SNT1', 'SGN', 'NTR', '2026-06-09 20:30:00', '2026-06-10 05:15:00', 'SCHEDULED'),
('SGN_QNH', 'SE25', 'SGN', 'QNH', '2026-06-09 19:00:00', '2026-06-10 09:10:00', 'SCHEDULED'),
('DLA_TRA', 'N11', 'DLA', 'TRA', '2026-06-09 08:10:00', '2026-06-09 08:40:00', 'SCHEDULED'),
('SGN_HAN', 'SE3', 'SGN', 'HAN', '2026-06-09 19:30:00', '2026-06-11 05:45:00', 'SCHEDULED'),
-- 2026-06-10
('HAN_HPH', 'HP1', 'HAN', 'HPH', '2026-06-10 06:00:00', '2026-06-10 08:25:00', 'SCHEDULED'),
('HAN_HPH', 'LP3', 'HAN', 'HPH', '2026-06-10 09:25:00', '2026-06-10 12:00:00', 'SCHEDULED'),
('HAN_HPH', 'LP5', 'HAN', 'HPH', '2026-06-10 15:15:00', '2026-06-10 18:00:00', 'SCHEDULED'),
('HAN_HPH', 'LP7', 'HAN', 'HPH', '2026-06-10 18:00:00', '2026-06-10 20:45:00', 'SCHEDULED'),
('HAN_VIN', 'SE3', 'HAN', 'VIN', '2026-06-10 09:00:00', '2026-06-10 14:05:00', 'SCHEDULED'),
('HAN_VIN', 'SE5', 'HAN', 'VIN', '2026-06-10 13:20:00', '2026-06-10 18:50:00', 'SCHEDULED'),
('HAN_DHO', 'VD31', 'HAN', 'DHO', '2026-06-10 19:45:00', '2026-06-11 06:20:00', 'SCHEDULED'),
('HAN_HUE', 'DH41', 'HAN', 'HUE', '2026-06-10 20:20:00', '2026-06-11 09:00:00', 'SCHEDULED'),
('HAN_SGN', 'SE1', 'HAN', 'SGN', '2026-06-10 08:00:00', '2026-06-11 18:20:00', 'SCHEDULED'),
('HAN_LCA', 'SP3', 'HAN', 'LCA', '2026-06-10 22:00:00', '2026-06-11 06:05:00', 'SCHEDULED'),
('HAN_LCA', 'LC1', 'HAN', 'LCA', '2026-06-10 06:10:00', '2026-06-10 14:10:00', 'SCHEDULED'),
('HAN_TNG', 'QT91', 'HAN', 'TNG', '2026-06-10 06:40:00', '2026-06-10 08:55:00', 'SCHEDULED'),
('HAN_DDN', 'DD3', 'HAN', 'DDN', '2026-06-10 07:30:00', '2026-06-10 12:10:00', 'SCHEDULED'),
('KEP_HLG', 'R157', 'KEP', 'HLG', '2026-06-10 13:00:00', '2026-06-10 15:00:00', 'SCHEDULED'),
('SGN_HUE', 'SE21', 'SGN', 'HUE', '2026-06-10 07:10:00', '2026-06-11 04:30:00', 'SCHEDULED'),
('SGN_PTH', 'SPT1', 'SGN', 'PTH', '2026-06-10 06:20:00', '2026-06-10 10:35:00', 'SCHEDULED'),
('SGN_PTH', 'PT3', 'SGN', 'PTH', '2026-06-10 15:10:00', '2026-06-10 19:25:00', 'SCHEDULED'),
('SGN_NTR', 'SNT1', 'SGN', 'NTR', '2026-06-10 20:45:00', '2026-06-11 05:30:00', 'SCHEDULED'),
('SGN_QNH', 'SE25', 'SGN', 'QNH', '2026-06-10 19:15:00', '2026-06-11 09:25:00', 'SCHEDULED'),
('SGN_THO', 'N11', 'SGN', 'THO', '2026-06-10 18:00:00', '2026-06-11 05:30:00', 'SCHEDULED'),
('DLA_TRA', 'N11', 'DLA', 'TRA', '2026-06-10 09:00:00', '2026-06-10 09:30:00', 'SCHEDULED'),
-- 2026-06-11
('HAN_HPH', 'HP1', 'HAN', 'HPH', '2026-06-11 06:00:00', '2026-06-11 08:25:00', 'SCHEDULED'),
('HAN_HPH', 'LP3', 'HAN', 'HPH', '2026-06-11 09:25:00', '2026-06-11 12:00:00', 'SCHEDULED'),
('HAN_HPH', 'LP5', 'HAN', 'HPH', '2026-06-11 15:15:00', '2026-06-11 18:00:00', 'SCHEDULED'),
('HAN_HPH', 'LP7', 'HAN', 'HPH', '2026-06-11 18:00:00', '2026-06-11 20:45:00', 'SCHEDULED'),
('HAN_VIN', 'SE5', 'HAN', 'VIN', '2026-06-11 07:10:00', '2026-06-11 12:30:00', 'SCHEDULED'),
('HAN_DHO', 'NA1', 'HAN', 'DHO', '2026-06-11 19:20:00', '2026-06-12 06:00:00', 'SCHEDULED'),
('HAN_HUE', 'DH41', 'HAN', 'HUE', '2026-06-11 20:10:00', '2026-06-12 09:10:00', 'SCHEDULED'),
('HAN_SGN', 'SE7', 'HAN', 'SGN', '2026-06-11 06:20:00', '2026-06-12 16:50:00', 'SCHEDULED'),
('HAN_LCA', 'LC3', 'HAN', 'LCA', '2026-06-11 21:40:00', '2026-06-12 05:50:00', 'SCHEDULED'),
('HAN_LCA', 'YB1', 'HAN', 'LCA', '2026-06-11 13:10:00', '2026-06-11 21:05:00', 'SCHEDULED'),
('HAN_DDN', 'DD3', 'HAN', 'DDN', '2026-06-11 07:30:00', '2026-06-11 12:10:00', 'SCHEDULED'),
('KEP_HLG', 'R157', 'KEP', 'HLG', '2026-06-11 13:00:00', '2026-06-11 15:00:00', 'SCHEDULED'),
('SGN_HUE', 'SE21', 'SGN', 'HUE', '2026-06-11 07:30:00', '2026-06-12 04:50:00', 'SCHEDULED'),
('SGN_QNH', 'SE25', 'SGN', 'QNH', '2026-06-11 19:20:00', '2026-06-12 09:40:00', 'SCHEDULED'),
('SGN_PTH', 'PT3', 'SGN', 'PTH', '2026-06-11 14:40:00', '2026-06-11 18:55:00', 'SCHEDULED'),
('SGN_PTH', 'SPT1', 'SGN', 'PTH', '2026-06-11 06:30:00', '2026-06-11 10:45:00', 'SCHEDULED'),
('SGN_NTR', 'SNT1', 'SGN', 'NTR', '2026-06-11 20:50:00', '2026-06-12 05:40:00', 'SCHEDULED'),
('SGN_THO', 'N11', 'SGN', 'THO', '2026-06-11 18:10:00', '2026-06-12 05:45:00', 'SCHEDULED'),
('DLA_TRA', 'N11', 'DLA', 'TRA', '2026-06-11 08:20:00', '2026-06-11 08:50:00', 'SCHEDULED');

INSERT INTO `trips`
(`train_id`, `departure_station_id`, `arrival_station_id`, `departure_time`, `arrival_time`, `service_date`,
 `estimated_departure_time`, `estimated_arrival_time`, `duration`, `status`)
SELECT tr.id,
       dep.id,
       arr.id,
       dt.departure_time,
       dt.arrival_time,
       DATE(dt.departure_time),
       dt.departure_time,
       dt.arrival_time,
       TIMESTAMPDIFF(MINUTE, dt.departure_time, dt.arrival_time),
       dt.status
FROM `tmp_seed_rich_trips` dt
JOIN `trains` tr ON tr.code = dt.train_code
JOIN `stations` dep ON dep.code = dt.departure_code
JOIN `stations` arr ON arr.code = dt.arrival_code
WHERE NOT EXISTS (
    SELECT 1
    FROM `trips` t
    WHERE t.train_id = tr.id
      AND t.departure_station_id = dep.id
      AND t.arrival_station_id = arr.id
      AND t.departure_time = dt.departure_time
      AND t.deleted_at IS NULL
);

UPDATE `trips` t
JOIN `trains` tr ON tr.id = t.train_id
JOIN `stations` dep ON dep.id = t.departure_station_id
JOIN `stations` arr ON arr.id = t.arrival_station_id
JOIN `tmp_seed_rich_trips` dt
  ON dt.train_code = tr.code
 AND dt.departure_code = dep.code
 AND dt.arrival_code = arr.code
 AND dt.departure_time = t.departure_time
SET t.arrival_time = dt.arrival_time,
    t.service_date = DATE(dt.departure_time),
    t.estimated_departure_time = dt.departure_time,
    t.estimated_arrival_time = dt.arrival_time,
    t.duration = TIMESTAMPDIFF(MINUTE, dt.departure_time, dt.arrival_time),
    t.status = dt.status,
    t.deleted_at = NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_trip_ids`;
CREATE TEMPORARY TABLE `tmp_seed_trip_ids` AS
SELECT t.id AS trip_id,
       dt.route_key,
       dt.train_code,
       dt.departure_code,
       dt.arrival_code,
       dt.departure_time,
       dt.arrival_time
FROM `tmp_seed_rich_trips` dt
JOIN `trains` tr ON tr.code = dt.train_code
JOIN `stations` dep ON dep.code = dt.departure_code
JOIN `stations` arr ON arr.code = dt.arrival_code
JOIN `trips` t
  ON t.train_id = tr.id
 AND t.departure_station_id = dep.id
 AND t.arrival_station_id = arr.id
 AND t.departure_time = dt.departure_time
 AND t.deleted_at IS NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_route_stops`;
CREATE TEMPORARY TABLE `tmp_seed_route_stops` (
    `route_key` VARCHAR(50) NOT NULL,
    `station_code` VARCHAR(20) NOT NULL,
    `stop_order` INT NOT NULL,
    `offset_minutes` INT NOT NULL,
    `distance_km` DECIMAL(10,2) NOT NULL,
    `platform` VARCHAR(20) NULL
);

INSERT INTO `tmp_seed_route_stops`
(`route_key`, `station_code`, `stop_order`, `offset_minutes`, `distance_km`, `platform`) VALUES
('HAN_HPH', 'HAN', 1, 0, 0, '1'), ('HAN_HPH', 'HDU', 2, 75, 57, '2'), ('HAN_HPH', 'HPH', 3, 145, 102, '3'),
('HAN_VIN', 'HAN', 1, 0, 0, '1'), ('HAN_VIN', 'NBI', 2, 90, 115, '2'), ('HAN_VIN', 'THA', 3, 180, 175, '1'), ('HAN_VIN', 'VIN', 4, 305, 319, '2'),
('HAN_DAN', 'HAN', 1, 0, 0, '2'), ('HAN_DAN', 'VIN', 2, 300, 319, '1'), ('HAN_DAN', 'HUE', 3, 760, 688, '2'), ('HAN_DAN', 'DAN', 4, 930, 791, '3'),
('HAN_DHO', 'HAN', 1, 0, 0, '1'), ('HAN_DHO', 'VIN', 2, 320, 319, '2'), ('HAN_DHO', 'DHO', 3, 640, 522, '1'),
('HAN_HUE', 'HAN', 1, 0, 0, '1'), ('HAN_HUE', 'VIN', 2, 315, 319, '2'), ('HAN_HUE', 'DHO', 3, 620, 522, '1'), ('HAN_HUE', 'HUE', 4, 780, 688, '2'),
('HAN_SGN', 'HAN', 1, 0, 0, '1'), ('HAN_SGN', 'VIN', 2, 305, 319, '2'), ('HAN_SGN', 'HUE', 3, 745, 688, '1'), ('HAN_SGN', 'DAN', 4, 890, 791, '3'), ('HAN_SGN', 'NTR', 5, 1475, 1315, '2'), ('HAN_SGN', 'SGN', 6, 2060, 1726, '5'),
('SGN_HAN', 'SGN', 1, 0, 0, '5'), ('SGN_HAN', 'NTR', 2, 570, 411, '2'), ('SGN_HAN', 'DAN', 3, 1220, 935, '1'), ('SGN_HAN', 'HUE', 4, 1360, 1038, '2'), ('SGN_HAN', 'VIN', 5, 1810, 1407, '1'), ('SGN_HAN', 'HAN', 6, 2060, 1726, '3'),
('HAN_LCA', 'HAN', 1, 0, 0, '1'), ('HAN_LCA', 'YBI', 2, 260, 155, '2'), ('HAN_LCA', 'LCA', 3, 480, 296, '1'),
('HAN_TNG', 'HAN', 1, 0, 0, '1'), ('HAN_TNG', 'TNG', 2, 135, 75, '2'),
('HAN_DDN', 'HAN', 1, 0, 0, '1'), ('HAN_DDN', 'KEP', 2, 125, 67, '2'), ('HAN_DDN', 'DDN', 3, 280, 162, '1'),
('KEP_HLG', 'KEP', 1, 0, 0, '1'), ('KEP_HLG', 'HLG', 2, 120, 83, '2'),
('SGN_PTH', 'SGN', 1, 0, 0, '5'), ('SGN_PTH', 'BTH', 2, 190, 165, '2'), ('SGN_PTH', 'PTH', 3, 260, 198, '1'),
('SGN_NTR', 'SGN', 1, 0, 0, '5'), ('SGN_NTR', 'BTH', 2, 240, 165, '2'), ('SGN_NTR', 'NTR', 3, 525, 411, '2'),
('SGN_QNH', 'SGN', 1, 0, 0, '5'), ('SGN_QNH', 'NTR', 2, 525, 411, '2'), ('SGN_QNH', 'THO', 3, 680, 528, '1'), ('SGN_QNH', 'QNH', 4, 850, 650, '3'),
('SGN_HUE', 'SGN', 1, 0, 0, '5'), ('SGN_HUE', 'NTR', 2, 525, 411, '2'), ('SGN_HUE', 'DAN', 3, 1100, 935, '1'), ('SGN_HUE', 'HUE', 4, 1280, 1038, '2'),
('SGN_THO', 'SGN', 1, 0, 0, '5'), ('SGN_THO', 'NTR', 2, 525, 411, '2'), ('SGN_THO', 'THO', 3, 690, 528, '1'),
('DLA_TRA', 'DLA', 1, 0, 0, '1'), ('DLA_TRA', 'TRA', 2, 30, 7, '1');

INSERT INTO `trip_stops`
(`trip_id`, `station_id`, `stop_order`, `scheduled_arrival_time`, `scheduled_departure_time`,
 `estimated_arrival_time`, `estimated_departure_time`, `distance_from_origin_km`, `status`, `platform`, `note`)
SELECT ids.trip_id,
       st.id,
       rs.stop_order,
       CASE
           WHEN rs.stop_order = 1 THEN NULL
           WHEN rs.station_code = ids.arrival_code THEN ids.arrival_time
           ELSE TIMESTAMPADD(MINUTE, rs.offset_minutes, ids.departure_time)
       END,
       CASE
           WHEN rs.station_code = ids.arrival_code THEN NULL
           WHEN rs.stop_order = 1 THEN ids.departure_time
           ELSE TIMESTAMPADD(MINUTE, rs.offset_minutes + 10, ids.departure_time)
       END,
       CASE
           WHEN rs.stop_order = 1 THEN NULL
           WHEN rs.station_code = ids.arrival_code THEN ids.arrival_time
           ELSE TIMESTAMPADD(MINUTE, rs.offset_minutes, ids.departure_time)
       END,
       CASE
           WHEN rs.station_code = ids.arrival_code THEN NULL
           WHEN rs.stop_order = 1 THEN ids.departure_time
           ELSE TIMESTAMPADD(MINUTE, rs.offset_minutes + 10, ids.departure_time)
       END,
       rs.distance_km,
       'SCHEDULED',
       rs.platform,
       NULL
FROM `tmp_seed_trip_ids` ids
JOIN `tmp_seed_route_stops` rs ON rs.route_key = ids.route_key
JOIN `stations` st ON st.code = rs.station_code
ON DUPLICATE KEY UPDATE
    `scheduled_arrival_time` = VALUES(`scheduled_arrival_time`),
    `scheduled_departure_time` = VALUES(`scheduled_departure_time`),
    `estimated_arrival_time` = VALUES(`estimated_arrival_time`),
    `estimated_departure_time` = VALUES(`estimated_departure_time`),
    `distance_from_origin_km` = VALUES(`distance_from_origin_km`),
    `status` = VALUES(`status`),
    `platform` = VALUES(`platform`);

INSERT INTO `trip_segments` (`trip_id`, `from_stop_id`, `to_stop_id`, `segment_order`, `distance_km`, `status`)
SELECT s1.trip_id,
       s1.id,
       s2.id,
       s1.stop_order,
       GREATEST(COALESCE(s2.distance_from_origin_km, 0) - COALESCE(s1.distance_from_origin_km, 0), 0),
       'SCHEDULED'
FROM `trip_stops` s1
JOIN `trip_stops` s2 ON s2.trip_id = s1.trip_id AND s2.stop_order = s1.stop_order + 1
JOIN `tmp_seed_trip_ids` ids ON ids.trip_id = s1.trip_id
ON DUPLICATE KEY UPDATE
    `from_stop_id` = VALUES(`from_stop_id`),
    `to_stop_id` = VALUES(`to_stop_id`),
    `distance_km` = VALUES(`distance_km`),
    `status` = VALUES(`status`);

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_passenger_multiplier`;
CREATE TEMPORARY TABLE `tmp_seed_passenger_multiplier` (
    `passenger_type` VARCHAR(30) NOT NULL PRIMARY KEY,
    `multiplier` DECIMAL(5,2) NOT NULL
);
INSERT INTO `tmp_seed_passenger_multiplier` (`passenger_type`, `multiplier`)
SELECT `passenger_type`, `fare_multiplier`
FROM `passenger_fare_rules`
WHERE `status` = 'ACTIVE';

INSERT INTO `trip_segment_prices` (`segment_id`, `carriage_type_id`, `passenger_type`, `price`, `currency`, `status`)
SELECT seg.id,
       ct.id,
       pm.passenger_type,
       CEIL(GREATEST(
           CASE
               WHEN ct.code = 'HARD_SEAT' THEN 35000
               WHEN ct.code = 'SLEEPER_4' THEN 65000
               ELSE 45000
           END,
           seg.distance_km *
           CASE
               WHEN ct.code = 'HARD_SEAT' THEN 650
               WHEN ct.code = 'SLEEPER_4' THEN 1350
               ELSE 900
           END *
           pm.multiplier
       ) / 1000) * 1000,
       'VND',
       'ACTIVE'
FROM `tmp_seed_trip_ids` ids
JOIN `trips` t ON t.id = ids.trip_id
JOIN `trip_segments` seg ON seg.trip_id = t.id
JOIN `carriages` c ON c.train_id = t.train_id AND c.deleted_at IS NULL
JOIN `carriage_types` ct ON ct.id = c.type_id
JOIN `tmp_seed_passenger_multiplier` pm
GROUP BY seg.id, ct.id, pm.passenger_type
ON DUPLICATE KEY UPDATE
    `price` = VALUES(`price`),
    `currency` = VALUES(`currency`),
    `status` = VALUES(`status`);

INSERT INTO `ticket_prices` (`trip_id`, `type_id`, `price`)
SELECT seg.trip_id,
       p.carriage_type_id,
       SUM(p.price)
FROM `tmp_seed_trip_ids` ids
JOIN `trip_segments` seg ON seg.trip_id = ids.trip_id
JOIN `trip_segment_prices` p ON p.segment_id = seg.id AND p.passenger_type = 'ADULT'
GROUP BY seg.trip_id, p.carriage_type_id
ON DUPLICATE KEY UPDATE
    `price` = VALUES(`price`);

INSERT INTO `tickets` (`trip_id`, `seat_id`, `price`, `status`)
SELECT ids.trip_id,
       s.id,
       tp.price,
       CASE
           WHEN MOD(ids.trip_id + s.id, 31) = 0 THEN 'BOOKED'
           WHEN MOD(ids.trip_id + s.id, 47) = 0 THEN 'BOOKED'
           ELSE 'AVAILABLE'
       END
FROM `tmp_seed_trip_ids` ids
JOIN `trips` t ON t.id = ids.trip_id
JOIN `carriages` c ON c.train_id = t.train_id AND c.deleted_at IS NULL
JOIN `seats` s ON s.carriage_id = c.id AND s.deleted_at IS NULL
JOIN `ticket_prices` tp ON tp.trip_id = ids.trip_id AND tp.type_id = c.type_id
ON DUPLICATE KEY UPDATE
    `price` = VALUES(`price`),
    `status` = IF(`tickets`.`status` = 'AVAILABLE', VALUES(`status`), `tickets`.`status`);

INSERT INTO `seat_segment_inventory` (`trip_id`, `segment_id`, `seat_id`, `status`, `hold_expired_at`, `booking_detail_id`)
SELECT seg.trip_id,
       seg.id,
       s.id,
       CASE
           WHEN tk.status = 'BOOKED' THEN 'BOOKED'
           ELSE 'AVAILABLE'
       END,
       NULL,
       NULL
FROM `tmp_seed_trip_ids` ids
JOIN `trips` t ON t.id = ids.trip_id
JOIN `trip_segments` seg ON seg.trip_id = t.id
JOIN `carriages` c ON c.train_id = t.train_id AND c.deleted_at IS NULL
JOIN `seats` s ON s.carriage_id = c.id AND s.deleted_at IS NULL
LEFT JOIN `tickets` tk ON tk.trip_id = t.id AND tk.seat_id = s.id
ON DUPLICATE KEY UPDATE
    `status` = IF(`seat_segment_inventory`.`status` = 'AVAILABLE', VALUES(`status`), `seat_segment_inventory`.`status`);

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_passenger_multiplier`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_route_stops`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_trip_ids`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_rich_trips`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_carriage_templates`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_train_codes`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_numbers`;
