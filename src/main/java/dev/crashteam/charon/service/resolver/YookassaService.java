package dev.crashteam.charon.service.resolver;

import dev.crashteam.charon.mapper.integration.YookassaPaymentMapper;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentCreateRequestDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundRequestDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundResponseDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentResponseDTO;
import dev.crashteam.charon.service.CurrencyService;
import dev.crashteam.charon.service.feign.YookassaClient;
import dev.crashteam.payment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class YookassaService implements PaymentResolver {

    private final YookassaClient kassaClient;
    private final CurrencyService currencyService;
    private final YookassaPaymentMapper yookassaPaymentMapper;

    public PaymentData createPayment(PaymentCreateRequest request, String amount) {
        String convertedAmount = currencyService.getConvertedAmount("USD", "RUB", amount);
        YkPaymentCreateRequestDTO paymentRequestDto = yookassaPaymentMapper
                .getCreatePaymentRequestDto(request, convertedAmount);
        YkPaymentResponseDTO responseDTO = kassaClient.createPayment(paymentRequestDto);
        PaymentData paymentData = new PaymentData();
        paymentData.setId(responseDTO.getId());
        paymentData.setCreatedAt(responseDTO.getCreatedAt());
        paymentData.setStatus(responseDTO.getStatus());
        paymentData.setCurrency("RUB");
        paymentData.setValue(responseDTO.getAmount().getValue());
        paymentData.setDescription(responseDTO.getDescription());
        paymentData.setConfirmationUrl(paymentData.getConfirmationUrl());
        return paymentData;
    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {
        return yookassaPaymentMapper.getPaymentStatus(kassaClient.paymentStatus(paymentId).getStatus());
    }

    @Deprecated
    public YkPaymentResponseDTO createRecurrentPayment(RecurrentPaymentCreateRequest request) {
        YkPaymentCreateRequestDTO requestDto = yookassaPaymentMapper.getRecurrentPaymentRequestDto(request);
        return kassaClient.createPayment(requestDto);

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
