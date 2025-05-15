package dev.crashteam.charon.resolver;

import dev.crashteam.charon.mapper.integration.YookassaPaymentMapper;
import dev.crashteam.charon.model.Operation;
import dev.crashteam.charon.model.PaymentSystemType;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.UserSavedPayment;
import dev.crashteam.charon.model.dto.UserSavedPaymentResolverDto;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.model.dto.yookassa.*;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.service.CurrencyService;
import dev.crashteam.charon.service.UserSavedPaymentService;
import dev.crashteam.charon.service.feign.YookassaClient;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentRefundRequest;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class YookassaService implements PaymentResolver {

    private final YookassaClient kassaClient;
    private final CurrencyService currencyService;
    private final YookassaPaymentMapper yookassaPaymentMapper;
    private final UserSavedPaymentService savedPaymentService;
    private final PaymentRepository paymentRepository;

    public PaymentData createPayment(PaymentCreateRequest request, String amount) {
        //BigDecimal exchangeRate = currencyService.getExchangeRate("RUB");
        BigDecimal convertedAmount = BigDecimal.valueOf(Double.parseDouble(amount));
        YkPaymentCreateRequestDTO paymentRequestDto = yookassaPaymentMapper
                .getCreatePaymentRequestDto(request, String.valueOf(convertedAmount));
        if (request.hasPaymentPurchaseService() && request.getPaymentPurchaseService().getSavePaymentMethod()) {
            paymentRequestDto.setSavePaymentMethod(true);
        }
        YkPaymentResponseDTO responseDTO = kassaClient.createPayment(paymentRequestDto);

        String phone = PaymentProtoUtils.getPhoneFromRequest(request);
        String email = PaymentProtoUtils.getEmailFromRequest(request);

        PaymentData paymentData = new PaymentData();
        paymentData.setPhone(phone);
        paymentData.setEmail(email);
        paymentData.setPaymentId(UUID.randomUUID().toString());
        paymentData.setProviderId(responseDTO.getId());
        paymentData.setCreatedAt(LocalDateTime.now());
        paymentData.setStatus(RequestPaymentStatus.PENDING);
        paymentData.setProviderCurrency("RUB");
        BigDecimal moneyAmount = PaymentProtoUtils.getMinorMoneyAmount(responseDTO.getAmount().getValue());
        paymentData.setProviderAmount(String.valueOf(moneyAmount));
        paymentData.setDescription(responseDTO.getDescription());
        paymentData.setConfirmationUrl(responseDTO.getConfirmation().getConfirmationUrl());
        //paymentData.setExchangeRate(exchangeRate);
        return paymentData;
    }

    @Override
    public PaymentData recurrentPayment(UserSavedPaymentResolverDto savedPaymentDto) {
        YkPaymentCreateRequestDTO paymentRequestDto = yookassaPaymentMapper
                .getRecurrentPaymentRequestDto(savedPaymentDto.getPaymentId(), savedPaymentDto.getAmount());
        YkPaymentResponseDTO responseDTO = kassaClient.createPayment(paymentRequestDto);

        PaymentData paymentData = new PaymentData();
        paymentData.setPaymentId(UUID.randomUUID().toString());
        paymentData.setProviderId(responseDTO.getId());
        paymentData.setCreatedAt(LocalDateTime.now());
        paymentData.setStatus(RequestPaymentStatus.PENDING);
        paymentData.setProviderCurrency("RUB");
        BigDecimal moneyAmount = PaymentProtoUtils.getMinorMoneyAmount(responseDTO.getAmount().getValue());
        paymentData.setProviderAmount(String.valueOf(moneyAmount));
        paymentData.setDescription(responseDTO.getDescription());
        paymentData.setConfirmationUrl(responseDTO.getConfirmation().getConfirmationUrl());

        return paymentData;

    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {
        YkPaymentResponseDTO paymentResponseDTO = kassaClient.paymentStatus(paymentId);
        YkPaymentMethodDTO paymentMethod = paymentResponseDTO.getPaymentMethod();
        RequestPaymentStatus paymentStatus = yookassaPaymentMapper.getPaymentStatus(paymentResponseDTO.getStatus());

        if (paymentStatus.equals(RequestPaymentStatus.SUCCESS) && paymentMethod != null && paymentMethod.getSaved()) {
            Optional<Payment> optionalPayment = paymentRepository.findByExternalId(paymentId);
            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                if (payment.getOperationType().getType().equals(Operation.PURCHASE_SERVICE.getTitle())) {
                    String userId = payment.getUser().getId();
                    UserSavedPayment userSavedPayment = savedPaymentService.findByUserId(userId);
                    if (userSavedPayment != null) {
                        userSavedPayment.setPaymentId(paymentMethod.getId());
                        userSavedPayment.setPaidService(payment.getPaidService());
                        userSavedPayment.setMonthPaid(payment.getMonthPaid());
                        userSavedPayment.setPaymentSystem(PaymentSystemType.PAYMENT_SYSTEM_YOOKASSA.getTitle());
                        savedPaymentService.save(userSavedPayment);
                    } else {
                        UserSavedPayment savedPayment = new UserSavedPayment();
                        savedPayment.setUserId(userId);
                        savedPayment.setPaymentId(paymentMethod.getId());
                        savedPayment.setPaymentSystem(PaymentSystemType.PAYMENT_SYSTEM_YOOKASSA.getTitle());
                        savedPayment.setPaidService(payment.getPaidService());
                        savedPayment.setMonthPaid(payment.getMonthPaid());
                        savedPaymentService.save(savedPayment);
                    }
                }
            }
        }
        return paymentStatus;
    }

    @Deprecated
    public YkPaymentRefundResponseDTO refundPayment(PaymentRefundRequest request) {
        YkPaymentRefundRequestDTO refundRequestDto = yookassaPaymentMapper.getPaymentRefundRequestDto(request);
        return kassaClient.refund(refundRequestDto);
    }

    @Override
    public PaymentSystem getPaymentSystem() {
        return PaymentSystem.PAYMENT_SYSTEM_YOOKASSA;
    }
}
