package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InitRequestDTO(
        @JsonProperty("TerminalKey") String terminalKey,
        @JsonProperty("Amount")       Long   amount,
        @JsonProperty("OrderId")      String orderId,
        @JsonProperty("Token")        String token,
        @JsonProperty("Description")  String description,
        @JsonProperty("CustomerKey")  String customerKey,
        @JsonProperty("Recurrent")    String recurrent
) {}
