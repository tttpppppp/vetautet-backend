package com.vetautet.infrastructure.messaging;

import org.springframework.transaction.annotation.Transactional;

import com.vetautet.domain.model.Booking;
import com.vetautet.domain.repository.BookingRepository;
import com.vetautet.domain.service.BookingDomainService;
import com.vetautet.infrastructure.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCancellationConsumer {

    @Autowired
    private BookingDomainService bookingDomainService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationService notificationService;

    @KafkaListener(topics = "order-created", groupId = "vetautet-group", autoStartup = "${vetautet.kafka.listeners.enabled:true}")
    @Transactional
    public void handleOrderTimeout(Long bookingId) {
        System.out.println(">>> Kafka received Order Created event for ID: " + bookingId);
        
        try {
            // Lấy thông tin booking trước khi hủy
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            
            // CHÚ Ý: Logic này đang bị sai vì nó hủy đơn hàng NGAY LẬP TỨC khi vừa tạo xong.
            // Cần sử dụng giải pháp Delay Queue hoặc Scheduled Task thay vì hủy ngay tại đây.
            /*
            if (booking != null && "PENDING".equals(booking.getStatus())) {
                System.out.println(">>> System will cancel booking " + bookingId + " (Chưa thanh toán).");
                bookingDomainService.cancelBooking(bookingId);
                
                // Gửi thông báo hủy đơn tới khách hàng
                notificationService.sendBookingCancellation(booking);
            } else {
                System.out.println(">>> Booking " + bookingId + " đã được thanh toán hoặc không tồn tại. Bỏ qua.");
            }
            */
            System.out.println(">>> Kafka received Order Created event for ID: " + bookingId + ". (Immediate cancellation disabled for testing)");
            
        } catch (Exception e) {
            System.err.println(">>> Error processing cancellation for booking " + bookingId + ": " + e.getMessage());
        }
    }
}
