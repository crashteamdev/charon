package dev.crashteam.charon.exception;

public class PaymentResponseException extends RuntimeException {

    public PaymentResponseException() {
    }

    public PaymentResponseException(String message) {
        super(message);
    }
}
