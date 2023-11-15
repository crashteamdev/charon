package dev.crashteam.charon.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.crashteam.charon.component.LavaProperties;
import dev.crashteam.charon.exception.IntegrationException;
import dev.crashteam.charon.mapper.integration.LavaPaymentMapper;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.dto.lava.LavaRequest;
import dev.crashteam.charon.model.dto.lava.LavaResponse;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.service.CurrencyService;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.charon.service.feign.LavaClient;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LavaService implements PaymentResolver {

    private final LavaClient lavaClient;
    private final CurrencyService currencyService;
    private final LavaProperties lavaProperties;
    private final PaymentService paymentService;
    private final LavaPaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PaymentSystem getPaymentSystem() {
        return PaymentSystem.PAYMENT_SYSTEM_LAVA;
    }

    @Override
    public PaymentData createPayment(PaymentCreateRequest request, String amount) {
        log.info("Processing lava payment");
        String paymentId = UUID.randomUUID().toString();
        String convertedAmount = currencyService.getConvertedAmount("USD", "RUB", amount);
        LavaRequest lavaRequest = paymentMapper.getRequest(request, paymentId, convertedAmount);

        String signature = generateSignature(lavaRequest);
        LavaResponse lavaResponse = lavaClient.create(signature, lavaRequest);

        PaymentData paymentData = new PaymentData();
        LavaResponse.LavaData lavaData = lavaResponse.getData();
        if (lavaData != null) {
            paymentData.setPaymentId(paymentId);
            paymentData.setCreatedAt(LocalDateTime.now());
            paymentData.setEmail(PaymentProtoUtils.getEmailFromRequest(request));
            paymentData.setPhone(PaymentProtoUtils.getPhoneFromRequest(request));
            paymentData.setStatus(RequestPaymentStatus.PENDING);
            paymentData.setProviderAmount(String.valueOf(PaymentProtoUtils.getMinorMoneyAmount(convertedAmount)));
            paymentData.setProviderCurrency("RUB");
            paymentData.setConfirmationUrl(lavaData.getUrl());
            paymentData.setProviderId(lavaData.getId());
            paymentData.setDescription(lavaData.getComment());
            return paymentData;
        }
        String userId = PaymentProtoUtils.getUserIdFromRequest(request);
        throw new IntegrationException("Error creating lava request for user - %s".formatted(userId) );
    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {
        Payment payment = paymentService.findByExternalId(paymentId);
        if (payment == null) {
            throw new EntityNotFoundException();
        }
        //TODO: узнать виды статусов у LAVA
        return RequestPaymentStatus.SUCCESS;
    }

    @SneakyThrows
    private String generateSignature(LavaRequest lavaRequest) {
        String json = objectMapper.writeValueAsString(lavaRequest);
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key =
                new SecretKeySpec(lavaProperties.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return Hex.encodeHexString(sha256_HMAC.doFinal(json.getBytes(StandardCharsets.UTF_8)));
    }
}
