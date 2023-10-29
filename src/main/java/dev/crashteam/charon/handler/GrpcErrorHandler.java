package dev.crashteam.charon.handler;

import dev.crashteam.charon.exception.NoConfirmationUrlException;
import dev.crashteam.charon.exception.NoSuchPaymentTypeException;
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
    public Status handleNoConfirmationUrlException(NoConfirmationUrlException e) {
        return Status.UNKNOWN.withDescription("No confirmation_url was returned").withCause(e);
    }

    @GrpcExceptionHandler(NoSuchPaymentTypeException.class)
    public Status handleNoSuchPaymentTypeException(NoSuchPaymentTypeException e) {
        return Status.UNKNOWN.withDescription("No such payment type").withCause(e);
    }


}
