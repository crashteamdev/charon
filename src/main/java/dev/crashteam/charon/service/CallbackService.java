package dev.crashteam.charon.service;

import dev.crashteam.charon.component.ClickProperties;
import dev.crashteam.charon.model.Operation;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.model.dto.FkCallbackData;
import dev.crashteam.charon.model.dto.click.ClickRequest;
import dev.crashteam.charon.model.dto.click.ClickResponse;
import dev.crashteam.charon.publisher.handler.StreamPublisherHandler;
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
    private final StreamPublisherHandler publisherHandler;
    private final ClickProperties clickProperties;
    private final UserService userService;

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
        if (payment == null) {
            log.error("Payment with id - {} not found", callbackData.getPaymentId());
            return;
        }
        if (payment.getStatus().equals(RequestPaymentStatus.SUCCESS)) {
            log.warn("Freekassa callback. Payment with id - {} already in success status", callbackData.getPaymentId());
            return;
        }
        BigDecimal callbackAmount = PaymentProtoUtils.getMinorMoneyAmount(callbackData.getAmount());
        if (!payment.getProviderAmount().equals(callbackAmount.longValue())) {
            payment.setStatus(RequestPaymentStatus.FAILED);
            paymentService.save(payment);
            publisherHandler.publishPaymentStatusChangeMessage(payment);
            return;
        }
        if (payment.getOperationType().getType().equals(Operation.DEPOSIT_BALANCE.getTitle())) {
            log.info("Payment with id [{}] successful, processing balance deposit", payment.getPaymentId());
            User user = userService.getUser(payment.getUser().getId());
            user.setBalance(user.getBalance() + payment.getAmount());
            userService.saveUser(user);
        }
        payment.setStatus(RequestPaymentStatus.SUCCESS);
        publisherHandler.publishPaymentStatusChangeMessage(payment);
        paymentService.save(payment);

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

        if (amount == null) {
            response.setError(-2L);
            response.setErrorNote("Incorrect parameter amount");
            return response;
        }

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

        if (error != null && error == -5017) {
            response.setError(-9L);
            response.setErrorNote("Transaction cancelled");
            response.setMerchantPrepareId(merchantPrepareId);
            if (!payment.getStatus().equals(RequestPaymentStatus.CANCELED)) {
                payment.setStatus(RequestPaymentStatus.CANCELED);
                paymentService.save(payment);
            }
            publisherHandler.publishPaymentStatusChangeMessage(payment);
            return response;
        }

        BigDecimal amountClick = amount.movePointRight(2);
        BigDecimal providerAmount = BigDecimal.valueOf(payment.getProviderAmount());
        log.info("Comparing amount - ours: [{}] ; click - [{}]", providerAmount, amountClick);
        if (amountClick.compareTo(providerAmount) != 0) {
            log.warn("Incorrect amount for payment - {}", payment.getPaymentId());
            response.setError(-2L);
            response.setErrorNote("Incorrect parameter amount");
            if (payment.getOperationId() != null) {
                response.setMerchantPrepareId(Long.valueOf(payment.getOperationId()));
            }
            return response;
        }

        if (!request.getSignString().equals(md5Hex)) {
            response.setError(-1L);
            response.setErrorNote("SIGN CHECK FAILED!");
            if (payment.getOperationId() != null) {
                response.setMerchantPrepareId(Long.valueOf(payment.getOperationId()));
            }
            return response;
        }

        payment.setStatus(RequestPaymentStatus.SUCCESS);
        paymentService.save(payment);
        if (payment.getOperationType().getType().equals(Operation.DEPOSIT_BALANCE.getTitle())) {
            log.info("Payment with id [{}] successful, processing balance deposit", payment.getPaymentId());
            User user = userService.getUser(payment.getUser().getId());
            user.setBalance(user.getBalance() + payment.getAmount());
            userService.saveUser(user);
        }
        publisherHandler.publishPaymentStatusChangeMessage(payment);
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

        if (amount == null) {
            response.setError(-2L);
            response.setErrorNote("Incorrect parameter amount");
            return response;
        }

        String md5Hex = DigestUtils.md5Hex("%s%s%s%s%s%s%s".formatted(clickTransId, serviceId, clickProperties.getSecretKey(),
                merchantTransId, amount.toString(), action, signTime));

        BigDecimal amountClick = amount.movePointRight(2);
        BigDecimal providerAmount = BigDecimal.valueOf(payment.getProviderAmount());
        log.info("Comparing amount - ours: [{}] ; click - [{}]", providerAmount, amountClick);
        if (amountClick.compareTo(providerAmount) != 0) {
            log.warn("Incorrect amount for payment - {}", payment.getPaymentId());
            response.setError(-2L);
            response.setErrorNote("Incorrect parameter amount");
            return response;
        }

        if (!request.getSignString().equals(md5Hex)) {
            log.info("Sign string incorrect for click payment - {}", payment.getPaymentId());
            response.setError(-1L);
            response.setErrorNote("SIGN CHECK FAILED!");
            return response;
        }

        if (payment.getOperationId() == null) {
            Long prepareId = paymentService.getOperationIdSeq();
            response.setMerchantPrepareId(prepareId);
            payment.setOperationId(String.valueOf(prepareId));
        }
        if (clickTransId != null) {
            payment.setExternalId(String.valueOf(clickTransId));
        }
        paymentService.save(payment);

        response.setError(0L);
        response.setErrorNote("Success");
        return response;
    }

}
