package dev.crashteam.charon.exception;

public class MessagePublishException extends RuntimeException {
    public MessagePublishException() {
        super();
    }

    public MessagePublishException(String message) {
        super(message);
    }
}
