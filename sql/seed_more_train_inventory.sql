USE vetautet;

INSERT INTO `trains` (`id`, `code`, `category`, `description`)
SELECT 3, 'SE5', 'SE_TN', 'Tau nhanh Bac Nam bo sung'
WHERE NOT EXISTS (SELECT 1 FROM `trains` WHERE `id` = 3);

INSERT INTO `trains` (`id`, `code`, `category`, `description`)
SELECT 4, 'TN1', 'SE_TN', 'Tau thong nhat dem bo sung'
WHERE NOT EXISTS (SELECT 1 FROM `trains` WHERE `id` = 4);

INSERT INTO `carriages` (`id`, `train_id`, `name`, `type_id`, `seat_layout`)
SELECT 4, 3, 'Toa 1 - Ghe Mem', 1, '{"rows": 12, "cols": 4}'
WHERE NOT EXISTS (SELECT 1 FROM `carriages` WHERE `id` = 4);

INSERT INTO `carriages` (`id`, `train_id`, `name`, `type_id`, `seat_layout`)
SELECT 5, 3, 'Toa 2 - Ghe Cung', 2, '{"rows": 12, "cols": 4}'
WHERE NOT EXISTS (SELECT 1 FROM `carriages` WHERE `id` = 5);

INSERT INTO `carriages` (`id`, `train_id`, `name`, `type_id`, `seat_layout`)
SELECT 6, 3, 'Toa 3 - Giuong 4', 4, '{"rows": 12, "cols": 4}'
WHERE NOT EXISTS (SELECT 1 FROM `carriages` WHERE `id` = 6);

INSERT INTO `carriages` (`id`, `train_id`, `name`, `type_id`, `seat_layout`)
SELECT 7, 4, 'Toa 1 - Ghe Mem', 1, '{"rows": 12, "cols": 4}'
WHERE NOT EXISTS (SELECT 1 FROM `carriages` WHERE `id` = 7);

INSERT INTO `carriages` (`id`, `train_id`, `name`, `type_id`, `seat_layout`)
SELECT 8, 4, 'Toa 2 - Giuong 6', 3, '{"rows": 12, "cols": 4}'
WHERE NOT EXISTS (SELECT 1 FROM `carriages` WHERE `id` = 8);

INSERT INTO `seats` (`carriage_id`, `seat_number`, `seat_type`)
SELECT c.id,
       CONCAT(CASE WHEN c.type_id IN (3, 4) THEN 'G' WHEN c.type_id = 2 THEN 'C' ELSE 'A' END, n.n),
       CASE WHEN c.type_id = 1 THEN 'SOFT_SEAT'
            WHEN c.type_id = 2 THEN 'HARD_SEAT'
            WHEN c.type_id = 3 THEN 'SLEEPER_6'
            ELSE 'SLEEPER_4'
       END
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
WHERE c.id BETWEEN 4 AND 8
  AND NOT EXISTS (
      SELECT 1 FROM seats s
      WHERE s.carriage_id = c.id
        AND s.seat_number = CONCAT(CASE WHEN c.type_id IN (3, 4) THEN 'G' WHEN c.type_id = 2 THEN 'C' ELSE 'A' END, n.n)
  );

INSERT INTO `trips` (`id`, `train_id`, `departure_station_id`, `arrival_station_id`, `departure_time`, `arrival_time`, `duration`, `status`) VALUES
(23, 3, 1, 3, '2026-05-11 06:00:00', '2026-05-12 08:00:00', 1560, 'SCHEDULED'),
(24, 3, 3, 1, '2026-05-11 19:30:00', '2026-05-12 21:30:00', 1560, 'SCHEDULED'),
(25, 3, 1, 2, '2026-05-12 07:15:00', '2026-05-12 23:15:00', 960, 'SCHEDULED'),
(26, 3, 2, 3, '2026-05-13 09:00:00', '2026-05-14 01:00:00', 960, 'SCHEDULED'),
(27, 4, 3, 1, '2026-05-14 20:00:00', '2026-05-15 22:00:00', 1560, 'SCHEDULED'),
(28, 4, 1, 3, '2026-05-15 05:45:00', '2026-05-16 07:45:00', 1560, 'SCHEDULED'),
(29, 4, 2, 1, '2026-05-16 08:30:00', '2026-05-17 00:30:00', 960, 'SCHEDULED'),
(30, 4, 1, 2, '2026-05-17 13:20:00', '2026-05-18 05:20:00', 960, 'SCHEDULED')
ON DUPLICATE KEY UPDATE
    departure_time = VALUES(departure_time),
    arrival_time = VALUES(arrival_time),
    duration = VALUES(duration),
    status = VALUES(status);

INSERT INTO `ticket_prices` (`trip_id`, `type_id`, `price`)
SELECT t.id,
       c.type_id,
       CASE
           WHEN c.type_id = 1 THEN 540000 + (MOD(t.id, 3) * 25000)
           WHEN c.type_id = 2 THEN 340000 + (MOD(t.id, 3) * 20000)
           WHEN c.type_id = 3 THEN 900000 + (MOD(t.id, 3) * 40000)
           WHEN c.type_id = 4 THEN 1300000 + (MOD(t.id, 3) * 50000)
           ELSE 450000
       END AS price
FROM trips t
JOIN carriages c ON c.train_id = t.train_id
WHERE t.id BETWEEN 23 AND 30
GROUP BY t.id, c.type_id
ON DUPLICATE KEY UPDATE price = VALUES(price);

INSERT INTO `tickets` (`trip_id`, `seat_id`, `price`, `status`)
SELECT t.id,
       s.id,
       CASE
           WHEN c.type_id = 1 THEN 540000 + (MOD(t.id, 3) * 25000)
           WHEN c.type_id = 2 THEN 340000 + (MOD(t.id, 3) * 20000)
           WHEN c.type_id = 3 THEN 900000 + (MOD(t.id, 3) * 40000)
           WHEN c.type_id = 4 THEN 1300000 + (MOD(t.id, 3) * 50000)
           ELSE 450000
       END AS price,
       'AVAILABLE'
FROM trips t
JOIN carriages c ON c.train_id = t.train_id
JOIN seats s ON s.carriage_id = c.id
WHERE t.id BETWEEN 23 AND 30
ON DUPLICATE KEY UPDATE price = VALUES(price);
