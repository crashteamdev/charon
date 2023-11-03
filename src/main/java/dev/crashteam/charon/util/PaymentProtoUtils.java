package dev.crashteam.charon.util;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentStatus;

import java.math.BigDecimal;
import java.util.Optional;

public class PaymentProtoUtils {


    public static String getUrlFromRequest(PaymentCreateRequest request) {
        return Optional.of(request.getPaymentDepositUserBalance().getReturnUrl())
                .orElse(request.getPaymentPurchaseService().getReturnUrl());
    }

    public static String getEmailFromRequest(PaymentCreateRequest request) {
        return Optional.of(request.getPaymentDepositUserBalance().getUserEmail())
                .orElse(request.getPaymentPurchaseService().getUserEmail());
    }

    public static String getPhoneFromRequest(PaymentCreateRequest request) {
        return Optional.of(request.getPaymentDepositUserBalance().getUserPhone())
                .orElse(request.getPaymentPurchaseService().getUserPhone());
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

    public static BigDecimal getMajorMoneyAmount(Long amount) {
        BigDecimal actualAmount = BigDecimal.valueOf(amount);
        return actualAmount.movePointLeft(2);
    }

    public static BigDecimal getMinorMoneyAmount(Long amount) {
        BigDecimal actualAmount = BigDecimal.valueOf(amount);
        return actualAmount.movePointRight(2);
    }

    public static BigDecimal getMajorMoneyAmount(String amount) {
        BigDecimal actualAmount = BigDecimal.valueOf(Double.parseDouble(amount));
        return actualAmount.movePointLeft(2);
    }

    public static BigDecimal getMinorMoneyAmount(String amount) {
        BigDecimal actualAmount = BigDecimal.valueOf(Double.parseDouble(amount));
        return actualAmount.movePointRight(2);
    }
}
