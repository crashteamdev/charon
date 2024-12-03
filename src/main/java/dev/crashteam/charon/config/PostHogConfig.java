package dev.crashteam.charon.config;

import com.posthog.java.PostHog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostHogConfig {

    private static final String HOST = "https://eu.i.posthog.com";

    @Value("${app.integration.posthog.api-key}")
    private String apiKey;

    @Bean
    public PostHog postHog() {
        return new PostHog.Builder(apiKey).host(HOST).build();
    }

}
