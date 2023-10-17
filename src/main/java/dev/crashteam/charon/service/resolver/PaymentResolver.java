package dev.crashteam.charon.service.resolver;

import dev.crashteam.charon.mapper.integration.PaymentMapper;
import dev.crashteam.charon.model.PaymentData;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;

public interface PaymentResolver {

    PaymentSystem getPaymentSystem();

    PaymentData createPayment(PaymentCreateRequest request, String currency, String amount);
}
