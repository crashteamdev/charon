package dev.crashteam.charon.exception;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException() {
        super();
    }

    public DuplicateTransactionException(String message) {
        super(message);
    }
}
