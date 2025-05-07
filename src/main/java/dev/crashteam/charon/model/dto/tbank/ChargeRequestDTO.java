package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChargeRequestDTO(
        @JsonProperty("TerminalKey") String terminalKey,
        @JsonProperty("PaymentId")   String paymentId,
        @JsonProperty("RebillId")    String rebillId,
        @JsonProperty("Token")       String token,
        @JsonProperty("IP")          String ip,
        @JsonProperty("SendEmail")   Boolean sendEmail,
        @JsonProperty("InfoEmail")   String infoEmail
) {}
