package dev.crashteam.charon.service.feign;

import dev.crashteam.charon.model.dto.currency.ExchangeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "currencyApiClient", url = "${app.integration.currency-api.url}")
public interface CurrencyApiClient {
    @GetMapping("/latest")
    ExchangeDto exchangeRate(@RequestHeader("apikey") String apiKey,
                             @RequestParam("currencies") String currency);

}
