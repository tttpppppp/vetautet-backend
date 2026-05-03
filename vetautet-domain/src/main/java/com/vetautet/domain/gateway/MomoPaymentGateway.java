package com.vetautet.domain.gateway;

import com.vetautet.domain.model.MomoCreatePaymentCommand;
import com.vetautet.domain.model.MomoCreatePaymentResult;
import com.vetautet.domain.model.MomoPaymentResult;

public interface MomoPaymentGateway {
    MomoCreatePaymentResult createPayment(MomoCreatePaymentCommand command);
    boolean verifyPaymentResult(MomoPaymentResult result);
}
