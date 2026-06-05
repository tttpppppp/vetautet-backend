package com.vetautet.infrastructure.notification;

import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingDetail;
import com.vetautet.domain.model.Notification;
import com.vetautet.domain.model.UserNotificationEvent;
import com.vetautet.domain.repository.NotificationRepository;
import com.vetautet.domain.security.SensitiveDataCryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SensitiveDataCryptoService sensitiveDataCryptoService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification sendBookingConfirmation(Booking booking) {
        String customerName = booking.getUser().getName();
        Long userId = booking.getUser().getId();

        StringBuilder sb = new StringBuilder();
        sb.append("Xin chao ").append(customerName).append(",\n");
        sb.append("Don hang #").append(booking.getId()).append(" da duoc xac nhan thanh cong!\n");
        sb.append("Tong tien: ").append(booking.getTotalPrice()).append(" VND\n");

        if (booking.getDetails() != null) {
            sb.append("Chi tiet ve:\n");
            int stt = 1;
            for (BookingDetail detail : booking.getDetails()) {
                String seatInfo = detail.getTicket() != null
                        ? "Ghe " + detail.getTicket().getSeatNumber() + " - Toa " + detail.getTicket().getCarriageNumber()
                        : "N/A";
                sb.append("  ").append(stt++).append(". ").append(seatInfo);
                sb.append(" | Hanh khach: ").append(detail.getPassengerName());
                sb.append(" | CCCD: ")
                        .append(sensitiveDataCryptoService.decrypt(detail.getPassengerIdCard()))
                        .append("\n");
            }
        }

        String title = "Dat ve thanh cong #" + booking.getId();
        String content = sb.toString();

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type("BOOKING_CONFIRMED")
                .referenceId(booking.getId())
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);

        pushToUser(userId, saved);

        System.out.println(">>> [NOTI] Saved and pushed booking confirmation id=" + saved.getId() + " to userId=" + userId);
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendBookingCancellation(Booking booking) {
        Long userId = booking.getUser().getId();

        String title = "Don hang #" + booking.getId() + " da bi huy";
        String content = "Don hang #" + booking.getId() + " da bi huy do qua han thanh toan 15 phut. "
                + "Ghe da duoc giai phong. Ban co the dat lai bat cu luc nao.";

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type("BOOKING_CANCELLED")
                .referenceId(booking.getId())
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);

        pushToUser(userId, saved);

        System.out.println(">>> [NOTI] Saved and pushed booking cancellation to userId=" + userId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendPaymentFailure(Booking booking, String reason) {
        Long userId = booking.getUser().getId();
        if (notificationRepository.existsByUserIdAndTypeAndReferenceId(userId, "PAYMENT_FAILED", booking.getId())) {
            System.out.println(">>> [NOTI] Payment failure notification already exists userId=" + userId
                    + " bookingId=" + booking.getId());
            return;
        }

        String title = "Thanh toan that bai #" + booking.getId();
        String content = "Don hang #" + booking.getId()
                + " thanh toan khong thanh cong. He thong da huy don va giai phong ghe."
                + " Ly do: " + safeReason(reason);

        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type("PAYMENT_FAILED")
                .referenceId(booking.getId())
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);

        pushToUser(userId, saved);

        System.out.println(">>> [NOTI] Saved and pushed payment failure to userId=" + userId
                + " bookingId=" + booking.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendSystemNotification(Long userId, String title, String content, String type, Long referenceId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);

        pushToUser(userId, saved);

        System.out.println(">>> [NOTI] Saved and pushed system notification to userId=" + userId + ": " + title);
    }

    private void pushToUser(Long userId, Notification notification) {
        UserNotificationEvent event = UserNotificationEvent.builder()
                .notificationId(notification.getId())
                .userId(userId)
                .bookingId(notification.getReferenceId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();

        afterCommit(() -> {
            try {
                messagingTemplate.convertAndSend(
                        "/topic/users/" + userId + "/notifications",
                        event
                );
                messagingTemplate.convertAndSendToUser(
                        userId.toString(),
                        "/queue/notifications",
                        event
                );
            } catch (Exception e) {
                System.err.println(">>> [WS ERROR] Could not push realtime notification to userId="
                        + userId + ": " + e.getMessage());
            }
        });
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "Giao dich bi tu choi hoac bi huy." : reason;
    }
}
