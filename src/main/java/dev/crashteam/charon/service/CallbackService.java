package dev.crashteam.charon.service;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.dto.FkCallbackData;
import dev.crashteam.charon.util.PaymentProtoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CallbackService {

    private final PaymentService paymentService;

    @Transactional
    public void freeKassaCallback(FkCallbackData callbackData) {
        Payment payment = paymentService.findByPaymentId(callbackData.getPaymentId());
        BigDecimal callbackAmount = PaymentProtoUtils.getMinorMoneyAmount(callbackData.getAmount());
        if (!payment.getProviderAmount().equals(callbackAmount.longValue())) {
            payment.setStatus(RequestPaymentStatus.FAILED);
            paymentService.save(payment);
            return;
        }
        payment.setStatus(RequestPaymentStatus.SUCCESS);
        paymentService.save(payment);

    }

}
