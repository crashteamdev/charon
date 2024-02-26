package dev.crashteam.charon.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.integration.enot")
public class EnotProperties {

    private String url;
    private String shopId;
    private String secretKey;
}
