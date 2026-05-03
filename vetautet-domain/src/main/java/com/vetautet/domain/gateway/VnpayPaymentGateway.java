package com.vetautet.domain.gateway;

import com.vetautet.domain.model.VnpayCreatePaymentCommand;
import com.vetautet.domain.model.VnpayCreatePaymentResult;
import com.vetautet.domain.model.VnpayPaymentResult;

public interface VnpayPaymentGateway {
    VnpayCreatePaymentResult createPayment(VnpayCreatePaymentCommand command);
    boolean verifyPaymentResult(VnpayPaymentResult result);
}
