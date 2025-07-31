package dev.crashteam.charon.model.dto.tbank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record InitRequestDTO(
        @JsonProperty("TerminalKey") String terminalKey,
        @JsonProperty("Amount")       Long   amount,
        @JsonProperty("OrderId")      String orderId,
        @JsonProperty("Token")        String token,
        @JsonProperty("Description")  String description,
        @JsonProperty("CustomerKey")  String customerKey,
        @JsonProperty("Recurrent")    String recurrent
) {}
