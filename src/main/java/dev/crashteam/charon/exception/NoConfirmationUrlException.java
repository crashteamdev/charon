package dev.crashteam.charon.exception;

public class NoConfirmationUrlException extends RuntimeException {

    public NoConfirmationUrlException() {
    }

    public NoConfirmationUrlException(String message) {
        super(message);
    }

    public NoConfirmationUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
