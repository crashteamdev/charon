package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record InitResponseDTO(
        @JsonProperty("Success")    Boolean success,
        @JsonProperty("ErrorCode")  String  errorCode,
        @JsonProperty("PaymentURL") String  paymentURL,
        @JsonProperty("PaymentId")  String  paymentId,
        @JsonProperty("Status")     String  status,
        @JsonProperty("Amount")     Long  amount
) {}
