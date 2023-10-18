package dev.crashteam.charon.service.resolver;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;

public interface PaymentResolver {

    PaymentSystem getPaymentSystem();

    PaymentData createPayment(PaymentCreateRequest request, String amount);

    /***
     * @param paymentId set null if payment system on callbacks
     * @return RequestPaymentStatus.NOT_IMPLEMENTED if payment system on callbacks
     */
    RequestPaymentStatus getPaymentStatus(String paymentId);
}
