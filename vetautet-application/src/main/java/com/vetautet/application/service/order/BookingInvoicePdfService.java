package com.vetautet.application.service.order;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vetautet.application.dto.BookingDetailResponse;
import com.vetautet.application.service.ticket.TicketQrService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class BookingInvoicePdfService {

    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final TicketQrService ticketQrService;

    public BookingInvoicePdfService(TicketQrService ticketQrService) {
        this.ticketQrService = ticketQrService;
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
                rows,
                esc(money(booking.getOriginalPrice())),
                esc(valueOrDash(booking.getPromoCode())),
                esc(money(booking.getDiscountAmount())),
                esc(money(booking.getTotalPrice()))
        );
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
