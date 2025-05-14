package dev.crashteam.charon.model;

public enum RecurrentPaymentParams {

    PAYMENT_ID("PAYMENT_ID"),
    AMOUNT("AMOUNT"),
    PROVIDER_RECURRENT_PAYMENT_ID("PROVIDER_RECURRENT_PAYMENT_ID");

    private final String title;

    RecurrentPaymentParams(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
