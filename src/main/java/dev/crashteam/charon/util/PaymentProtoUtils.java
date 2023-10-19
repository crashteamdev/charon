package dev.crashteam.charon.util;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentStatus;

import java.util.Optional;

public class PaymentProtoUtils {


    public static String getUrlFromRequest(PaymentCreateRequest request) {
        return Optional.of(request.getPaymentDepositUserBalance().getReturnUrl())
                .orElse(request.getPaymentPurchaseService().getReturnUrl());
    }

    public static String getDescriptionFromRequest(PaymentCreateRequest request) {
        return Optional.of(request.getPaymentDepositUserBalance().getDescription())
                .orElse(request.getPaymentPurchaseService().getDescription());
    }

    public static String getUserIdFromRequest(PaymentCreateRequest request) {
        return Optional.of(request.getPaymentDepositUserBalance().getUserId())
                .orElse(request.getPaymentPurchaseService().getUserId());
    }

    public static RequestPaymentStatus getStatus(int status) {
        return switch (PaymentStatus.forNumber(status)) {
            case PAYMENT_STATUS_SUCCESS -> RequestPaymentStatus.SUCCESS;
            case PAYMENT_STATUS_CANCELED -> RequestPaymentStatus.CANCELED;
            case PAYMENT_STATUS_PENDING -> RequestPaymentStatus.PENDING;
            default -> throw new IllegalArgumentException();
        };
    }
}
