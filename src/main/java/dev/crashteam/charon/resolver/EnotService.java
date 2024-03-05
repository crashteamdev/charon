package dev.crashteam.charon.resolver;

import dev.crashteam.charon.component.EnotProperties;
import dev.crashteam.charon.exception.IntegrationException;
import dev.crashteam.charon.mapper.integration.EnotPaymentMapper;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.dto.enot.EnotPaymentCreateRequest;
import dev.crashteam.charon.model.dto.enot.EnotPaymentCreateResponse;
import dev.crashteam.charon.model.dto.enot.EnotPaymentStatusResponse;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.service.CurrencyService;
import dev.crashteam.charon.service.feign.EnotClient;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnotService implements PaymentResolver {

    private final CurrencyService currencyService;
    private final EnotClient enotClient;
    private final EnotPaymentMapper paymentMapper;
    private final EnotProperties enotProperties;
    private final PaymentRepository paymentRepository;

    @Override
    public PaymentSystem getPaymentSystem() {
        return PaymentSystem.PAYMENT_SYSTEM_ENOT;
    }

    @Override
    public PaymentData createPayment(PaymentCreateRequest request, String amount) {
        log.info("Processing enot payment");
        String paymentId = UUID.randomUUID().toString();
        BigDecimal exchangeRate = currencyService.getExchangeRate("RUB");
        BigDecimal convertedAmount = (BigDecimal.valueOf(Double.parseDouble(amount))
                .multiply(exchangeRate.setScale(2, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
        EnotPaymentCreateRequest paymentCreateRequest = paymentMapper.createRequest(request, paymentId, convertedAmount);
        log.info("Creating payment with data - {}", paymentCreateRequest);
        EnotPaymentCreateResponse response = enotClient.create(enotProperties.getSecretKey(), paymentCreateRequest);

        PaymentData paymentData = new PaymentData();
        EnotPaymentCreateResponse.EnotPaymentData enotData = response.getData();
        if (enotData != null) {
            paymentData.setPaymentId(paymentId);
            paymentData.setCreatedAt(LocalDateTime.now());
            paymentData.setEmail(PaymentProtoUtils.getEmailFromRequest(request));
            paymentData.setPhone(PaymentProtoUtils.getPhoneFromRequest(request));
            paymentData.setStatus(RequestPaymentStatus.PENDING);
            paymentData.setProviderAmount(String.valueOf(PaymentProtoUtils.getMinorMoneyAmount(String.valueOf(convertedAmount))));
            paymentData.setProviderCurrency(enotData.getCurrency());
            paymentData.setConfirmationUrl(enotData.getUrl());
            paymentData.setProviderId(enotData.getId());
            paymentData.setDescription(null);
            paymentData.setExchangeRate(exchangeRate);
            return paymentData;
        }
        String userId = PaymentProtoUtils.getUserIdFromRequest(request);
        throw new IntegrationException("Error creating enot request for user - %s".formatted(userId));
    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findByExternalId(paymentId).orElse(null);
        if (payment == null) {
            throw new EntityNotFoundException();
        }
        EnotPaymentStatusResponse status = enotClient
                .status(enotProperties.getSecretKey(), paymentId, enotProperties.getShopId());
        EnotPaymentStatusResponse.EnotPaymentStatusResponseData statusData = status.getData();
        if (statusData != null && statusData.getStatus() != null) {
            log.info("Got ENOT payment status - {} for payment - {}", statusData.getStatus(), paymentId);
            return switch (statusData.getStatus()) {
                case "success" -> RequestPaymentStatus.SUCCESS;
                case "fail" -> RequestPaymentStatus.FAILED;
                case "expired" -> RequestPaymentStatus.CANCELED;
                default -> RequestPaymentStatus.PENDING;
            };
        }
        log.info("ENOT payment status maybe null or unknown. Data - {} for payment - {}", statusData, paymentId);
        return RequestPaymentStatus.PENDING;
    }
}
