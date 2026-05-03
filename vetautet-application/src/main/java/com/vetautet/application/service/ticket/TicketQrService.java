package com.vetautet.application.service.ticket;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vetautet.application.dto.TicketQrVerifyResponse;
import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.BookingDetail;
import com.vetautet.domain.model.Ticket;
import com.vetautet.domain.model.Trip;
import com.vetautet.domain.service.BookingDomainService;
import com.vetautet.domain.service.TripDomainService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class TicketQrService {

    private static final String TOKEN_VERSION = "VETAU1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final BookingDomainService bookingDomainService;
    private final TripDomainService tripDomainService;

    @Value("${ticket.qr.secret:${jwt.secret}}")
    private String qrSecret;

    public TicketQrService(BookingDomainService bookingDomainService, TripDomainService tripDomainService) {
        this.bookingDomainService = bookingDomainService;
        this.tripDomainService = tripDomainService;
    }

    public String generateToken(Long bookingId, Long ticketId) {
        if (bookingId == null || ticketId == null) {
            throw new IllegalArgumentException("bookingId and ticketId are required");
        }
        String payload = bookingId + ":" + ticketId;
        String encodedPayload = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return TOKEN_VERSION + "." + encodedPayload + "." + sign(encodedPayload);
    }

    public byte[] generateQrPng(String text, int width, int height) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("QR_GENERATION_FAILED", ex);
        }
    }

    public String generateQrDataUri(Long bookingId, Long ticketId, int size) {
        byte[] qrBytes = generateQrPng(generateToken(bookingId, ticketId), size, size);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);
    }

    public byte[] generateOwnedTicketQrPng(Long userId, Long bookingId, Long ticketId, int size) {
        Booking booking = bookingDomainService.getBookingByIdFetched(bookingId);
        if (booking.getUser() == null || !userId.equals(booking.getUser().getId())) {
            throw new RuntimeException("Ban khong co quyen xem QR cua ve nay");
        }
        findDetail(booking, ticketId);
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("BOOKING_NOT_CONFIRMED");
        }
        return generateQrPng(generateToken(bookingId, ticketId), size, size);
    }

    public TicketQrVerifyResponse verify(String token) {
        ParsedTicketQr parsed = parseAndVerify(token);
        if (parsed == null) {
            return invalid("INVALID_QR_TOKEN", "QR token khong hop le");
        }

        Booking booking;
        try {
            booking = bookingDomainService.getBookingByIdFetched(parsed.bookingId());
        } catch (Exception ex) {
            return invalid("BOOKING_NOT_FOUND", "Khong tim thay booking");
        }

        BookingDetail detail = findDetailOrNull(booking, parsed.ticketId());
        if (detail == null || detail.getTicket() == null) {
            return invalid("TICKET_NOT_IN_BOOKING", "Ve khong thuoc booking nay");
        }

        Ticket ticket = detail.getTicket();
        if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            return invalid("BOOKING_NOT_CONFIRMED", "Booking chua duoc xac nhan");
        }
        if (!"BOOKED".equalsIgnoreCase(ticket.getStatus())) {
            return invalid("TICKET_NOT_BOOKED", "Ve chua o trang thai da ban");
        }

        Trip trip = ticket.getTripId() != null ? tripDomainService.getTripByIdFetched(ticket.getTripId()) : null;
        return TicketQrVerifyResponse.builder()
                .valid(true)
                .code("VALID_TICKET")
                .message("VALID_TICKET")
                .bookingId(booking.getId())
                .ticketId(ticket.getId())
                .ticketStatus(ticket.getStatus())
                .bookingStatus(booking.getStatus())
                .passengerName(detail.getPassengerName())
                .passengerIdCard(detail.getPassengerIdCard())
                .seatNumber(ticket.getSeatNumber())
                .carriageNumber(ticket.getCarriageNumber())
                .carriageTypeName(ticket.getCarriageTypeName())
                .trainCode(trip != null && trip.getTrain() != null ? trip.getTrain().getCode() : null)
                .departureStation(trip != null && trip.getDepartureStation() != null ? trip.getDepartureStation().getName() : null)
                .arrivalStation(trip != null && trip.getArrivalStation() != null ? trip.getArrivalStation().getName() : null)
                .departureTime(trip != null ? trip.getDepartureTime() : null)
                .arrivalTime(trip != null ? trip.getArrivalTime() : null)
                .build();
    }

    private BookingDetail findDetail(Booking booking, Long ticketId) {
        BookingDetail detail = findDetailOrNull(booking, ticketId);
        if (detail == null) {
            throw new RuntimeException("TICKET_NOT_IN_BOOKING");
        }
        return detail;
    }

    private BookingDetail findDetailOrNull(Booking booking, Long ticketId) {
        if (booking.getDetails() == null || ticketId == null) {
            return null;
        }
        return booking.getDetails().stream()
                .filter(detail -> detail.getTicket() != null && ticketId.equals(detail.getTicket().getId()))
                .findFirst()
                .orElse(null);
    }

    private ParsedTicketQr parseAndVerify(String token) {
        try {
            if (token == null || token.isBlank()) {
                return null;
            }
            String[] parts = token.trim().split("\\.");
            if (parts.length != 3 || !TOKEN_VERSION.equals(parts[0])) {
                return null;
            }

            String expectedSignature = sign(parts[1]);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String[] payloadParts = payload.split(":");
            if (payloadParts.length != 2) {
                return null;
            }
            return new ParsedTicketQr(Long.parseLong(payloadParts[0]), Long.parseLong(payloadParts[1]));
        } catch (Exception ex) {
            return null;
        }
    }

    private String sign(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(qrSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new RuntimeException("QR_SIGNING_FAILED", ex);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private TicketQrVerifyResponse invalid(String code, String message) {
        return TicketQrVerifyResponse.builder()
                .valid(false)
                .code(code)
                .message(message)
                .build();
    }

    private record ParsedTicketQr(Long bookingId, Long ticketId) {
    }
}
