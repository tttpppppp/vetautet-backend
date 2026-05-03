package com.vetautet.controller.resource;

import com.vetautet.application.service.notification.NotificationAppService;
import com.vetautet.domain.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationAppService notificationAppService;

    /**
     * Lấy danh sách thông báo (có phân trang - Lazy Loading)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyNotifications(
            @AuthenticationPrincipal(expression = "domainUser") User user,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationAppService.getMyNotifications(user.getId(), page, size));
    }

    /**
     * Đếm số thông báo chưa đọc (cho badge 🔔)
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal(expression = "domainUser") User user) {
        long count = notificationAppService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /**
     * Đánh dấu 1 thông báo đã đọc
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Long id) {
        notificationAppService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Đánh dấu tất cả đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal(expression = "domainUser") User user) {
        notificationAppService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }
}
