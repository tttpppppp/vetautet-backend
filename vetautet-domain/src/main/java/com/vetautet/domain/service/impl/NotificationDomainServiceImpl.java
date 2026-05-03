package com.vetautet.domain.service.impl;

import com.vetautet.domain.repository.NotificationRepository;
import com.vetautet.domain.service.NotificationDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class NotificationDomainServiceImpl implements NotificationDomainService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public Map<String, Object> getNotifications(Long userId, int page, int size) {
        return notificationRepository.findByUserId(userId, page, size);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.markAsRead(id);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
