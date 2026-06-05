package com.vetautet.infrastructure.persistence.order;

import com.vetautet.infrastructure.persistence.entity.BookingEntity;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BookingMonthlyMirrorRepository {

    private static final DateTimeFormatter STORAGE_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int USER_HISTORY_MONTHS = 12;

    private final JdbcTemplate jdbcTemplate;
    private final BookingMonthlyTableRouter tableRouter;

    public BookingMonthlyMirrorRepository(JdbcTemplate jdbcTemplate,
                                          BookingMonthlyTableRouter tableRouter) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableRouter = tableRouter;
    }

    public void upsert(BookingEntity booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }

        String storageMonth = booking.getStorageMonth();
        if (storageMonth == null || storageMonth.isBlank()) {
            storageMonth = deriveStorageMonth(booking.getCreatedAt());
            booking.setStorageMonth(storageMonth);
        }

        String tableName = tableRouter.resolveTableByStorageMonth(storageMonth);
        ensureMonthlyTableExists(tableName);

        String sql = """
                INSERT INTO %s
                    (id, order_number, storage_month, trip_type, user_id, original_price, promo_code, discount_amount, total_price, contact_name, contact_email, contact_phone, contact_id_card, status, expired_at, created_at, updated_at, deleted_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    order_number = VALUES(order_number),
                    storage_month = VALUES(storage_month),
                    trip_type = VALUES(trip_type),
                    user_id = VALUES(user_id),
                    original_price = VALUES(original_price),
                    promo_code = VALUES(promo_code),
                    discount_amount = VALUES(discount_amount),
                    total_price = VALUES(total_price),
                    contact_name = VALUES(contact_name),
                    contact_email = VALUES(contact_email),
                    contact_phone = VALUES(contact_phone),
                    contact_id_card = VALUES(contact_id_card),
                    status = VALUES(status),
                    expired_at = VALUES(expired_at),
                    created_at = VALUES(created_at),
                    updated_at = VALUES(updated_at),
                    deleted_at = VALUES(deleted_at)
                """.formatted(tableName);

        String finalStorageMonth = storageMonth;
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setLong(1, booking.getId());
            ps.setString(2, booking.getOrderNumber());
            ps.setString(3, finalStorageMonth);
            ps.setString(4, booking.getTripType() != null ? booking.getTripType().name() : BookingEntity.TripType.ONE_WAY.name());
            ps.setLong(5, booking.getUser().getId());
            ps.setBigDecimal(6, booking.getOriginalPrice());
            ps.setString(7, booking.getPromoCode());
            ps.setBigDecimal(8, booking.getDiscountAmount());
            ps.setBigDecimal(9, booking.getTotalPrice());
            ps.setString(10, booking.getContactName());
            ps.setString(11, booking.getContactEmail());
            ps.setString(12, booking.getContactPhone());
            ps.setString(13, booking.getContactIdCard());
            ps.setString(14, booking.getStatus() != null ? booking.getStatus().name() : null);
            ps.setTimestamp(15, toTimestamp(booking.getExpiredAt()));
            ps.setTimestamp(16, toTimestamp(booking.getCreatedAt()));
            ps.setTimestamp(17, toTimestamp(booking.getUpdatedAt()));
            ps.setTimestamp(18, toTimestamp(booking.getDeletedAt()));
            return ps;
        });
    }

    public void deleteBy(String storageMonth, Long bookingId, String orderNumber) {
        if (storageMonth == null || storageMonth.isBlank() || bookingId == null) {
            return;
        }
        String tableName = tableRouter.resolveTableByStorageMonth(storageMonth);
        ensureMonthlyTableExists(tableName);
        jdbcTemplate.update("DELETE FROM " + tableName + " WHERE id = ? OR order_number = ?", bookingId, orderNumber);
    }

    public Optional<Long> findBookingIdByOrderNumber(String orderNumber, String storageMonth) {
        String tableName = tableRouter.resolveTableByStorageMonth(storageMonth);
        ensureMonthlyTableExists(tableName);
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM " + tableName + " WHERE order_number = ? LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                orderNumber
        );
        return ids.stream().findFirst();
    }

    public List<Long> findRecentBookingIdsByUserId(Long userId) {
        List<Long> result = new ArrayList<>();
        for (String storageMonth : recentStorageMonths()) {
            String tableName = tableRouter.resolveTableByStorageMonth(storageMonth);
            ensureMonthlyTableExists(tableName);
            result.addAll(jdbcTemplate.query(
                    "SELECT id FROM " + tableName + " WHERE user_id = ? ORDER BY created_at DESC",
                    (rs, rowNum) -> rs.getLong("id"),
                    userId
            ));
        }
        return result;
    }

    private List<String> recentStorageMonths() {
        List<String> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < USER_HISTORY_MONTHS; i++) {
            months.add(current.minusMonths(i).format(STORAGE_MONTH_FORMAT));
        }
        return months;
    }

    private String deriveStorageMonth(LocalDateTime createdAt) {
        LocalDateTime effective = createdAt == null ? LocalDateTime.now() : createdAt;
        return effective.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private void ensureMonthlyTableExists(String tableName) {
        String sql = """
                CREATE TABLE IF NOT EXISTS %s (
                    id BIGINT PRIMARY KEY,
                    order_number VARCHAR(64) NOT NULL UNIQUE,
                    storage_month CHAR(6) NOT NULL,
                    trip_type VARCHAR(20) NOT NULL DEFAULT 'ONE_WAY',
                    user_id BIGINT NOT NULL,
                    original_price DECIMAL(15,2) NOT NULL DEFAULT 0,
                    promo_code VARCHAR(50) DEFAULT NULL,
                    discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
                    total_price DECIMAL(15,2) NOT NULL,
                    contact_name VARCHAR(150) DEFAULT NULL,
                    contact_email VARCHAR(150) DEFAULT NULL,
                    contact_phone VARCHAR(30) DEFAULT NULL,
                    contact_id_card VARCHAR(512) DEFAULT NULL,
                    status VARCHAR(32) NOT NULL,
                    expired_at DATETIME NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    deleted_at DATETIME DEFAULT NULL,
                    INDEX idx_%s_user_status (user_id, status),
                    INDEX idx_%s_created_at (created_at),
                    INDEX idx_%s_storage_month (storage_month)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """.formatted(tableName, tableName, tableName, tableName);
        jdbcTemplate.execute(sql);
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS contact_name VARCHAR(150) DEFAULT NULL");
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS contact_email VARCHAR(150) DEFAULT NULL");
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30) DEFAULT NULL");
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS contact_id_card VARCHAR(512) DEFAULT NULL");
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS trip_type VARCHAR(20) NOT NULL DEFAULT 'ONE_WAY' AFTER storage_month");
    }
}
