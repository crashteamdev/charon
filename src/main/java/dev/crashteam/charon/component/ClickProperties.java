package dev.crashteam.charon.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "click")
public class ClickProperties {

    private String baseUrl;
    private String merchantId;
    private String serviceId;
    private String secretKey;
    private String merchantUserId;
}
