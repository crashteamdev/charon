package dev.crashteam.charon.exception;

public class NoSuchSubscriptionTypeException extends RuntimeException {

    public NoSuchSubscriptionTypeException() {
    }

    public NoSuchSubscriptionTypeException(String message) {
        super(message);
    }
}
