package com.vetautet.infrastructure.persistence.mapper;

import com.vetautet.domain.model.*;
import com.vetautet.infrastructure.persistence.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PersistenceMapper {

    // User & Security
    User toDomain(UserEntity entity);
    UserEntity toEntity(User domain);

    Role toDomain(RoleEntity entity);
    RoleEntity toEntity(Role domain);

    Permission toDomain(PermissionEntity entity);
    PermissionEntity toEntity(Permission domain);

    RefreshToken toDomain(RefreshTokenEntity entity);
    RefreshTokenEntity toEntity(RefreshToken domain);

    // Infrastructure & Trip
    Station toDomain(StationEntity entity);
    StationEntity toEntity(Station domain);

    Train toDomain(TrainEntity entity);
    TrainEntity toEntity(Train domain);

    Trip toDomain(TripEntity entity);
    TripEntity toEntity(Trip domain);

    @Mapping(target = "trip", ignore = true)
    @Mapping(target = "tripId", source = "trip.id")
    @Mapping(target = "seatId", source = "seat.id")
    @Mapping(target = "seatNumber", source = "seat.seatNumber")
    @Mapping(target = "carriageId", source = "seat.carriage.id")
    @Mapping(target = "carriageNumber", source = "seat.carriage.name")
    @Mapping(target = "carriageTypeName", source = "seat.carriage.type.name")
    Ticket toDomain(TicketEntity entity);
    
    @Mapping(target = "trip", ignore = true)
    TicketEntity toEntity(Ticket domain);

    Booking toDomain(BookingEntity entity);
    BookingEntity toEntity(Booking domain);

    @Mapping(target = "booking", ignore = true)
    BookingDetail toDomain(BookingDetailEntity entity);
    
    @Mapping(target = "booking", ignore = true)
    BookingDetailEntity toEntity(BookingDetail domain);

    @Mapping(target = "bookingId", source = "booking.id")
    Payment toDomain(PaymentEntity entity);

    @Mapping(target = "booking", ignore = true)
    PaymentEntity toEntity(Payment domain);

    EmailVerificationOtp toDomain(EmailVerificationOtpEntity entity);
    EmailVerificationOtpEntity toEntity(EmailVerificationOtp domain);
}
