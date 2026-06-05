SET @outbox_cleanup_index_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'outbox_events'
      AND index_name = 'idx_outbox_events_published_cleanup'
);

SET @outbox_cleanup_index_sql := IF(
    @outbox_cleanup_index_exists = 0,
    'CREATE INDEX idx_outbox_events_published_cleanup ON outbox_events (published, published_at, id)',
    'SELECT 1'
);

PREPARE outbox_cleanup_index_stmt FROM @outbox_cleanup_index_sql;
EXECUTE outbox_cleanup_index_stmt;
DEALLOCATE PREPARE outbox_cleanup_index_stmt;
