ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS order_number VARCHAR(64) NULL AFTER id,
    ADD COLUMN IF NOT EXISTS storage_month CHAR(6) NULL AFTER order_number;

UPDATE bookings
SET order_number = CONCAT(
        'ORD-',
        DATE_FORMAT(COALESCE(created_at, NOW()), '%Y%m%d'),
        '-',
        LPAD(id, 6, '0')
    )
WHERE order_number IS NULL OR order_number = '';

UPDATE bookings
SET storage_month = DATE_FORMAT(COALESCE(created_at, NOW()), '%Y%m')
WHERE storage_month IS NULL OR storage_month = '';

ALTER TABLE bookings
    MODIFY COLUMN order_number VARCHAR(64) NOT NULL,
    MODIFY COLUMN storage_month CHAR(6) NOT NULL;

CREATE UNIQUE INDEX idx_bookings_order_number ON bookings (order_number);
CREATE INDEX idx_bookings_storage_month ON bookings (storage_month);
