package dev.crashteam.charon.config;

import dev.crashteam.charon.component.FreeKassaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {FreeKassaProperties.class})
public class AppConfig {
}
