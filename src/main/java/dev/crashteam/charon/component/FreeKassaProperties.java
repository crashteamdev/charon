package dev.crashteam.charon.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "freekassa")
public class FreeKassaProperties {

    private String baseUrl;
    private Long shopId;
    private String apiKey;
    private String secretWordFirst;
    private String secretWordSecond;
}
