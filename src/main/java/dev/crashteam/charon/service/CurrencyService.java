package dev.crashteam.charon.service;

import dev.crashteam.charon.model.domain.CurrencyRate;
import dev.crashteam.charon.service.feign.CurrencyApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyApiClient client;
    private final CurrencyRateService rateService;

    @Value("${app.integration.currency-api.api-key}")
    private String apiKey;

    @Cacheable("exchangeRate")
    public BigDecimal getExchangeRate(String currency) {
        try {
            BigDecimal value = client.exchangeRate(apiKey, currency).getData().get(currency).getValue();

            CurrencyRate currencyRate = rateService.getByCurrencyAndInitCurrency(currency, "USD") != null ?
                    rateService.getByCurrencyAndInitCurrency(currency, "USD") : new CurrencyRate();
            currencyRate.setCurrency(currency);
            currencyRate.setInitCurrency("USD");
            currencyRate.setRate(value);
            rateService.save(currencyRate);

            return value;
        } catch (Exception e) {
            log.warn("Exception while trying to get exchange rates, getting latest known rate from DB");
            return rateService.getByCurrencyAndInitCurrency(currency, "USD").getRate();
        }
    }
}
