package dev.crashteam.charon.model.dto.ninja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeRateDto {

    @JsonProperty(value = "currency_pair")
    private String currencyPair;
    @JsonProperty(value = "exchange_rate")
    private String exchangeRate;
}
