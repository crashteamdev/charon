package dev.crashteam.charon.model.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeDto {

    private Map<String, ExchangeData> data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExchangeData {
        private String code;
        private BigDecimal value;
    }
}
