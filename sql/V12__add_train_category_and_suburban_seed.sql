USE vetautet;

ALTER TABLE `trains`
    ADD COLUMN IF NOT EXISTS `category` VARCHAR(50) NOT NULL DEFAULT 'SE_TN' AFTER `code`;

UPDATE `trains` SET `category` = 'SE_TN' WHERE `code` IN ('SE1', 'SE5', 'TN1');
UPDATE `trains` SET `category` = 'HIGH_QUALITY' WHERE `code` = 'SE3';
UPDATE `trains` SET `category` = 'SUBURBAN' WHERE `code` LIKE 'SUB%';

INSERT INTO `trains` (`id`, `code`, `category`, `description`)
SELECT 5, 'SUB1', 'SUBURBAN', 'Tau dia phuong ket noi do thi'
WHERE NOT EXISTS (SELECT 1 FROM `trains` WHERE `id` = 5);

UPDATE `trains`
SET `category` = 'SUBURBAN',
    `description` = 'Tau dia phuong ket noi do thi'
WHERE `code` = 'SUB1';

INSERT INTO `carriages` (`id`, `train_id`, `name`, `type_id`, `seat_layout`)
SELECT 9, 5, 'Toa 1 - Ghe Mem', 1, '{"rows": 10, "cols": 4}'
WHERE NOT EXISTS (SELECT 1 FROM `carriages` WHERE `id` = 9);

INSERT INTO `carriages` (`id`, `train_id`, `name`, `type_id`, `seat_layout`)
SELECT 10, 5, 'Toa 2 - Ghe Cung', 2, '{"rows": 10, "cols": 4}'
WHERE NOT EXISTS (SELECT 1 FROM `carriages` WHERE `id` = 10);

INSERT INTO `seats` (`carriage_id`, `seat_number`, `seat_type`)
SELECT c.id,
       CONCAT(CASE WHEN c.type_id = 2 THEN 'C' ELSE 'A' END, n.n),
       CASE WHEN c.type_id = 2 THEN 'HARD_SEAT' ELSE 'SOFT_SEAT' END
FROM carriages c
JOIN (
    SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
    UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
    UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
    UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
    UNION ALL SELECT 31 UNION ALL SELECT 32 UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35
    UNION ALL SELECT 36 UNION ALL SELECT 37 UNION ALL SELECT 38 UNION ALL SELECT 39 UNION ALL SELECT 40
) n
WHERE c.id IN (9, 10)
  AND NOT EXISTS (
      SELECT 1 FROM seats s
      WHERE s.carriage_id = c.id
        AND s.seat_number = CONCAT(CASE WHEN c.type_id = 2 THEN 'C' ELSE 'A' END, n.n)
  );

INSERT INTO `trips` (`id`, `train_id`, `departure_station_id`, `arrival_station_id`, `departure_time`, `arrival_time`, `duration`, `status`) VALUES
(31, 5, 1, 2, '2026-05-18 06:00:00', '2026-05-18 14:00:00', 480, 'SCHEDULED'),
(32, 5, 2, 1, '2026-05-18 15:30:00', '2026-05-18 23:30:00', 480, 'SCHEDULED'),
(33, 5, 1, 2, '2026-05-19 07:00:00', '2026-05-19 15:00:00', 480, 'SCHEDULED'),
(34, 5, 2, 1, '2026-05-19 16:30:00', '2026-05-20 00:30:00', 480, 'SCHEDULED')
ON DUPLICATE KEY UPDATE
    train_id = VALUES(train_id),
    departure_station_id = VALUES(departure_station_id),
    arrival_station_id = VALUES(arrival_station_id),
    departure_time = VALUES(departure_time),
    arrival_time = VALUES(arrival_time),
    duration = VALUES(duration),
    status = VALUES(status);

INSERT INTO `ticket_prices` (`trip_id`, `type_id`, `price`)
SELECT t.id,
       c.type_id,
       CASE
           WHEN c.type_id = 1 THEN 220000 + (MOD(t.id, 2) * 20000)
           WHEN c.type_id = 2 THEN 150000 + (MOD(t.id, 2) * 15000)
           ELSE 180000
       END AS price
FROM trips t
JOIN carriages c ON c.train_id = t.train_id
WHERE t.id BETWEEN 31 AND 34
GROUP BY t.id, c.type_id
ON DUPLICATE KEY UPDATE price = VALUES(price);

INSERT INTO `tickets` (`trip_id`, `seat_id`, `price`, `status`)
SELECT t.id,
       s.id,
       CASE
           WHEN c.type_id = 1 THEN 220000 + (MOD(t.id, 2) * 20000)
           WHEN c.type_id = 2 THEN 150000 + (MOD(t.id, 2) * 15000)
           ELSE 180000
       END AS price,
       'AVAILABLE'
FROM trips t
JOIN carriages c ON c.train_id = t.train_id
JOIN seats s ON s.carriage_id = c.id
WHERE t.id BETWEEN 31 AND 34
ON DUPLICATE KEY UPDATE price = VALUES(price);
