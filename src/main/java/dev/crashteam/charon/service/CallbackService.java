package dev.crashteam.charon.service;

import dev.crashteam.charon.component.ClickProperties;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.dto.FkCallbackData;
import dev.crashteam.charon.model.dto.click.ClickRequest;
import dev.crashteam.charon.model.dto.click.ClickResponse;
import dev.crashteam.charon.stream.StreamService;
import dev.crashteam.charon.util.PaymentProtoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallbackService {

    private final PaymentService paymentService;
    private final StreamService streamService;
    private final ClickProperties clickProperties;

    @Transactional
    public ClickResponse clickResponse(ClickRequest request) {
        Long clickTransId = request.getClickTransId();
        Long serviceId = request.getServiceId();
        String merchantTransId = request.getMerchantTransId();
        Long merchantPrepareId = request.getMerchantPrepareId();
        BigDecimal amount = request.getAmount();
        Long action = request.getAction();
        String signTime = request.getRawSignTime();

        ClickResponse response = new ClickResponse();
        response.setClickTransId(clickTransId);
        response.setMerchantTransId(merchantTransId);
        if (action == 0) {
            log.info("Got PREPARE action from CLICK");
            String md5Hex = DigestUtils.md5Hex("%s%s%s%s%s%s%s".formatted(clickTransId, serviceId, clickProperties.getSecretKey(),
                    merchantTransId, amount.toString(), action, signTime));
            if (!request.getSignString().equals(md5Hex)) {
                //TODO: return error when sign not equals
            }

            Payment payment = paymentService.findByPaymentId(merchantTransId);
            Long prepareId = paymentService.getOperationIdSeq();

            payment.setOperationId(String.valueOf(prepareId));
            payment.setExternalId(String.valueOf(clickTransId));
            paymentService.save(payment);

            response.setMerchantPrepareId(prepareId);
            response.setError(0L);
            response.setErrorNote("");
            return response;
        } else if (action == 1) {
            log.info("Got COMPLETE action from CLICK");
            String md5Hex = DigestUtils.md5Hex("%s%s%s%s%s%s%s%s".formatted(clickTransId, serviceId, clickProperties.getSecretKey(),
                    merchantTransId, merchantPrepareId, amount.toString(), action, signTime));
            if (!request.getSignString().equals(md5Hex)) {
                //TODO: return error when sign not equals
            }

            Payment payment = paymentService.findByOperationId(String.valueOf(merchantPrepareId));
            if (payment == null) {
                throw new  EntityNotFoundException();
            }

            payment.setStatus(RequestPaymentStatus.SUCCESS);
            paymentService.save(payment);

            streamService.publishPaymentStatusChangeAwsMessage(payment);

            response.setError(0L);
            response.setErrorNote("");
            return response;
        }

        return response;
    }

    @Transactional
    public void freeKassaCallback(FkCallbackData callbackData) {

        Payment payment = paymentService.findByPaymentId(callbackData.getPaymentId());
        BigDecimal callbackAmount = PaymentProtoUtils.getMinorMoneyAmount(callbackData.getAmount());
        if (!payment.getProviderAmount().equals(callbackAmount.longValue())) {
            payment.setStatus(RequestPaymentStatus.FAILED);
            paymentService.save(payment);
            streamService.publishPaymentStatusChangeAwsMessage(payment);
            return;
        }
        payment.setStatus(RequestPaymentStatus.SUCCESS);
        streamService.publishPaymentStatusChangeAwsMessage(payment);
        paymentService.save(payment);

    }

}
