package dev.crashteam.charon.util;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.payment.PaymentStatus;

public class PaymentProtoUtils {

    public static RequestPaymentStatus getStatus(int status) {
        return switch (PaymentStatus.forNumber(status)) {
            case SUCCESS -> RequestPaymentStatus.SUCCEEDED;
            case CANCELED -> RequestPaymentStatus.CANCELED;
            case PENDING -> RequestPaymentStatus.PENDING;
            default -> throw new IllegalArgumentException();
        };
    }
}
