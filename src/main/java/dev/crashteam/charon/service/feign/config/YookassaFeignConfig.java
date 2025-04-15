package dev.crashteam.charon.service.feign.config;

import dev.crashteam.charon.decoder.YookassaErrorDecoder;
import feign.RequestInterceptor;
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
        return new Retryer.Default(
                period,
                TimeUnit.SECONDS.toMillis(duration),
                maxAttempts
        );
    }
}
