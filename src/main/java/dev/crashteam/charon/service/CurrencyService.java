package dev.crashteam.charon.service;

import dev.crashteam.charon.model.dto.ninja.ConversionDto;
import dev.crashteam.charon.model.dto.ninja.ExchangeRateDto;
import dev.crashteam.charon.service.feign.NinjaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final NinjaClient client;

    @Value("${app.integration.ninja.api-key}")
    private String apiKey;

    @Cacheable("currency")
    public String getConvertedAmount(String have, String want, String amount) {
        ConversionDto converted = client.convert(apiKey, have, want, amount);
        return converted.getNewAmount();
    }

    @Cacheable("exchangeRate")
    public ExchangeRateDto getExchangeRate(String pair) {
        return client.exchangeRate(apiKey, pair);
    }
}
