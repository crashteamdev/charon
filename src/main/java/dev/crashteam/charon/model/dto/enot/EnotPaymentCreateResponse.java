package dev.crashteam.charon.model.dto.enot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnotPaymentCreateResponse {

    private EnotPaymentData data;
    private Long status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EnotPaymentData {
        private String id;
        private String amount;
        private String currency;
        private String url;
    }

}
