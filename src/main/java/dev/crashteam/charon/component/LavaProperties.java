package dev.crashteam.charon.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.integration.lava")
public class LavaProperties {

    private String url;
    private String shopId;
    private String secretKey;
}
