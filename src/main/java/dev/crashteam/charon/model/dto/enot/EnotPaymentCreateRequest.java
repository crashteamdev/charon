package dev.crashteam.charon.model.dto.enot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnotPaymentCreateRequest {

    private BigDecimal amount;
    @JsonProperty("order_id")
    private String orderId;
    private String currency;
    private String shopId;
    @JsonProperty("fail_url")
    private String failUrl;
    @JsonProperty("success_url")
    private String successUrl;
    private Long expire;
}
