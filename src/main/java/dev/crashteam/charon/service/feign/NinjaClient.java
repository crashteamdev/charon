package dev.crashteam.charon.service.feign;

import dev.crashteam.charon.model.dto.ninja.ConversionDto;
import dev.crashteam.charon.model.dto.ninja.ExchangeRateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "ninjaClient", url = "${app.integration.ninja.url}")
public interface NinjaClient {
    @GetMapping("/convertcurrency")
    ConversionDto convert(@RequestHeader("X-Api-Key") String apiKey,
                          @RequestParam("have") String have,
                          @RequestParam("want") String want,
                          @RequestParam("amount") String amount);

    @GetMapping("/exchangerate")
    ExchangeRateDto exchangeRate(@RequestHeader("X-Api-Key") String apiKey,
                                 @RequestParam(value = "pair") String pair);
}
