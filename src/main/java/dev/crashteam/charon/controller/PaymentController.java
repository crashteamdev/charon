package dev.crashteam.charon.controller;

import dev.crashteam.charon.component.FreeKassaProperties;
import dev.crashteam.charon.model.dto.FkCallbackData;
import dev.crashteam.charon.model.dto.click.ClickRequest;
import dev.crashteam.charon.model.dto.click.ClickResponse;
import dev.crashteam.charon.service.CallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final FreeKassaProperties freeKassaProperties;

    private final CallbackService callbackService;

    @PostMapping(value = "/callback/freekassa",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<String> callbackFreekassa(@RequestParam Map<String, String> attributes) {
        String merchantId = attributes.get("MERCHANT_ID");
        String amount = attributes.get("AMOUNT");
        String orderId = attributes.get("MERCHANT_ORDER_ID");
        String paymentId = attributes.get("us_paymentid");
        String curId = attributes.get("CUR_ID");
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

    @PostMapping(value = "/callback/click",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClickResponse> callbackClick(@RequestParam Map<String, String> attributes) {

        log.info("Callback CLICK payment. Body={}", attributes);
        String clickTransId = attributes.get("click_trans_id");
        String serviceId = attributes.get("service_id");
        String clickPaydocId = attributes.get("click_paydoc_id");
        String merchantTransId = attributes.get("merchant_trans_id");
        String prepareId = attributes.get("merchant_prepare_id");
        String amount = attributes.get("amount");
        String action = attributes.get("action");
        String error = attributes.get("error");
        String errorNote = attributes.get("error_note");
        String signTime = attributes.get("sign_time");
        String signString = attributes.get("sign_string");

        ClickRequest clickRequest = new ClickRequest();
        clickRequest.setClickTransId(StringUtils.hasText(clickTransId) ? Long.valueOf(clickTransId) : null);
        clickRequest.setClickPaydocId(StringUtils.hasText(clickPaydocId) ? Long.valueOf(clickPaydocId) : null);
        clickRequest.setAmount(StringUtils.hasText(amount) ? BigDecimal.valueOf(Double.parseDouble(amount)) : null);
        clickRequest.setError(StringUtils.hasText(error) ? Long.valueOf(error) : null);
        clickRequest.setAction(StringUtils.hasText(action) ? Long.valueOf(action) : null);
        clickRequest.setErrorNote(errorNote);
        clickRequest.setServiceId(StringUtils.hasText(serviceId) ? Long.valueOf(serviceId) : null);
        clickRequest.setMerchantTransId(merchantTransId);
        clickRequest.setMerchantPrepareId(StringUtils.hasText(prepareId) ? Long.valueOf(prepareId) : null);
        clickRequest.setSignString(signString);
        clickRequest.setSignTime(signTime);
        return ResponseEntity.ok(callbackService.clickResponse(clickRequest));
    }
}


