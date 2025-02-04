package dev.crashteam.charon.decoder;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YookassaErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new RetryableException(
                response.status(),
                "Yokassa request failed",
                response.request().httpMethod(),
                null,
                response.request()
        );
    }
}
