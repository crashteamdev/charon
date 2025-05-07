package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetStateRequestDTO(
        @JsonProperty("TerminalKey") String terminalKey,
        @JsonProperty("PaymentId")   String paymentId,
        @JsonProperty("Token")       String token
) {}
