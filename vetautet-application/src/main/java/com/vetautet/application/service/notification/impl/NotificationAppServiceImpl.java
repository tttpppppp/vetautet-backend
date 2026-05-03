package com.vetautet.application.service.notification.impl;

import com.vetautet.application.service.notification.NotificationAppService;
import com.vetautet.domain.service.NotificationDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class NotificationAppServiceImpl implements NotificationAppService {

    @Autowired
    private NotificationDomainService notificationDomainService;

    @Override
    public Map<String, Object> getMyNotifications(Long userId, int page, int size) {
        return notificationDomainService.getNotifications(userId, page, size);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationDomainService.getUnreadCount(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        notificationDomainService.markAsRead(id);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationDomainService.markAllAsRead(userId);
    }
}
