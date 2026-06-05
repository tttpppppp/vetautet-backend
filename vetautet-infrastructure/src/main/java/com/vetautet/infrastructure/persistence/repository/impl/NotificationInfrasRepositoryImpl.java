package com.vetautet.infrastructure.persistence.repository.impl;

import com.vetautet.domain.model.Notification;
import com.vetautet.domain.repository.NotificationRepository;
import com.vetautet.infrastructure.persistence.entity.NotificationEntity;
import com.vetautet.infrastructure.persistence.entity.UserEntity;
import com.vetautet.infrastructure.persistence.repository.NotificationJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Repository
public class NotificationInfrasRepositoryImpl implements NotificationRepository {

    @Autowired
    private NotificationJpaRepository jpaRepository;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = new NotificationEntity();
        
        UserEntity userRef = new UserEntity();
        userRef.setId(notification.getUserId());
        entity.setUser(userRef);
        
        entity.setTitle(notification.getTitle());
        entity.setContent(notification.getContent());
        entity.setType(NotificationEntity.NotificationType.valueOf(notification.getType()));
        entity.setReferenceId(notification.getReferenceId());
        entity.setRead(false);
        entity.setCreatedAt(LocalDateTime.now());

        NotificationEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<Notification> findByUserId(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> findByUserId(Long userId, int page, int size) {
        Page<NotificationEntity> pageResult = jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        
        List<Notification> notifications = pageResult.getContent().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        response.put("currentPage", pageResult.getNumber());
        response.put("totalItems", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("hasNext", pageResult.hasNext());
        return response;
    }

    @Override
    public boolean existsByUserIdAndTypeAndReferenceId(Long userId, String type, Long referenceId) {
        if (userId == null || type == null || referenceId == null) {
            return false;
        }
        return jpaRepository.existsByUserIdAndTypeAndReferenceId(
                userId,
                NotificationEntity.NotificationType.valueOf(type),
                referenceId
        );
    }

    @Override
    @Transactional
    public void markAsRead(Long id) {
        jpaRepository.markAsRead(id);
    }

    @Override
    @Transactional
    public void markAllAsReadByUserId(Long userId) {
        jpaRepository.markAllAsReadByUserId(userId);
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        return jpaRepository.countByUserIdAndIsReadFalse(userId);
    }

    private Notification toDomain(NotificationEntity entity) {
        return Notification.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType().name())
                .referenceId(entity.getReferenceId())
                .isRead(entity.isRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
