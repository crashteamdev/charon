package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetStateResponseDTO(
        @JsonProperty("Success")   Boolean success,
        @JsonProperty("ErrorCode") String  errorCode,
        @JsonProperty("Status")    String  status,
        @JsonProperty("OrderId")   String  orderId,
        @JsonProperty("PaymentId") String  paymentId
) {}
