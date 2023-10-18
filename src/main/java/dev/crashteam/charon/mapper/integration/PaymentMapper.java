package dev.crashteam.charon.mapper.integration;

import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentCreateResponse;

public interface PaymentMapper {

    Payment getPaymentServiceEntity(PaymentData response, PaymentCreateRequest request);

    Payment getPaymentBalanceEntity(PaymentData response, PaymentCreateRequest request);
    PaymentCreateResponse getPaymentResponse(Object response);

}
