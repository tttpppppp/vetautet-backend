package com.vetautet.application.scheduler;

import com.vetautet.application.service.ticket.TicketAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TicketScheduler {

    @Autowired
    private TicketAppService ticketAppService;

    // Chạy mỗi 1 phút để quét các ghế giữ chỗ quá 15 phút
    @Scheduled(fixedRate = 60000)
    public void releaseExpiredTickets() {
        ticketAppService.releaseExpiredTickets();
    }
}
