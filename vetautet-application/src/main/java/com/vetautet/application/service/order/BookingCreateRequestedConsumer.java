package com.vetautet.application.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vetautet.application.dto.BookingResponse;
import com.vetautet.application.dto.BookingRequest;
import com.vetautet.domain.model.BookingCreateRequestedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingCreateRequestedConsumer {

    private static final String BOOKING_CREATE_REQUESTED_TOPIC = "booking-create-requested";

    private final BookingAppService bookingAppService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public BookingCreateRequestedConsumer(BookingAppService bookingAppService,
                                          ObjectMapper objectMapper,
                                          SimpMessagingTemplate messagingTemplate) {
        this.bookingAppService = bookingAppService;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(
            topics = BOOKING_CREATE_REQUESTED_TOPIC,
            groupId = "vetautet-booking-create-group",
            autoStartup = "${vetautet.kafka.listeners.enabled:true}"
    )
    public void handle(ConsumerRecord<String, Object> record) {
        BookingCreateRequestedEvent event = toEvent(record.value());
        if (event == null || event.getRequestId() == null || event.getUserId() == null) {
            System.err.println(">>> [BOOKING CREATE ASYNC SKIPPED] invalid payload key=" + record.key()
                    + " valueType=" + (record.value() != null ? record.value().getClass().getName() : "null"));
            return;
        }

        try {
            bookingAppService.createBookingForUser(event.getUserId(), toBookingRequest(event), event.getRequestId());
        } catch (RuntimeException ex) {
            System.err.println(">>> [BOOKING CREATE ASYNC FAILED] requestId=" + event.getRequestId()
                    + " userId=" + event.getUserId()
                    + " error=" + ex.getMessage());
            ex.printStackTrace();
            pushFailed(event, ex);
        }
    }

    private void pushFailed(BookingCreateRequestedEvent event, RuntimeException ex) {
        BookingResponse response = BookingResponse.builder()
                .requestId(event.getRequestId())
                .status("FAILED")
                .ticketIds(allTicketIds(event))
                .build();
        messagingTemplate.convertAndSend("/topic/users/" + event.getUserId() + "/bookings", response);
        messagingTemplate.convertAndSendToUser(event.getUserId().toString(), "/queue/bookings", response);
    }

    private BookingCreateRequestedEvent toEvent(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BookingCreateRequestedEvent event) {
            return event;
        }
        if (value instanceof String json) {
            try {
                return objectMapper.readValue(json, BookingCreateRequestedEvent.class);
            } catch (Exception ex) {
                throw new IllegalArgumentException("BOOKING_CREATE_REQUEST_PAYLOAD_INVALID", ex);
            }
        }
        return objectMapper.convertValue(value, BookingCreateRequestedEvent.class);
    }

    private BookingRequest toBookingRequest(BookingCreateRequestedEvent event) {
        return BookingRequest.builder()
                .tripType(event.getTripType())
                .tripId(event.getTripId())
                .departureStationId(event.getDepartureStationId())
                .arrivalStationId(event.getArrivalStationId())
                .ticketIds(event.getTicketIds())
                .returnTripId(event.getReturnTripId())
                .returnDepartureStationId(event.getReturnDepartureStationId())
                .returnArrivalStationId(event.getReturnArrivalStationId())
                .returnTicketIds(event.getReturnTicketIds())
                .passengers(toPassengers(event.getPassengers()))
                .promoCode(event.getPromoCode())
                .contactName(event.getContactName())
                .contactEmail(event.getContactEmail())
                .contactPhone(event.getContactPhone())
                .contactIdCard(event.getContactIdCard())
                .build();
    }

    private List<BookingRequest.PassengerDetails> toPassengers(
            List<BookingCreateRequestedEvent.PassengerDetails> passengers) {
        if (passengers == null) {
            return List.of();
        }

        return passengers.stream()
                .map(passenger -> new BookingRequest.PassengerDetails(
                        passenger.getTicketId(),
                        passenger.getDirection(),
                        passenger.getName(),
                        passenger.getIdCard()
                ))
                .collect(Collectors.toList());
    }

    private List<Long> allTicketIds(BookingCreateRequestedEvent event) {
        java.util.ArrayList<Long> ticketIds = new java.util.ArrayList<>();
        if (event.getTicketIds() != null) {
            ticketIds.addAll(event.getTicketIds());
        }
        if (event.getReturnTicketIds() != null) {
            ticketIds.addAll(event.getReturnTicketIds());
        }
        return ticketIds;
    }
}
