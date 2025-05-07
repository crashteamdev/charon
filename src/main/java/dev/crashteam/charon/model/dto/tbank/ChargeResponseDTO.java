package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChargeResponseDTO(
        @JsonProperty("Success")   Boolean success,
        @JsonProperty("ErrorCode") String  errorCode,
        @JsonProperty("PaymentId") String  paymentId,
        @JsonProperty("Status")    String  status
) {}
