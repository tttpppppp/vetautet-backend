package com.vetautet.application.service.payment;

import com.vetautet.application.dto.MomoCreatePaymentResponse;
import com.vetautet.application.dto.MomoIpnRequest;
import com.vetautet.application.dto.MomoIpnResponse;
import com.vetautet.application.dto.VnpayCreatePaymentResponse;
import com.vetautet.application.dto.VnpayIpnResponse;
import com.vetautet.application.dto.VnpayReturnResponse;

import java.util.Map;

public interface PaymentAppService {
    MomoCreatePaymentResponse createMomoPayment(Long bookingId);
    MomoIpnResponse handleMomoIpn(MomoIpnRequest request);
    MomoIpnResponse handleMomoReturn(MomoIpnRequest request);
    VnpayCreatePaymentResponse createVnpayPayment(Long bookingId, String clientIp);
    VnpayReturnResponse handleVnpayReturn(Map<String, String> params);
    VnpayIpnResponse handleVnpayIpn(Map<String, String> params);
}
