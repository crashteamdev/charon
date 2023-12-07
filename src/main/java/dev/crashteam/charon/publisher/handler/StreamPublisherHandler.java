package dev.crashteam.charon.publisher.handler;

import dev.crashteam.charon.model.domain.Payment;

public interface StreamPublisherHandler<T> {

     T publishPaymentStatusChangeMessage(Payment payment);

     T publishPaymentCreatedMessage(Payment payment);
}
