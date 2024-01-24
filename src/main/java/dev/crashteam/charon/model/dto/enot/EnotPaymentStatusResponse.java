package dev.crashteam.charon.model.dto.enot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnotPaymentStatusResponse {

    private EnotPaymentStatusResponseData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EnotPaymentStatusResponseData {
        @JsonProperty("invoice_id")
        private String invoiceId;
        @JsonProperty("invoice_amount")
        private BigDecimal invoiceAmount;
        @JsonProperty("order_id")
        private String orderId;
        private String currency;
        private String shopId;
        private String status;
    }
}
