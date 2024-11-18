package dev.crashteam.charon.service.feign.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

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
}
