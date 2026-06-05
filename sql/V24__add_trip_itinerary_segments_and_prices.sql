USE vetautet;

ALTER TABLE `trips`
    ADD COLUMN IF NOT EXISTS `service_date` DATE DEFAULT NULL AFTER `arrival_time`,
    ADD COLUMN IF NOT EXISTS `estimated_departure_time` DATETIME DEFAULT NULL AFTER `service_date`,
    ADD COLUMN IF NOT EXISTS `estimated_arrival_time` DATETIME DEFAULT NULL AFTER `estimated_departure_time`,
    ADD COLUMN IF NOT EXISTS `actual_departure_time` DATETIME DEFAULT NULL AFTER `estimated_arrival_time`,
    ADD COLUMN IF NOT EXISTS `actual_arrival_time` DATETIME DEFAULT NULL AFTER `actual_departure_time`;

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
