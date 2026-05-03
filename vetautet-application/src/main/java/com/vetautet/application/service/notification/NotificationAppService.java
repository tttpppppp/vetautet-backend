package com.vetautet.application.service.notification;

import java.util.Map;

public interface NotificationAppService {
    Map<String, Object> getMyNotifications(Long userId, int page, int size);
    long getUnreadCount(Long userId);
    void markAsRead(Long id);
    void markAllAsRead(Long userId);
}
