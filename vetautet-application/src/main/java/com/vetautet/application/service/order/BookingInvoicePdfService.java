package com.vetautet.application.service.order;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vetautet.application.dto.BookingDetailResponse;
import com.vetautet.application.service.ticket.TicketQrService;
import com.vetautet.domain.model.TripSegment;
import com.vetautet.domain.model.TripStop;
import com.vetautet.domain.repository.TripScheduleRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class BookingInvoicePdfService {

    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final TicketQrService ticketQrService;
    private final TripScheduleRepository tripScheduleRepository;

    public BookingInvoicePdfService(TicketQrService ticketQrService,
                                    TripScheduleRepository tripScheduleRepository) {
        this.ticketQrService = ticketQrService;
        this.tripScheduleRepository = tripScheduleRepository;
    }

    public byte[] generate(BookingDetailResponse booking) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            useVietnameseFontIfAvailable(builder);
            builder.withHtmlContent(buildHtml(booking), null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("PDF_GENERATION_FAILED", ex);
        }
    }

    private void useVietnameseFontIfAvailable(PdfRendererBuilder builder) {
        File windowsArial = new File("C:/Windows/Fonts/arial.ttf");
        if (windowsArial.exists()) {
            builder.useFont(windowsArial, "Arial");
            return;
        }

        File linuxDejaVu = new File("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
        if (linuxDejaVu.exists()) {
            builder.useFont(linuxDejaVu, "DejaVu Sans");
        }
    }

    private String buildHtml(BookingDetailResponse booking) {
        StringBuilder rows = new StringBuilder();
        int index = 1;
        if (booking.getDetails() != null) {
            for (BookingDetailResponse.TicketDetail detail : booking.getDetails()) {
                String qrDataUri = detail.getTicketId() != null
                        ? ticketQrService.generateQrDataUri(booking.getBookingId(), detail.getTicketId(), 140)
                        : "";
                rows.append("""
                        <tr>
                            <td class="center">%d</td>
                            <td>
                                <div class="strong">%s</div>
                                <div class="muted">%s</div>
                            </td>
                            <td>%s</td>
                            <td>%s</td>
                            <td class="right">%s</td>
                            <td class="qr">
                                <img src="%s" alt="Ticket QR" />
                                <div class="muted">#%s</div>
                            </td>
                        </tr>
                        """.formatted(
                        index++,
                        esc(valueOrDash(detail.getPassengerName())),
                        esc(valueOrDash(detail.getPassengerIdCard())),
                        esc(valueOrDash(detail.getCarriageNumber())),
                        esc(valueOrDash(detail.getSeatNumber())),
                        esc(money(detail.getPrice())),
                        esc(qrDataUri),
                        esc(String.valueOf(detail.getTicketId()))
                ));
            }
        }
        String routeHtml = buildRouteHtml(booking);

        return """
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        @page { size: A4; margin: 22mm 18mm; }
                        * { box-sizing: border-box; }
                        body {
                            font-family: Arial, "DejaVu Sans", sans-serif;
                            color: #111827;
                            font-size: 12px;
                            line-height: 1.45;
                        }
                        .header {
                            display: table;
                            width: 100%%;
                            border-bottom: 3px solid #dc2626;
                            padding-bottom: 14px;
                            margin-bottom: 18px;
                        }
                        .brand, .meta { display: table-cell; vertical-align: top; }
                        .brand h1 { margin: 0; color: #dc2626; font-size: 28px; letter-spacing: 0; }
                        .brand div { color: #6b7280; font-weight: 700; margin-top: 2px; }
                        .meta { text-align: right; color: #374151; }
                        .meta .code { font-size: 18px; font-weight: 800; color: #111827; }
                        .badge {
                            display: inline-block;
                            border-radius: 4px;
                            padding: 4px 9px;
                            background: #dcfce7;
                            color: #166534;
                            font-weight: 800;
                            margin-top: 6px;
                        }
                        .section { margin-top: 16px; }
                        .section-title {
                            color: #dc2626;
                            font-size: 12px;
                            font-weight: 800;
                            text-transform: uppercase;
                            letter-spacing: 1px;
                            margin-bottom: 8px;
                        }
                        .grid {
                            display: table;
                            width: 100%%;
                            border: 1px solid #e5e7eb;
                            border-radius: 6px;
                        }
                        .row { display: table-row; }
                        .cell {
                            display: table-cell;
                            width: 50%%;
                            padding: 10px 12px;
                            border-bottom: 1px solid #e5e7eb;
                        }
                        .row:last-child .cell { border-bottom: 0; }
                        .label {
                            color: #6b7280;
                            font-size: 10px;
                            text-transform: uppercase;
                            font-weight: 800;
                            letter-spacing: .7px;
                        }
                        .value { margin-top: 3px; font-weight: 700; color: #111827; }
                        table {
                            width: 100%%;
                            border-collapse: collapse;
                            border: 1px solid #e5e7eb;
                        }
                        th {
                            background: #f9fafb;
                            color: #374151;
                            text-align: left;
                            padding: 9px;
                            border-bottom: 1px solid #e5e7eb;
                            font-size: 11px;
                            text-transform: uppercase;
                        }
                        td {
                            padding: 9px;
                            border-bottom: 1px solid #e5e7eb;
                            vertical-align: top;
                        }
                        .center { text-align: center; }
                        .right { text-align: right; }
                        .strong { font-weight: 800; }
                        .muted { color: #6b7280; font-size: 11px; }
                        .qr {
                            width: 82px;
                            text-align: center;
                        }
                        .qr img {
                            width: 64px;
                            height: 64px;
                        }
                        .summary {
                            width: 42%%;
                            margin-left: auto;
                            margin-top: 12px;
                            border: 1px solid #e5e7eb;
                        }
                        .summary-line {
                            display: table;
                            width: 100%%;
                            padding: 8px 10px;
                            border-bottom: 1px solid #e5e7eb;
                        }
                        .summary-line:last-child { border-bottom: 0; background: #fff7ed; }
                        .summary-label, .summary-value { display: table-cell; }
                        .summary-label { color: #6b7280; }
                        .summary-value { text-align: right; font-weight: 800; }
                        .total { color: #dc2626; font-size: 15px; }
                        .route-table { margin-top: 8px; }
                        .route-table th { font-size: 10px; }
                        .route-table td { font-size: 11px; }
                        .tag {
                            display: inline-block;
                            border-radius: 4px;
                            padding: 2px 6px;
                            background: #fee2e2;
                            color: #991b1b;
                            font-weight: 800;
                            font-size: 9px;
                            text-transform: uppercase;
                        }
                        .footer {
                            margin-top: 22px;
                            padding-top: 10px;
                            border-top: 1px solid #e5e7eb;
                            color: #6b7280;
                            font-size: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <div class="brand">
                            <h1>VÉ TÀU</h1>
                            <div>Hóa đơn và vé điện tử</div>
                        </div>
                        <div class="meta">
                            <div>Mã đặt chỗ</div>
                            <div class="code">#%s</div>
                            <div class="badge">%s</div>
                        </div>
                    </div>

                    <div class="section">
                        <div class="section-title">Thông tin hành trình</div>
                        <div class="grid">
                            <div class="row">
                                <div class="cell">
                                    <div class="label">Tuyến</div>
                                    <div class="value">%s → %s</div>
                                </div>
                                <div class="cell">
                                    <div class="label">Tàu</div>
                                    <div class="value">%s</div>
                                </div>
                            </div>
                            <div class="row">
                                <div class="cell">
                                    <div class="label">Giờ đi</div>
                                    <div class="value">%s</div>
                                </div>
                                <div class="cell">
                                    <div class="label">Giờ đến</div>
                                    <div class="value">%s</div>
                                </div>
                            </div>
                            <div class="row">
                                <div class="cell">
                                    <div class="label">Thanh toán</div>
                                    <div class="value">%s - %s</div>
                                </div>
                                <div class="cell">
                                    <div class="label">Ngày xuất vé</div>
                                    <div class="value">%s</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    %s

                    <div class="section">
                        <div class="section-title">Hành khách và chỗ ngồi</div>
                        <table>
                            <thead>
                                <tr>
                                    <th class="center">#</th>
                                    <th>Hành khách</th>
                                    <th>Toa</th>
                                    <th>Ghế</th>
                                    <th class="right">Giá vé</th>
                                    <th class="center">QR</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                    </div>

                    <div class="summary">
                        <div class="summary-line">
                            <div class="summary-label">Tạm tính</div>
                            <div class="summary-value">%s</div>
                        </div>
                        <div class="summary-line">
                            <div class="summary-label">Mã giảm giá</div>
                            <div class="summary-value">%s</div>
                        </div>
                        <div class="summary-line">
                            <div class="summary-label">Giảm giá</div>
                            <div class="summary-value">-%s</div>
                        </div>
                        <div class="summary-line">
                            <div class="summary-label">Tổng thanh toán</div>
                            <div class="summary-value total">%s</div>
                        </div>
                    </div>

                    <div class="footer">
                        Vui lòng xuất trình giấy tờ tùy thân trùng với thông tin hành khách khi lên tàu.
                        Hóa đơn/vé này được tạo tự động từ hệ thống đặt vé trực tuyến.
                    </div>
                </body>
                </html>
                """.formatted(
                esc(String.valueOf(booking.getBookingId())),
                esc(valueOrDash(booking.getStatus())),
                esc(valueOrDash(booking.getDepartureStation())),
                esc(valueOrDash(booking.getArrivalStation())),
                esc(valueOrDash(booking.getTrainCode())),
                esc(dateTime(booking.getDepartureTime())),
                esc(dateTime(booking.getArrivalTime())),
                esc(valueOrDash(booking.getPaymentMethod())),
                esc(valueOrDash(booking.getPaymentStatus())),
                esc(DATE_FORMATTER.format(LocalDateTime.now())),
                routeHtml,
                rows,
                esc(money(booking.getOriginalPrice())),
                esc(valueOrDash(booking.getPromoCode())),
                esc(money(booking.getDiscountAmount())),
                esc(money(booking.getTotalPrice()))
        );
    }

    private String buildRouteHtml(BookingDetailResponse booking) {
        if (booking == null || booking.getTripId() == null) {
            return "";
        }

        List<TripStop> stops = tripScheduleRepository.findStopsByTripId(booking.getTripId()).stream()
                .sorted(Comparator.comparing(TripStop::getStopOrder))
                .toList();
        if (stops.isEmpty()) {
            return "";
        }

        List<TripSegment> segments = tripScheduleRepository.findSegmentsByTripId(booking.getTripId()).stream()
                .sorted(Comparator.comparing(TripSegment::getSegmentOrder))
                .toList();
        Long departureStationId = booking.getDepartureStationId();
        Long arrivalStationId = booking.getArrivalStationId();
        if ((departureStationId == null || arrivalStationId == null)
                && booking.getDetails() != null
                && !booking.getDetails().isEmpty()) {
            BookingDetailResponse.TicketDetail detail = booking.getDetails().get(0);
            if (departureStationId == null) {
                departureStationId = detail.getDepartureStationId();
            }
            if (arrivalStationId == null) {
                arrivalStationId = detail.getArrivalStationId();
            }
        }

        TripStop departureStop = findStopByStationId(stops, departureStationId);
        TripStop arrivalStop = findStopByStationId(stops, arrivalStationId);
        if (departureStop == null) {
            departureStop = stops.get(0);
        }
        if (arrivalStop == null) {
            arrivalStop = stops.get(stops.size() - 1);
        }
        if (departureStop.getStopOrder() == null
                || arrivalStop.getStopOrder() == null
                || departureStop.getStopOrder() >= arrivalStop.getStopOrder()) {
            return "";
        }

        int fromOrder = departureStop.getStopOrder();
        int toOrder = arrivalStop.getStopOrder();
        List<TripStop> routeStops = stops.stream()
                .filter(stop -> stop.getStopOrder() != null)
                .filter(stop -> stop.getStopOrder() >= fromOrder && stop.getStopOrder() <= toOrder)
                .toList();
        List<TripSegment> routeSegments = segments.stream()
                .filter(segment -> segment.getSegmentOrder() != null)
                .filter(segment -> segment.getSegmentOrder() >= fromOrder && segment.getSegmentOrder() < toOrder)
                .toList();

        StringBuilder stopRows = new StringBuilder();
        for (int i = 0; i < routeStops.size(); i++) {
            TripStop stop = routeStops.get(i);
            boolean isFirst = i == 0;
            boolean isLast = i == routeStops.size() - 1;
            stopRows.append("""
                    <tr>
                        <td class="center">%d</td>
                        <td><span class="tag">%s</span></td>
                        <td class="strong">%s</td>
                        <td>%s</td>
                        <td>%s</td>
                    </tr>
                    """.formatted(
                    i + 1,
                    esc(isFirst ? "Ga đi" : isLast ? "Ga đến" : "Dừng"),
                    esc(stationName(stop)),
                    esc(dateTime(arrivalTimeOf(stop))),
                    esc(dateTime(departureTimeOf(stop)))
            ));
        }

        StringBuilder segmentRows = new StringBuilder();
        for (int i = 0; i < routeSegments.size(); i++) {
            TripSegment segment = routeSegments.get(i);
            segmentRows.append("""
                    <tr>
                        <td class="center">%d</td>
                        <td class="strong">%s -> %s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td class="right">%s</td>
                    </tr>
                    """.formatted(
                    i + 1,
                    esc(stationName(segment.getFromStop())),
                    esc(stationName(segment.getToStop())),
                    esc(dateTime(departureTimeOf(segment.getFromStop()))),
                    esc(dateTime(arrivalTimeOf(segment.getToStop()))),
                    esc(distance(segment.getDistanceKm()))
            ));
        }

        return """
                <div class="section">
                    <div class="section-title">Lịch trình chặng đã đặt</div>
                    <table class="route-table">
                        <thead>
                            <tr>
                                <th class="center">#</th>
                                <th>Loại ga</th>
                                <th>Ga</th>
                                <th>Đến lúc</th>
                                <th>Rời lúc</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                    <div class="section-title" style="margin-top:12px;">Chi tiết từng chặng</div>
                    <table class="route-table">
                        <thead>
                            <tr>
                                <th class="center">#</th>
                                <th>Chặng</th>
                                <th>Rời ga</th>
                                <th>Đến ga</th>
                                <th class="right">Km</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
                """.formatted(stopRows.toString(), segmentRows.toString());
    }

    private TripStop findStopByStationId(List<TripStop> stops, Long stationId) {
        if (stationId == null) {
            return null;
        }
        return stops.stream()
                .filter(stop -> stop.getStation() != null)
                .filter(stop -> Objects.equals(stop.getStation().getId(), stationId))
                .findFirst()
                .orElse(null);
    }

    private LocalDateTime arrivalTimeOf(TripStop stop) {
        if (stop == null) {
            return null;
        }
        if (stop.getActualArrivalTime() != null) {
            return stop.getActualArrivalTime();
        }
        if (stop.getEstimatedArrivalTime() != null) {
            return stop.getEstimatedArrivalTime();
        }
        return stop.getScheduledArrivalTime();
    }

    private LocalDateTime departureTimeOf(TripStop stop) {
        if (stop == null) {
            return null;
        }
        if (stop.getActualDepartureTime() != null) {
            return stop.getActualDepartureTime();
        }
        if (stop.getEstimatedDepartureTime() != null) {
            return stop.getEstimatedDepartureTime();
        }
        return stop.getScheduledDepartureTime();
    }

    private String stationName(TripStop stop) {
        return stop != null && stop.getStation() != null ? valueOrDash(stop.getStation().getName()) : "-";
    }

    private String distance(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0 ₫";
        }
        return NumberFormat.getCurrencyInstance(VIETNAM).format(value);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String esc(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
