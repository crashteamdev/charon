package dev.crashteam.charon.mapper.integration;

import dev.crashteam.charon.component.LavaProperties;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.dto.lava.LavaRequest;
import dev.crashteam.charon.model.dto.lava.LavaStatusRequest;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LavaPaymentMapper {

    private final LavaProperties lavaProperties;

    public LavaRequest getRequest(PaymentCreateRequest request, String paymentId, String amount) {
        LavaRequest lavaRequest = new LavaRequest();
        BigDecimal amountDecimal = BigDecimal.valueOf(Double.parseDouble(amount));
        lavaRequest.setSum(amountDecimal);
        lavaRequest.setShopId(lavaProperties.getShopId());
        lavaRequest.setSuccessUrl(PaymentProtoUtils.getUrlFromRequest(request));
        lavaRequest.setOrderId(paymentId);
        return lavaRequest;
    }

    public LavaStatusRequest getStatusRequest(Payment payment) {
        LavaStatusRequest lavaRequest = new LavaStatusRequest();
        lavaRequest.setShopId(lavaProperties.getShopId());
        lavaRequest.setInvoiceId(payment.getExternalId());
        lavaRequest.setOrderId(payment.getPaymentId());
        return lavaRequest;
    }
}
