UPDATE `booking_details`
SET `passenger_type` = 'ADULT'
WHERE `passenger_type` IS NULL
   OR TRIM(`passenger_type`) = ''
   OR UPPER(TRIM(`passenger_type`)) NOT IN (
       SELECT `passenger_type` FROM `passenger_fare_rules`
   );

UPDATE `booking_details`
SET `passenger_type` = UPPER(TRIM(`passenger_type`));

UPDATE `trip_segment_prices`
SET `passenger_type` = 'ADULT'
WHERE `passenger_type` IS NULL
   OR TRIM(`passenger_type`) = ''
   OR UPPER(TRIM(`passenger_type`)) NOT IN (
       SELECT `passenger_type` FROM `passenger_fare_rules`
   );

UPDATE `trip_segment_prices`
SET `passenger_type` = UPPER(TRIM(`passenger_type`));

ALTER TABLE `booking_details`
    MODIFY COLUMN `passenger_type` VARCHAR(30) NOT NULL DEFAULT 'ADULT';

ALTER TABLE `trip_segment_prices`
    MODIFY COLUMN `passenger_type` VARCHAR(30) NOT NULL DEFAULT 'ADULT';

CREATE INDEX `idx_booking_details_passenger_type`
    ON `booking_details` (`passenger_type`);

CREATE INDEX `idx_trip_segment_prices_passenger_type`
    ON `trip_segment_prices` (`passenger_type`);

ALTER TABLE `booking_details`
    ADD CONSTRAINT `fk_booking_details_passenger_fare_rule`
    FOREIGN KEY (`passenger_type`)
    REFERENCES `passenger_fare_rules` (`passenger_type`)
    ON UPDATE CASCADE
    ON DELETE RESTRICT;

ALTER TABLE `trip_segment_prices`
    ADD CONSTRAINT `fk_trip_segment_prices_passenger_fare_rule`
    FOREIGN KEY (`passenger_type`)
    REFERENCES `passenger_fare_rules` (`passenger_type`)
    ON UPDATE CASCADE
    ON DELETE RESTRICT;
