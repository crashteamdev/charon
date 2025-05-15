package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ChargeResponseDTO(
        @JsonProperty("Success")   Boolean success,
        @JsonProperty("ErrorCode") String  errorCode,
        @JsonProperty("PaymentId") String  paymentId,
        @JsonProperty("Status")    String  status
) {}
