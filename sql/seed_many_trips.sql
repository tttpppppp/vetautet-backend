USE vetautet;

INSERT INTO `trips` (`id`, `train_id`, `departure_station_id`, `arrival_station_id`, `departure_time`, `arrival_time`, `duration`, `status`) VALUES
(3, 1, 1, 3, '2026-05-01 06:00:00', '2026-05-02 08:00:00', 1560, 'SCHEDULED'),
(4, 1, 1, 3, '2026-05-02 07:30:00', '2026-05-03 09:30:00', 1560, 'SCHEDULED'),
(5, 1, 1, 3, '2026-05-03 09:00:00', '2026-05-04 11:00:00', 1560, 'SCHEDULED'),
(6, 1, 1, 2, '2026-05-01 13:00:00', '2026-05-02 05:00:00', 960, 'SCHEDULED'),
(7, 1, 1, 2, '2026-05-02 14:30:00', '2026-05-03 06:30:00', 960, 'SCHEDULED'),
(8, 1, 2, 3, '2026-05-03 08:15:00', '2026-05-04 00:15:00', 960, 'SCHEDULED'),
(9, 1, 2, 3, '2026-05-04 10:45:00', '2026-05-05 02:45:00', 960, 'SCHEDULED'),
(10, 1, 3, 1, '2026-05-01 17:00:00', '2026-05-02 19:00:00', 1560, 'SCHEDULED'),
(11, 1, 3, 1, '2026-05-02 18:30:00', '2026-05-03 20:30:00', 1560, 'SCHEDULED'),
(12, 1, 3, 2, '2026-05-03 21:00:00', '2026-05-04 13:00:00', 960, 'SCHEDULED'),
(13, 2, 3, 1, '2026-05-01 20:00:00', '2026-05-02 22:00:00', 1560, 'SCHEDULED'),
(14, 2, 3, 1, '2026-05-02 20:00:00', '2026-05-03 22:00:00', 1560, 'SCHEDULED'),
(15, 2, 3, 1, '2026-05-03 20:00:00', '2026-05-04 22:00:00', 1560, 'SCHEDULED'),
(16, 2, 3, 2, '2026-05-04 15:00:00', '2026-05-05 07:00:00', 960, 'SCHEDULED'),
(17, 2, 2, 1, '2026-05-05 06:00:00', '2026-05-05 22:00:00', 960, 'SCHEDULED'),
(18, 2, 2, 3, '2026-05-06 09:00:00', '2026-05-07 01:00:00', 960, 'SCHEDULED'),
(19, 1, 1, 3, '2026-05-07 06:30:00', '2026-05-08 08:30:00', 1560, 'SCHEDULED'),
(20, 1, 3, 1, '2026-05-08 18:00:00', '2026-05-09 20:00:00', 1560, 'SCHEDULED'),
(21, 2, 1, 2, '2026-05-09 05:30:00', '2026-05-09 21:30:00', 960, 'SCHEDULED'),
(22, 2, 2, 1, '2026-05-10 07:00:00', '2026-05-10 23:00:00', 960, 'SCHEDULED');

INSERT INTO `ticket_prices` (`trip_id`, `type_id`, `price`)
SELECT t.id,
       c.type_id,
       CASE
           WHEN c.type_id = 1 THEN 500000 + (MOD(t.id, 4) * 25000)
           WHEN c.type_id = 2 THEN 300000 + (MOD(t.id, 3) * 20000)
           WHEN c.type_id = 4 THEN 1200000 + (MOD(t.id, 4) * 50000)
           ELSE 450000
       END AS price
FROM trips t
JOIN carriages c ON c.train_id = t.train_id
WHERE t.id BETWEEN 3 AND 22
GROUP BY t.id, c.type_id;

INSERT INTO `tickets` (`trip_id`, `seat_id`, `price`, `status`)
SELECT t.id,
       s.id,
       CASE
           WHEN c.type_id = 1 THEN 500000 + (MOD(t.id, 4) * 25000)
           WHEN c.type_id = 2 THEN 300000 + (MOD(t.id, 3) * 20000)
           WHEN c.type_id = 4 THEN 1200000 + (MOD(t.id, 4) * 50000)
           ELSE 450000
       END AS price,
       'AVAILABLE'
FROM trips t
JOIN carriages c ON c.train_id = t.train_id
JOIN seats s ON s.carriage_id = c.id
WHERE t.id BETWEEN 3 AND 22;
