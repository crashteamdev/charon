package dev.crashteam.charon.util;

import dev.crashteam.charon.exception.NoSuchPaymentTypeException;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.SubscriptionType;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentProtoUtils {


    public static String getUrlFromRequest(PaymentCreateRequest request) {
        return switch (request.getPaymentCase()) {
            case PAYMENT_DEPOSIT_USER_BALANCE -> request.getPaymentDepositUserBalance().getReturnUrl();
            case PAYMENT_PURCHASE_SERVICE -> request.getPaymentPurchaseService().getReturnUrl();
            case GENERIC_PAYMENT_PURCHASE_SERVICE -> request.getGenericPaymentPurchaseService().getReturnUrl();
            case PAYMENT_NOT_SET -> throw new NoSuchPaymentTypeException("No such payment type exists");
        };
    }

    public static String getEmailFromRequest(PaymentCreateRequest request) {
        return switch (request.getPaymentCase()) {
            case PAYMENT_DEPOSIT_USER_BALANCE -> request.getPaymentDepositUserBalance().getUserEmail();
            case PAYMENT_PURCHASE_SERVICE -> request.getPaymentPurchaseService().getUserEmail();
            case GENERIC_PAYMENT_PURCHASE_SERVICE -> "";
            case PAYMENT_NOT_SET -> throw new NoSuchPaymentTypeException("No such payment type exists");
        };
    }

    public static String getPhoneFromRequest(PaymentCreateRequest request) {
        return switch (request.getPaymentCase()) {
            case PAYMENT_DEPOSIT_USER_BALANCE -> request.getPaymentDepositUserBalance().getUserPhone();
            case PAYMENT_PURCHASE_SERVICE -> request.getPaymentPurchaseService().getUserPhone();
            case GENERIC_PAYMENT_PURCHASE_SERVICE -> "";
            case PAYMENT_NOT_SET -> throw new NoSuchPaymentTypeException("No such payment type exists");
        };
    }

    public static String getDescriptionFromRequest(PaymentCreateRequest request) {
        return switch (request.getPaymentCase()) {
            case PAYMENT_DEPOSIT_USER_BALANCE -> request.getPaymentDepositUserBalance().getDescription();
            case PAYMENT_PURCHASE_SERVICE -> request.getPaymentPurchaseService().getDescription();
            case GENERIC_PAYMENT_PURCHASE_SERVICE -> request.getGenericPaymentPurchaseService().getDescription();
            case PAYMENT_NOT_SET -> throw new NoSuchPaymentTypeException("No such payment type exists");
        };
    }

    public static String getUserIdFromRequest(PaymentCreateRequest request) {
        return switch (request.getPaymentCase()) {
            case PAYMENT_DEPOSIT_USER_BALANCE -> request.getPaymentDepositUserBalance().getUserId();
            case PAYMENT_PURCHASE_SERVICE -> request.getPaymentPurchaseService().getUserId();
            case GENERIC_PAYMENT_PURCHASE_SERVICE -> request.getGenericPaymentPurchaseService().getUserId();
            case PAYMENT_NOT_SET -> throw new NoSuchPaymentTypeException("No such payment type exists");
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

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static long multiplyDiscount(long amount, long multiply, SubscriptionType subscriptionType) {
        if (multiply > 1 && subscriptionType != null) {
            BigDecimal discount;
            if (subscriptionType.getName().equals("advanced")) {
                discount = new BigDecimal("0.15");
            } else if (subscriptionType.getName().equals("pro")) {
                discount = new BigDecimal("0.20");
            } else {
                discount = new BigDecimal("0.10");
            }
            return BigDecimal.valueOf(amount).subtract(BigDecimal.valueOf(amount).multiply(discount)).longValue();
        }
        return amount;

    }
}
