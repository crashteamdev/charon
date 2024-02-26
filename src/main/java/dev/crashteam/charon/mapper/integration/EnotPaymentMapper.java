package dev.crashteam.charon.mapper.integration;

import dev.crashteam.charon.component.EnotProperties;
import dev.crashteam.charon.model.dto.enot.EnotPaymentCreateRequest;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EnotPaymentMapper {

    private final EnotProperties enotProperties;

    public EnotPaymentCreateRequest createRequest(PaymentCreateRequest request, String paymentId, BigDecimal amount) {
        return EnotPaymentCreateRequest.builder()
                .currency("RUB")
                .amount(amount)
                .orderId(paymentId)
                .shopId(enotProperties.getShopId())
                .expire(60L)
                .successUrl(PaymentProtoUtils.getUrlFromRequest(request))
                .failUrl(PaymentProtoUtils.getUrlFromRequest(request)).build();

    }
}
