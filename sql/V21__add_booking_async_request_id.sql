ALTER TABLE bookings
    ADD COLUMN async_request_id VARCHAR(64) NULL,
    ADD UNIQUE KEY ux_bookings_async_request_id (async_request_id);
