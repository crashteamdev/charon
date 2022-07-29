package dev.crashteam.charon.service;

import dev.crashteam.payment.*;

public interface PaymentResolver {

    PaymentCreateResponse createPayment(PaymentCreateRequest request);

    PaymentRecurrentResponse createRecurrentPayment(RecurrentPaymentCreateRequest request);

    PaymentRefundResponse refundPayment(PaymentRefundRequest request);
}
