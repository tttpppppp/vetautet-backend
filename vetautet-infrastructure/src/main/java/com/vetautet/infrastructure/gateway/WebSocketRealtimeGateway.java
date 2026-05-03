package com.vetautet.infrastructure.gateway;

import com.vetautet.domain.gateway.RealtimeGateway;
import com.vetautet.domain.model.SeatStatusEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketRealtimeGateway implements RealtimeGateway {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastSeatStatus(Long tripId, Long ticketId, String seatNumber, String status) {
        String destination = "/topic/trips/" + tripId + "/seats";
        SeatStatusEvent event = SeatStatusEvent.builder()
                .tripId(tripId)
                .ticketId(ticketId)
                .seatNumber(seatNumber)
                .status(status)
                .build();
        messagingTemplate.convertAndSend(destination, event);
    }
}
