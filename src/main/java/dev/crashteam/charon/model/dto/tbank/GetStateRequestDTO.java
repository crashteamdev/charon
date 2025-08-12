package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetStateRequestDTO(
        @JsonProperty("TerminalKey") String terminalKey,
        @JsonProperty("PaymentId")   String paymentId,
        @JsonProperty("Token")       String token
) {}
