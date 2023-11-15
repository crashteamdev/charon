package dev.crashteam.charon.exception;

public class IntegrationException extends RuntimeException {
    public IntegrationException() {
    }

    public IntegrationException(String message) {
        super(message);
    }
}
