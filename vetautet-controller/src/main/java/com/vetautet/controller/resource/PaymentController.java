package com.vetautet.controller.resource;

import com.vetautet.application.dto.MomoCreatePaymentResponse;
import com.vetautet.application.dto.MomoIpnRequest;
import com.vetautet.application.dto.MomoIpnResponse;
import com.vetautet.application.dto.VnpayCreatePaymentResponse;
import com.vetautet.application.dto.VnpayIpnResponse;
import com.vetautet.application.dto.VnpayReturnResponse;
import com.vetautet.application.service.payment.PaymentAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentAppService paymentAppService;

    public PaymentController(PaymentAppService paymentAppService) {
        this.paymentAppService = paymentAppService;
    }

    @PostMapping("/momo/bookings/{bookingId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<MomoCreatePaymentResponse> createMomoPayment(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentAppService.createMomoPayment(bookingId));
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<MomoIpnResponse> handleMomoIpn(@RequestBody MomoIpnRequest request) {
        return ResponseEntity.ok(paymentAppService.handleMomoIpn(request));
    }

    @GetMapping("/momo/return")
    public ResponseEntity<MomoIpnResponse> handleMomoReturn(@ModelAttribute MomoIpnRequest request) {
        return ResponseEntity.ok(paymentAppService.handleMomoReturn(request));
    }

    @PostMapping("/vnpay/bookings/{bookingId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<VnpayCreatePaymentResponse> createVnpayPayment(@PathVariable Long bookingId,
                                                                         HttpServletRequest request) {
        return ResponseEntity.ok(paymentAppService.createVnpayPayment(bookingId, resolveClientIp(request)));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<VnpayReturnResponse> handleVnpayReturn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentAppService.handleVnpayReturn(params));
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<VnpayIpnResponse> handleVnpayIpn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(paymentAppService.handleVnpayIpn(params));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
