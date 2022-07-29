package dev.crashteam.charon.service;

import dev.crashteam.charon.mapper.YookassaPaymentMapper;
import dev.crashteam.charon.model.dto.yookassa.PaymentCreateRequestDTO;
import dev.crashteam.charon.model.dto.yookassa.PaymentRefundRequestDTO;
import dev.crashteam.charon.model.dto.yookassa.PaymentRefundResponseDTO;
import dev.crashteam.charon.model.dto.yookassa.PaymentResponseDTO;
import dev.crashteam.charon.service.feign.YookassaClient;
import dev.crashteam.payment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class YookassaService implements PaymentResolver {

    private final YookassaClient kassaClient;
    private final YookassaPaymentMapper paymentMapper;
    private final PaymentService paymentService;

    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        PaymentCreateRequestDTO paymentRequestDto = paymentMapper.getCreatePaymentRequestDto(request);
        PaymentResponseDTO response = kassaClient.createPayment(paymentRequestDto);
        paymentService.saveFromPaymentResponse(response, request.getUserId(), request.getId());
        return paymentMapper.getPaymentResponse(response);
    }

    public PaymentRecurrentResponse createRecurrentPayment(RecurrentPaymentCreateRequest request) {
        PaymentCreateRequestDTO requestDto = paymentMapper.getRecurrentPaymentRequestDto(request);
        PaymentResponseDTO response = kassaClient.createPayment(requestDto);
        paymentService.saveFromRecurrentPaymentResponse(response, request.getUserId(), request.getId());
        return paymentMapper.getRecurrentPaymentResponse(response);
    }

    public PaymentRefundResponse refundPayment(PaymentRefundRequest request) {
        PaymentRefundRequestDTO refundRequestDto = paymentMapper.getPaymentRefundRequestDto(request);
        PaymentRefundResponseDTO response = kassaClient.refund(refundRequestDto);
        paymentService.saveFromRefundResponse(response, request.getUserId(), request.getPaymentId());
        return paymentMapper.getPaymentRefundResponse(response);
    }
}
