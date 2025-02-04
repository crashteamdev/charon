package dev.crashteam.charon.service.feign.config;

import dev.crashteam.charon.decoder.YookassaErrorDecoder;
import dev.crashteam.charon.exception.IntegrationException;
import dev.crashteam.charon.util.FeignUtils;
import feign.RequestInterceptor;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
public class YookassaFeignConfig {

    @Value("${app.integration.yookassa.secretKey}")
    private String secretKey;
    @Value("${app.integration.yookassa.shopId}")
    private String shopId;

    @Bean
    @Primary
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            if (requestTemplate.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                requestTemplate.removeHeader(HttpHeaders.AUTHORIZATION);
            }
            String auth = shopId + ":" + secretKey;
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            requestTemplate.header(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            requestTemplate.header("Idempotence-Key", UUID.randomUUID().toString());
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new YookassaErrorDecoder();
    }

    @Bean
    public Retryer retryer(
            @Value("${app.integration.yookassa.retry.period}") long period,
            @Value("${app.integration.yookassa.retry.duration}") long duration,
            @Value("${app.integration.yookassa.retry.max-attempts}") int maxAttempts
    ) {
        return new YookassaRetryer(
                period,
                TimeUnit.SECONDS.toMillis(duration),
                maxAttempts
        );
    }

    public static class YookassaRetryer implements Retryer {
        private final long period;
        private final long maxPeriod;
        private final int maxAttempts;

        private int attempt;
        private long sleptForMillis;

        public YookassaRetryer(long period, long maxPeriod, int maxAttempts) {
            this.period = period;
            this.maxPeriod = maxPeriod;
            this.maxAttempts = maxAttempts;
            this.attempt = 1;
        }

        protected long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        public void continueOrPropagate(RetryableException e) {
            if (this.attempt++ >= this.maxAttempts) {
                throw new IntegrationException("Retry attempts on yookassa request failed");
            } else {
                FeignUtils.getSleptForMillis(
                        e,
                        this.currentTimeMillis(),
                        this.maxPeriod,
                        this.period,
                        this.attempt,
                        this.sleptForMillis
                ).ifPresent(value -> this.sleptForMillis = value);
            }
        }

        public Retryer clone() {
            return new YookassaRetryer(period, maxPeriod, maxAttempts);
        }
    }
}
