package dev.crashteam.charon.service.feign;

import dev.crashteam.charon.model.dto.lava.LavaRequest;
import dev.crashteam.charon.model.dto.lava.LavaResponse;
import dev.crashteam.charon.model.dto.lava.LavaStatusRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "lavaClient", url = "${app.integration.lava.url}")
public interface LavaClient {

    @PostMapping("/create")
    LavaResponse create(@RequestHeader("Signature") String signature,
                         @RequestBody LavaRequest request);

    @PostMapping("/status")
    LavaResponse status(@RequestHeader("Signature") String signature,
                                 @RequestBody LavaStatusRequest request);
}
