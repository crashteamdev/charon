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
        String merchantTransId = request.getMerchantTransId();
        Long action = request.getAction();

        if (action == 0) {
            return getPrepareClickAction(request);
        } else if (action == 1) {
            return getCompleteClickAction(request);
        }
        ClickResponse response = new ClickResponse();
        response.setClickTransId(clickTransId);
        response.setMerchantTransId(merchantTransId);
        response.setError(-3L);
        response.setErrorNote("Action not found");
        response.setMerchantPrepareId(null);
        return response;
    }

    @Transactional
    public void freeKassaCallback(FkCallbackData callbackData) {

        Payment payment = paymentService.findByPaymentId(callbackData.getPaymentId());
        if (payment == null) throw new EntityNotFoundException("Payment with id - %s not found"
                .formatted(callbackData.getPaymentId()));
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

    private boolean checkClickRequestOnError(ClickRequest request, Payment payment, String hex) {
        BigDecimal amount = request.getAmount();
        return !amount.equals(BigDecimal.valueOf(payment.getProviderAmount()).movePointLeft(2)) ||
                !request.getSignString().equals(hex);
    }

    @Transactional
    public ClickResponse getCompleteClickAction(ClickRequest request) {
        log.info("Got COMPLETE action from CLICK");

        Long clickTransId = request.getClickTransId();
        Long serviceId = request.getServiceId();
        String merchantTransId = request.getMerchantTransId();
        Long merchantPrepareId = request.getMerchantPrepareId();
        BigDecimal amount = request.getAmount();
        Long action = request.getAction();
        String signTime = request.getSignTime();
        Long error = request.getError();

        ClickResponse response = new ClickResponse();
        response.setClickTransId(clickTransId);
        response.setMerchantTransId(merchantTransId);

        String md5Hex = DigestUtils.md5Hex("%s%s%s%s%s%s%s%s".formatted(clickTransId, serviceId, clickProperties.getSecretKey(),
                merchantTransId, merchantPrepareId, amount.toString(), action, signTime));

        Payment payment = paymentService.findByOperationId(String.valueOf(merchantPrepareId));

        if (payment == null) {
            response.setError(-6L);
            response.setErrorNote("Transaction does not exist");
            response.setMerchantPrepareId(merchantPrepareId);
            return response;
        }

        if (payment.getStatus().equals(RequestPaymentStatus.SUCCESS)) {
            response.setError(-4L);
            response.setErrorNote("Already paid");
            response.setMerchantPrepareId(merchantPrepareId);
            return response;
        }

        if (payment.getStatus().equals(RequestPaymentStatus.CANCELED)) {
            response.setError(-9L);
            response.setErrorNote("Transaction cancelled");
            response.setMerchantPrepareId(merchantPrepareId);
            return response;
        }

        if (error == -5017) {
            response.setError(-9L);
            response.setErrorNote("Transaction cancelled");
            response.setMerchantPrepareId(merchantPrepareId);
            if (!payment.getStatus().equals(RequestPaymentStatus.CANCELED)) {
                payment.setStatus(RequestPaymentStatus.CANCELED);
                paymentService.save(payment);
            }
            streamService.publishPaymentStatusChangeAwsMessage(payment);
            return response;
        }

        if (!amount.equals(BigDecimal.valueOf(payment.getProviderAmount()).movePointLeft(2))) {
            response.setError(-2L);
            response.setErrorNote("Incorrect parameter amount");
            response.setMerchantPrepareId(Long.valueOf(payment.getOperationId()));
            return response;
        }

        if (!request.getSignString().equals(md5Hex)) {
            response.setError(-1L);
            response.setErrorNote("SIGN CHECK FAILED!");
            response.setMerchantPrepareId(Long.valueOf(payment.getOperationId()));
            return response;
        }

        payment.setStatus(RequestPaymentStatus.SUCCESS);
        paymentService.save(payment);
        streamService.publishPaymentStatusChangeAwsMessage(payment);
        response.setError(0L);
        response.setErrorNote("Success");
        response.setMerchantConfirmId(null);
        return response;
    }

    @Transactional
    public ClickResponse getPrepareClickAction(ClickRequest request) {
        Long clickTransId = request.getClickTransId();
        Long serviceId = request.getServiceId();
        String merchantTransId = request.getMerchantTransId();
        BigDecimal amount = request.getAmount();
        Long action = request.getAction();
        String signTime = request.getSignTime();
        log.info("Got PREPARE action from CLICK");

        ClickResponse response = new ClickResponse();
        response.setClickTransId(clickTransId);
        response.setMerchantTransId(merchantTransId);

        Payment payment = paymentService.findByPaymentId(merchantTransId);
        if (payment == null) {
            response.setError(-5L);
            response.setErrorNote("User does not exist");
            return response;
        }

        String md5Hex = DigestUtils.md5Hex("%s%s%s%s%s%s%s".formatted(clickTransId, serviceId, clickProperties.getSecretKey(),
                merchantTransId, amount.toString(), action, signTime));

        if (!amount.equals(BigDecimal.valueOf(payment.getProviderAmount()).movePointLeft(2))) {
            response.setError(-2L);
            response.setErrorNote("Incorrect parameter amount");
            response.setMerchantPrepareId(Long.valueOf(payment.getOperationId()));
            return response;
        }

        if (!request.getSignString().equals(md5Hex)) {
            response.setError(-1L);
            response.setErrorNote("SIGN CHECK FAILED!");
            response.setMerchantPrepareId(Long.valueOf(payment.getOperationId()));
            return response;
        }

        if (payment.getOperationId() == null) {
            Long prepareId = paymentService.getOperationIdSeq();
            response.setMerchantPrepareId(prepareId);
            payment.setOperationId(String.valueOf(prepareId));
        }

        payment.setExternalId(String.valueOf(clickTransId));
        paymentService.save(payment);

        response.setError(0L);
        response.setErrorNote("Success");
        return response;
    }

}
