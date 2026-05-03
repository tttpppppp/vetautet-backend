package com.vetautet.domain.repository;

import com.vetautet.domain.model.Notification;
import java.util.List;
import java.util.Map;

public interface NotificationRepository {
    Notification save(Notification notification);
    List<Notification> findByUserId(Long userId);
    Map<String, Object> findByUserId(Long userId, int page, int size);
    void markAsRead(Long id);
    void markAllAsReadByUserId(Long userId);
    long countUnreadByUserId(Long userId);
}
