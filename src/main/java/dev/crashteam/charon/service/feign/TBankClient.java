package dev.crashteam.charon.service.feign;

import dev.crashteam.charon.model.dto.tbank.*;
import dev.crashteam.charon.service.feign.config.TinkoffFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "tinkoffClient",
        url  = "${tinkoff.base-url}",
        configuration = TinkoffFeignConfig.class
)
public interface TBankClient {

    @PostMapping("/Init")
    InitResponseDTO init(@RequestBody InitRequestDTO request);

    @PostMapping("/Charge")
    ChargeResponseDTO charge(@RequestBody ChargeRequestDTO request);

    @PostMapping("/GetState")
    GetStateResponseDTO getState(@RequestBody GetStateRequestDTO request);
}
