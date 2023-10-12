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

    public static PaymentStatus getPaymentStatus(String status) {
        RequestPaymentStatus requestPaymentStatus = RequestPaymentStatus.getStatus(status);
        return switch (requestPaymentStatus) {
            case FAILED -> PaymentStatus.PAYMENT_STATUS_FAILED;
            case UNKNOWN -> PaymentStatus.PAYMENT_STATUS_UNKNOWN;
            case PENDING -> PaymentStatus.PAYMENT_STATUS_PENDING;
            case CANCELED -> PaymentStatus.PAYMENT_STATUS_CANCELED;
            case SUCCESS -> PaymentStatus.PAYMENT_STATUS_SUCCESS;
        };
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
