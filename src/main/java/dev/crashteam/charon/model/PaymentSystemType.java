package dev.crashteam.charon.model;

import java.util.Arrays;

public enum PaymentSystemType {
    PAYMENT_SYSTEM_UNKNOWN("PAYMENT_SYSTEM_UNKNOWN", 0, false),
    PAYMENT_SYSTEM_YOOKASSA("PAYMENT_SYSTEM_YOOKASSA", 1, false),
    PAYMENT_SYSTEM_FREEKASSA("PAYMENT_SYSTEM_FREEKASSA", 2, true),
    PAYMENT_SYSTEM_UZUM_BANK("PAYMENT_SYSTEM_UZUM_BANK", 3, true),
    PAYMENT_SYSTEM_CLICK("PAYMENT_SYSTEM_CLICK", 4, true),
    UNRECOGNIZED("UNRECOGNIZED", -1, false);

    private final String title;
    private final int numberValue;

    private final boolean callback;

    PaymentSystemType(String title, int numberValue, boolean callback) {
        this.title = title;
        this.numberValue = numberValue;
        this.callback = callback;
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

    public boolean isCallback() {
        return callback;
    }
}
