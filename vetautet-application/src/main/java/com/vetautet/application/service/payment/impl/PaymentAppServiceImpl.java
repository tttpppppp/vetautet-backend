package com.vetautet.application.service.payment.impl;

import com.vetautet.application.dto.MomoCreatePaymentResponse;
import com.vetautet.application.dto.MomoIpnRequest;
import com.vetautet.application.dto.MomoIpnResponse;
import com.vetautet.application.dto.VnpayCreatePaymentResponse;
import com.vetautet.application.dto.VnpayIpnResponse;
import com.vetautet.application.dto.VnpayReturnResponse;
import com.vetautet.application.service.order.BookingAppService;
import com.vetautet.application.service.payment.PaymentAppService;
import com.vetautet.domain.gateway.MomoPaymentGateway;
import com.vetautet.domain.gateway.VnpayPaymentGateway;
import com.vetautet.domain.model.Booking;
import com.vetautet.domain.model.MomoCreatePaymentCommand;
import com.vetautet.domain.model.MomoCreatePaymentResult;
import com.vetautet.domain.model.MomoPaymentResult;
import com.vetautet.domain.model.Payment;
import com.vetautet.domain.model.User;
import com.vetautet.domain.model.VnpayCreatePaymentCommand;
import com.vetautet.domain.model.VnpayCreatePaymentResult;
import com.vetautet.domain.model.VnpayPaymentResult;
import com.vetautet.domain.service.BookingDomainService;
import com.vetautet.domain.service.PaymentDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentAppServiceImpl implements PaymentAppService {

    private static final long MOMO_MIN_AMOUNT = 1000L;
    private static final long VNPAY_MIN_AMOUNT = 1000L;

    private final BookingDomainService bookingDomainService;
    private final BookingAppService bookingAppService;
    private final PaymentDomainService paymentDomainService;
    private final MomoPaymentGateway momoPaymentGateway;
    private final VnpayPaymentGateway vnpayPaymentGateway;

    public PaymentAppServiceImpl(BookingDomainService bookingDomainService,
                                 BookingAppService bookingAppService,
                                 PaymentDomainService paymentDomainService,
                                 MomoPaymentGateway momoPaymentGateway,
                                 VnpayPaymentGateway vnpayPaymentGateway) {
        this.bookingDomainService = bookingDomainService;
        this.bookingAppService = bookingAppService;
        this.paymentDomainService = paymentDomainService;
        this.momoPaymentGateway = momoPaymentGateway;
        this.vnpayPaymentGateway = vnpayPaymentGateway;
    }

    @Override
    public MomoCreatePaymentResponse createMomoPayment(Long bookingId) {
        Booking booking = bookingDomainService.getBookingById(bookingId);
        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Chi co the tao thanh toan MoMo cho booking dang PENDING");
        }

        long amount = booking.getTotalPrice()
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        if (amount < MOMO_MIN_AMOUNT) {
            throw new RuntimeException("MoMo yeu cau so tien toi thieu " + MOMO_MIN_AMOUNT + " VND");
        }

        User user = booking.getUser();
        MomoCreatePaymentResult result = momoPaymentGateway.createPayment(
                MomoCreatePaymentCommand.builder()
                        .bookingId(booking.getId())
                        .amount(amount)
                        .orderInfo("Thanh toan don dat ve #" + booking.getId())
                        .customerName(user != null ? user.getName() : null)
                        .customerEmail(user != null ? user.getEmail() : null)
                        .customerPhone(user != null ? user.getPhone() : null)
                        .build()
        );
        savePendingPayment(booking.getId(), "MOMO", BigDecimal.valueOf(result.getAmount()), result.getOrderId());

        return MomoCreatePaymentResponse.builder()
                .bookingId(bookingId)
                .momoOrderId(result.getOrderId())
                .requestId(result.getRequestId())
                .amount(result.getAmount())
                .resultCode(result.getResultCode())
                .message(result.getMessage())
                .payUrl(result.getPayUrl())
                .deeplink(result.getDeeplink())
                .qrCodeUrl(result.getQrCodeUrl())
                .deeplinkMiniApp(result.getDeeplinkMiniApp())
                .build();
    }

    @Override
    public VnpayCreatePaymentResponse createVnpayPayment(Long bookingId, String clientIp) {
        Booking booking = bookingDomainService.getBookingById(bookingId);
        if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Chi co the tao thanh toan VNPAY cho booking dang PENDING");
        }

        long amount = normalizeAmount(booking.getTotalPrice());
        if (amount < VNPAY_MIN_AMOUNT) {
            throw new RuntimeException("VNPAY yeu cau so tien toi thieu " + VNPAY_MIN_AMOUNT + " VND");
        }

        String orderInfo = "Thanh toan don dat ve " + booking.getId();
        VnpayCreatePaymentResult result = vnpayPaymentGateway.createPayment(
                VnpayCreatePaymentCommand.builder()
                        .bookingId(booking.getId())
                        .amount(amount)
                        .orderInfo(orderInfo)
                        .clientIp(clientIp)
                        .build()
        );
        savePendingPayment(booking.getId(), "VNPAY", BigDecimal.valueOf(result.getAmount()), result.getTxnRef());

        return VnpayCreatePaymentResponse.builder()
                .bookingId(bookingId)
                .vnpTxnRef(result.getTxnRef())
                .amount(result.getAmount())
                .orderInfo(result.getOrderInfo())
                .paymentUrl(result.getPaymentUrl())
                .build();
    }

    @Override
    @Transactional
    public MomoIpnResponse handleMomoIpn(MomoIpnRequest request) {
        return handleMomoResult(request);
    }

    @Override
    @Transactional
    public MomoIpnResponse handleMomoReturn(MomoIpnRequest request) {
        return handleMomoResult(request);
    }

    private MomoIpnResponse handleMomoResult(MomoIpnRequest request) {
        MomoPaymentResult paymentResult = MomoPaymentResult.builder()
                .partnerCode(request.getPartnerCode())
                .orderId(request.getOrderId())
                .requestId(request.getRequestId())
                .amount(request.getAmount())
                .orderInfo(request.getOrderInfo())
                .orderType(request.getOrderType())
                .transId(request.getTransId())
                .resultCode(request.getResultCode())
                .message(request.getMessage())
                .payType(request.getPayType())
                .responseTime(request.getResponseTime())
                .extraData(request.getExtraData())
                .signature(request.getSignature())
                .build();

        if (!momoPaymentGateway.verifyPaymentResult(paymentResult)) {
            return new MomoIpnResponse(1, "Invalid MoMo signature");
        }

        Long bookingId = extractBookingId(request.getOrderId());
        Booking booking = bookingDomainService.getBookingById(bookingId);

        if (request.getResultCode() != null && request.getResultCode() == 0) {
            updatePaymentResult(bookingId, "MOMO", "SUCCESS", BigDecimal.valueOf(request.getAmount()),
                    request.getTransId() != null ? request.getTransId().toString() : request.getOrderId());
            if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                bookingAppService.confirmPayment(bookingId);
            }
            return new MomoIpnResponse(0, "Payment confirmed");
        }

        updatePaymentResult(bookingId, "MOMO", "FAILED", BigDecimal.valueOf(request.getAmount()), request.getOrderId());
        return new MomoIpnResponse(0, "Payment result received: " + request.getMessage());
    }

    @Override
    @Transactional
    public VnpayReturnResponse handleVnpayReturn(Map<String, String> params) {
        VnpayPaymentResult paymentResult = VnpayPaymentResult.builder()
                .params(params)
                .build();

        if (!vnpayPaymentGateway.verifyPaymentResult(paymentResult)) {
            return buildVnpayResponse(params, null, null, "97", "Invalid VNPAY signature");
        }

        String txnRef = params.get("vnp_TxnRef");
        Long bookingId = extractVnpayBookingId(txnRef);
        Booking booking = bookingDomainService.getBookingById(bookingId);
        Long paidAmount = parseVnpayAmount(params.get("vnp_Amount"));
        long bookingAmount = normalizeAmount(booking.getTotalPrice());

        if (paidAmount == null || paidAmount != bookingAmount) {
            return buildVnpayResponse(params, bookingId, paidAmount, "04", "Invalid VNPAY amount");
        }

        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            updatePaymentResult(bookingId, "VNPAY", "SUCCESS", BigDecimal.valueOf(paidAmount),
                    resolveVnpayTransactionId(params));
            if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                bookingAppService.confirmPayment(bookingId);
            }
            return buildVnpayResponse(params, bookingId, paidAmount, "00", "Payment confirmed");
        }

        updatePaymentResult(bookingId, "VNPAY", "FAILED", BigDecimal.valueOf(paidAmount),
                resolveVnpayTransactionId(params));
        return buildVnpayResponse(params, bookingId, paidAmount, responseCode, "Payment result received");
    }

    @Override
    @Transactional
    public VnpayIpnResponse handleVnpayIpn(Map<String, String> params) {
        try {
            VnpayPaymentResult paymentResult = VnpayPaymentResult.builder()
                    .params(params)
                    .build();

            if (!vnpayPaymentGateway.verifyPaymentResult(paymentResult)) {
                return new VnpayIpnResponse("97", "Invalid Checksum");
            }

            Booking booking;
            Long bookingId;
            try {
                bookingId = extractVnpayBookingId(params.get("vnp_TxnRef"));
                booking = bookingDomainService.getBookingById(bookingId);
            } catch (RuntimeException e) {
                return new VnpayIpnResponse("01", "Order not Found");
            }

            Long paidAmount = parseVnpayAmount(params.get("vnp_Amount"));
            long bookingAmount = normalizeAmount(booking.getTotalPrice());
            if (paidAmount == null || paidAmount.longValue() != bookingAmount) {
                return new VnpayIpnResponse("04", "Invalid Amount");
            }

            if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
                return new VnpayIpnResponse("02", "Order already confirmed");
            }

            String responseCode = params.get("vnp_ResponseCode");
            String transactionStatus = params.get("vnp_TransactionStatus");
            if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                updatePaymentResult(bookingId, "VNPAY", "SUCCESS", BigDecimal.valueOf(paidAmount),
                        resolveVnpayTransactionId(params));
                bookingAppService.confirmPayment(bookingId);
            } else {
                updatePaymentResult(bookingId, "VNPAY", "FAILED", BigDecimal.valueOf(paidAmount),
                        resolveVnpayTransactionId(params));
            }

            return new VnpayIpnResponse("00", "Confirm Success");
        } catch (Exception e) {
            return new VnpayIpnResponse("99", "Unknown error");
        }
    }

    private Long extractBookingId(String momoOrderId) {
        if (momoOrderId == null || !momoOrderId.startsWith("BOOKING-")) {
            throw new RuntimeException("Invalid MoMo orderId: " + momoOrderId);
        }

        String[] parts = momoOrderId.split("-");
        if (parts.length < 2) {
            throw new RuntimeException("Invalid MoMo orderId: " + momoOrderId);
        }

        return Long.parseLong(parts[1]);
    }

    private Long extractVnpayBookingId(String txnRef) {
        if (txnRef == null || !txnRef.matches("\\d{14,}")) {
            throw new RuntimeException("Invalid VNPAY vnp_TxnRef: " + txnRef);
        }

        return Long.parseLong(txnRef.substring(0, txnRef.length() - 13));
    }

    private Long parseVnpayAmount(String rawAmount) {
        if (rawAmount == null || rawAmount.isBlank()) {
            return null;
        }
        return Long.parseLong(rawAmount) / 100L;
    }

    private long normalizeAmount(BigDecimal amount) {
        return amount
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private void savePendingPayment(Long bookingId, String method, BigDecimal amount, String transactionId) {
        paymentDomainService.savePayment(Payment.builder()
                .bookingId(bookingId)
                .method(method)
                .amount(amount)
                .status("PENDING")
                .transactionId(transactionId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void updatePaymentResult(Long bookingId,
                                     String method,
                                     String status,
                                     BigDecimal amount,
                                     String transactionId) {
        Payment payment = paymentDomainService.findLatestByBookingIdOrNull(bookingId);
        if (payment == null) {
            payment = Payment.builder()
                    .bookingId(bookingId)
                    .method(method)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        payment.setMethod(method);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setTransactionId(transactionId);
        if ("SUCCESS".equalsIgnoreCase(status)) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentDomainService.savePayment(payment);
    }

    private String resolveVnpayTransactionId(Map<String, String> params) {
        if (params == null) {
            return null;
        }
        String transactionNo = params.get("vnp_TransactionNo");
        if (transactionNo != null && !transactionNo.isBlank()) {
            return transactionNo;
        }
        return params.get("vnp_TxnRef");
    }

    private VnpayReturnResponse buildVnpayResponse(Map<String, String> params,
                                                   Long bookingId,
                                                   Long amount,
                                                   String code,
                                                   String message) {
        return VnpayReturnResponse.builder()
                .code(code)
                .message(message)
                .bookingId(bookingId)
                .vnpTxnRef(params != null ? params.get("vnp_TxnRef") : null)
                .transactionNo(params != null ? params.get("vnp_TransactionNo") : null)
                .responseCode(params != null ? params.get("vnp_ResponseCode") : null)
                .transactionStatus(params != null ? params.get("vnp_TransactionStatus") : null)
                .amount(amount)
                .build();
    }
}
