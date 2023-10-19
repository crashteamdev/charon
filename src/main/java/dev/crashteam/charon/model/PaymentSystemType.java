package dev.crashteam.charon.model;

import java.util.Arrays;

public enum PaymentSystemType {
    PAYMENT_SYSTEM_UNKNOWN("PAYMENT_SYSTEM_UNKNOWN", 0),
    PAYMENT_SYSTEM_YOOKASSA("PAYMENT_SYSTEM_YOOKASSA", 1),
    PAYMENT_SYSTEM_FREEKASSA("PAYMENT_SYSTEM_FREEKASSA", 2),
    PAYMENT_SYSTEM_UZUM_BANK("PAYMENT_SYSTEM_UZUM_BANK", 3),
    UNRECOGNIZED("UNRECOGNIZED", -1);

    private final String title;
    private final int numberValue;

    PaymentSystemType(String title, int numberValue) {
        this.title = title;
        this.numberValue = numberValue;
    }

    public static PaymentSystemType getByTitle(String title) {
        return Arrays.stream(PaymentSystemType.values())
                .filter(it -> it.title.equals(title))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public String getTitle() {
        return title;
    }

    public int getNumberValue() {
        return numberValue;
    }
}
