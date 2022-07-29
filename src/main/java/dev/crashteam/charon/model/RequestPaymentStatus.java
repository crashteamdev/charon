package dev.crashteam.charon.model;

import java.util.Arrays;

public enum RequestPaymentStatus {

    SUCCEEDED("succeeded"),
    PENDING("pending"),
    CANCELED("canceled");

    private final String title;

    RequestPaymentStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static RequestPaymentStatus getStatus(String name) {
        return Arrays.stream(RequestPaymentStatus.values())
                .filter(it -> it.title.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
