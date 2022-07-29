package dev.crashteam.charon.handler;

import dev.crashteam.charon.exception.NoConfirmationUrlException;
import io.grpc.Metadata;
import io.grpc.Status;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@GrpcAdvice
public class GrpcErrorHandler {

    @GrpcExceptionHandler(Exception.class)
    public Status handleException(Exception e) {
        return Status.UNKNOWN.withDescription("Unknown server error").withCause(e);
    }

    @GrpcExceptionHandler(NoConfirmationUrlException.class)
    public Status handleNoConfirmationUrlException(Exception e) {
        return Status.UNKNOWN.withDescription("No confirmation_url was returned").withCause(e);
    }


}
