CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    method VARCHAR(50) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(255) UNIQUE,
    paid_at DATETIME NULL,
    created_at DATETIME NULL,
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_created_at ON payments(created_at);
