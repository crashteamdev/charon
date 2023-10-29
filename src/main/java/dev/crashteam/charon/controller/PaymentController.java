package dev.crashteam.charon.controller;

import dev.crashteam.charon.component.FreeKassaProperties;
import dev.crashteam.charon.model.PromoCodeType;
import dev.crashteam.charon.model.dto.FkCallbackData;
import dev.crashteam.charon.model.web.CallbackPaymentAdditionalInfo;
import dev.crashteam.charon.service.CallbackService;
import dev.crashteam.charon.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final FreeKassaProperties freeKassaProperties;

    private final CallbackService callbackService;

    @PostMapping(value = "/freekassa/callback",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> callbackFreekassa(ServerWebExchange exchange) {
        Map<String, Object> attributes = exchange.getAttributes();
        String merchantId = (String) attributes.get("MERCHANT_ID");
        String amount = (String) attributes.get("AMOUNT");
        String orderId = (String) attributes.get("MERCHANT_ORDER_ID");
        String paymentId = (String) attributes.get("us_paymentid");
        String curId = (String) attributes.get("CUR_ID");
        log.info("Callback freekassa payment. Body={}", attributes);
        if (merchantId == null || amount == null || orderId == null || curId == null || paymentId == null) {
            log.warn("Callback payment. Bad request. Body={}", attributes);
            return ResponseEntity.badRequest().build();
        }
        var md5Hash = DigestUtils.md5Hex("%s:%s:%s:%s".formatted(merchantId, amount, freeKassaProperties.getSecretWordSecond(), orderId));
        if (attributes.get("SIGN") != md5Hash) {
            log.warn("Callback payment sign is not valid. expected={}; actual={}", md5Hash, attributes.get("SIGN"));
            return ResponseEntity.badRequest().build();
        }
        callbackService.freeKassaCallback(new FkCallbackData(merchantId, amount, orderId, paymentId, curId));
        return ResponseEntity.ok().build();
    }
}


