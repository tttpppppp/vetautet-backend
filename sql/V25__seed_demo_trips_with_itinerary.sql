USE vetautet;

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
