package com.vetautet.application.mapper;

import com.vetautet.application.dto.*;
import com.vetautet.domain.model.Role;
import com.vetautet.domain.model.Station;
import com.vetautet.domain.model.Ticket;
import com.vetautet.domain.model.Trip;
import com.vetautet.domain.model.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(mapRoles(user.getRoles()))")
    UserResponse toResponse(User user);

    @Mapping(target = "trainCode", source = "trip.train.code")
    @Mapping(target = "departureStation", source = "trip.departureStation.name")
    @Mapping(target = "arrivalStation", source = "trip.arrivalStation.name")
    @Mapping(target = "departureTime", source = "trip.departureTime")
    @Mapping(target = "seatNumber", source = "seatNumber")
    @Mapping(target = "heldByCurrentBooking", ignore = true)
    @Mapping(target = "holdingBookingId", ignore = true)
    TicketResponse toTicketResponse(Ticket ticket);

    @Mapping(target = "trainCode", source = "train.code")
    @Mapping(target = "trainCategory", source = "train.category")
    @Mapping(target = "departureStation", source = "departureStation.name")
    @Mapping(target = "arrivalStation", source = "arrivalStation.name")
    @Mapping(target = "carriages", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "minPrice", ignore = true)
    @Mapping(target = "availableSeats", ignore = true)
    @Mapping(target = "totalSeats", ignore = true)
    TripResponse toTripResponse(Trip trip, @Context boolean detail);

    @AfterMapping
    default void groupTicketsByCarriage(Trip trip, @MappingTarget TripResponse response, @Context boolean detail) {
        // Tự động tính duration nếu trong DB đang để null
        if (response.getDuration() == null && trip.getDepartureTime() != null && trip.getArrivalTime() != null) {
            long minutes = java.time.Duration.between(trip.getDepartureTime(), trip.getArrivalTime()).toMinutes();
            response.setDuration((int) minutes);
        }

        applyTripSummary(trip, response);

        // Nếu là bản summary (detail = false), không load sơ đồ ghế
        if (!detail) {
            response.setCarriages(null);
            return;
        }

        if (trip.getTickets() == null || trip.getTickets().isEmpty()) {
            response.setCarriages(List.of());
            return;
        }

        Map<Long, List<Ticket>> grouped = trip.getTickets().stream()
                .filter(t -> t.getCarriageId() != null)
                .collect(Collectors.groupingBy(Ticket::getCarriageId));

        List<CarriageResponse> carriages = grouped.entrySet().stream()
                .map(entry -> {
                    Ticket representative = entry.getValue().get(0);
                    // Lưu ý: Chỗ này toTicketResponse không cần context vì TicketResponse không có AfterMapping phức tạp
                    return new CarriageResponse(
                            entry.getKey(),
                            representative.getCarriageNumber(),
                            representative.getCarriageTypeName(),
                            entry.getValue().stream()
                                    .map(this::toTicketResponse)
                                    .collect(Collectors.toList())
                    );
                })
                .sorted(Comparator.comparing(CarriageResponse::getCarriageNumber))
                .collect(Collectors.toList());

        response.setCarriages(carriages);
    }

    default void applyTripSummary(Trip trip, TripResponse response) {
        if (trip.getTickets() == null || trip.getTickets().isEmpty()) {
            response.setAvailableSeats(0);
            response.setTotalSeats(0);
            return;
        }

        int availableSeats = (int) trip.getTickets().stream()
                .filter(ticket -> "AVAILABLE".equalsIgnoreCase(ticket.getStatus()))
                .count();

        BigDecimal minPrice = trip.getTickets().stream()
                .filter(ticket -> "AVAILABLE".equalsIgnoreCase(ticket.getStatus()))
                .map(Ticket::getPrice)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElseGet(() -> trip.getTickets().stream()
                        .map(Ticket::getPrice)
                        .filter(price -> price != null)
                        .min(BigDecimal::compareTo)
                        .orElse(null));

        response.setAvailableSeats(availableSeats);
        response.setTotalSeats(trip.getTickets().size());
        response.setMinPrice(minPrice);
        response.setPrice(minPrice);
    }


    default Set<String> mapRoles(Set<Role> roles) {
        if (roles == null) return null;
        return roles.stream().map(Role::getCode).collect(Collectors.toSet());
    }

    StationResponse toStationResponse(Station station);
    Station toStation(StationRequest request);
    
    TrainResponse toTrainResponse(com.vetautet.domain.model.Train train);
    com.vetautet.domain.model.Train toTrain(TrainRequest request);
}
