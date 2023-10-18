package dev.crashteam.charon.model;

public enum PaymentSystemType {
    PAYMENT_SYSTEM_UNKNOWN("PAYMENT_SYSTEM_UNKNOWN"),
    PAYMENT_SYSTEM_YOOKASSA("PAYMENT_SYSTEM_YOOKASSA"),
    PAYMENT_SYSTEM_FREEKASSA("PAYMENT_SYSTEM_FREEKASSA"),
    PAYMENT_SYSTEM_UZUM_BANK("PAYMENT_SYSTEM_UZUM_BANK"),
    UNRECOGNIZED("UNRECOGNIZED");

    private final String title;

    PaymentSystemType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
