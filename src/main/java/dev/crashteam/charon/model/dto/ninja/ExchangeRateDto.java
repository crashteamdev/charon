package dev.crashteam.charon.model.dto.ninja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeRateDto {

    @JsonProperty(value = "currency_pair")
    private String currencyPair;
    @JsonProperty(value = "exchange_rate")
    private String exchangeRate;
}
