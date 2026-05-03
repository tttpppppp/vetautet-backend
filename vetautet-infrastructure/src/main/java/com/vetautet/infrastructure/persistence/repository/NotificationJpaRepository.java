package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Phân trang (Lazy Loading)
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(@Param("id") Long id);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsReadByUserId(@Param("userId") Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
