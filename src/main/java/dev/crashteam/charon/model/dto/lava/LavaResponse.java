package dev.crashteam.charon.model.dto.lava;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LavaResponse {

    private LavaData data;
    private String status;
    @JsonProperty("status_check")
    private Boolean statusCheck;
    private String error;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LavaData {
        private String id;
        private BigDecimal amount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime expired;
        private String status;
        @JsonProperty("shop_id")
        private String shopId;
        private String url;
        private String comment;
        @JsonProperty("fail_url")
        private String failUrl;
        @JsonProperty("success_url")
        private String successUrl;
        @JsonProperty("hook_url")
        private String hookUrl;
        @JsonProperty("custom_fields")
        private String custom_fields;

    }
}
