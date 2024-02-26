package dev.crashteam.charon.service.feign;

import dev.crashteam.charon.model.dto.enot.EnotPaymentCreateRequest;
import dev.crashteam.charon.model.dto.enot.EnotPaymentCreateResponse;
import dev.crashteam.charon.model.dto.enot.EnotPaymentStatusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "enotClient", url = "${app.integration.enot.url}")
public interface EnotClient {

    @PostMapping("/create")
    EnotPaymentCreateResponse create(@RequestHeader("x-api-key") String apiKey,
                                     EnotPaymentCreateRequest request);

    @GetMapping("/info")
    EnotPaymentStatusResponse status(@RequestHeader("x-api-key") String apiKey,
                                     @RequestParam("invoice_id") String invoiceId,
                                     @RequestParam("shop_id") String shopId);
}
