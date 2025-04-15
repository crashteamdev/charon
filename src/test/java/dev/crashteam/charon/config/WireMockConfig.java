package dev.crashteam.charon.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
@ImportAutoConfiguration({FeignAutoConfiguration.class})
public class WireMockConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer mockClient() {
        return new WireMockServer(8085);
    }
}
