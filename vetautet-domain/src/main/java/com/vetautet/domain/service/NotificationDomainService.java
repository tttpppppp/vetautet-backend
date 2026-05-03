package com.vetautet.domain.service;

import java.util.Map;

public interface NotificationDomainService {
    Map<String, Object> getNotifications(Long userId, int page, int size);
    long getUnreadCount(Long userId);
    void markAsRead(Long id);
    void markAllAsRead(Long userId);
}
