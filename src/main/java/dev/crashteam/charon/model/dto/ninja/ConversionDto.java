package dev.crashteam.charon.model.dto.ninja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConversionDto {

    @JsonProperty(value = "new_amount")
    private String newAmount;
    @JsonProperty(value = "new_currency")
    private String newCurrency;
    @JsonProperty(value = "old_currency")
    private String oldCurrency;
    @JsonProperty(value = "old_amount")
    private String oldAmount;
}
