CREATE INDEX idx_booking_details_ticket_id_non_unique ON booking_details(ticket_id);

ALTER TABLE booking_details DROP INDEX ticket_id;
