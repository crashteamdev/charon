package dev.crashteam.charon.exception;

public class NoSuchPaymentTypeException extends RuntimeException {
    public NoSuchPaymentTypeException() {
        super();
    }

    public NoSuchPaymentTypeException(String message) {
        super(message);
    }
}
