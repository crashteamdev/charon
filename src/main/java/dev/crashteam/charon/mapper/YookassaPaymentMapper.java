package dev.crashteam.charon.mapper;

import com.google.protobuf.Timestamp;
import dev.crashteam.charon.exception.NoConfirmationUrlException;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.dto.yookassa.*;
import dev.crashteam.payment.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class YookassaPaymentMapper {

    public PaymentCreateRequestDTO getCreatePaymentRequestDto(PaymentCreateRequest createRequest) {
        PaymentCreateRequestDTO requestDTO = new PaymentCreateRequestDTO();
        ConfirmationDTO redirectConfirmation = ConfirmationDTO.builder()
                .type("redirect")
                .returnUrl(createRequest.getReturnUrl())
                .build();
        requestDTO.setAmount(getAmountDto(createRequest.getAmount()));
        requestDTO.setConfirmation(redirectConfirmation);
        requestDTO.setDescription(createRequest.getDescription());
        requestDTO.setMetaData(createRequest.getMetadataMap());
        return requestDTO;
    }

    public PaymentCreateRequestDTO getRecurrentPaymentRequestDto(RecurrentPaymentCreateRequest createRequest) {
        PaymentCreateRequestDTO requestDTO = new PaymentCreateRequestDTO();
        ConfirmationDTO redirectConfirmation = ConfirmationDTO.builder()
                .type("redirect")
                .returnUrl(createRequest.getReturnUrl())
                .build();
        requestDTO.setAmount(getAmountDto(createRequest.getAmount()));
        requestDTO.setConfirmation(redirectConfirmation);
        requestDTO.setDescription(createRequest.getDescription());
        requestDTO.setMetaData(createRequest.getMetadataMap());
        requestDTO.setPaymentMethodId(createRequest.getPaymentMethodId());
        requestDTO.setSavePaymentMethod(createRequest.getSavePaymentMethod());
        return requestDTO;
    }

    public PaymentRefundRequestDTO getPaymentRefundRequestDto(PaymentRefundRequest refundRequest) {
        PaymentRefundRequestDTO requestDTO = new PaymentRefundRequestDTO();
        requestDTO.setPaymentId(refundRequest.getPaymentId());
        requestDTO.setAmount(getAmountDto(refundRequest.getAmount()));
        requestDTO.setMetaData(refundRequest.getMetadataMap());
        return requestDTO;
    }

    public UserPayment getUserPayment(Payment payment) {
        Amount amount = Amount.newBuilder()
                .setCurrency(payment.getCurrency())
                .setValue(payment.getValue())
                .build();
        Instant instantCreated = payment.getCreated().toInstant(ZoneOffset.UTC);
        Instant instantUpdated = payment.getUpdated().toInstant(ZoneOffset.UTC);
        return UserPayment.newBuilder()
                .setAmount(amount)
                .setCreatedAt(Timestamp.newBuilder().setSeconds(instantCreated.getEpochSecond())
                        .setNanos(instantCreated.getNano()).build())
                .setUpdatedAt(Timestamp.newBuilder().setSeconds(instantUpdated.getEpochSecond())
                        .setNanos(instantUpdated.getNano()).build())
                .setPaymentId(payment.getPaymentId())
                .setStatus(getPaymentStatus(payment.getStatus()))
                .setUserId(payment.getUserId()).build();
    }

    public List<UserPayment> getUserPaymentResponse(List<Payment> payments) {
        return payments.stream()
                .map(this::getUserPayment)
                .collect(Collectors.toList());
    }

    public PaymentCreateResponse getPaymentResponse(PaymentResponseDTO paymentResponseDTO) {
        Instant instantCreated = paymentResponseDTO.getCreatedAt().toInstant(ZoneOffset.UTC);
        return PaymentCreateResponse.newBuilder()
                .setAmount(getAmount(paymentResponseDTO.getAmount()))
                .setDescription(paymentResponseDTO.getDescription())
                .setCreatedAt(Timestamp.newBuilder().setSeconds(instantCreated.getEpochSecond())
                        .setNanos(instantCreated.getNano()).build())
                .setPaymentId(paymentResponseDTO.getId())
                .setStatus(getPaymentStatus(paymentResponseDTO.getStatus()))
                .setConfirmationUrl(Optional.ofNullable(paymentResponseDTO.getConfirmation())
                        .map(ConfirmationDTO::getConfirmationUrl)
                        .orElseThrow(NoConfirmationUrlException::new))
                .build();
    }

    public PaymentRecurrentResponse getRecurrentPaymentResponse(PaymentResponseDTO paymentResponseDTO) {
        Instant instantCreated = paymentResponseDTO.getCreatedAt().toInstant(ZoneOffset.UTC);
        return PaymentRecurrentResponse.newBuilder()
                .setAmount(getAmount(paymentResponseDTO.getAmount()))
                .setDescription(paymentResponseDTO.getDescription())
                .setCreatedAt(Timestamp.newBuilder().setSeconds(instantCreated.getEpochSecond())
                        .setNanos(instantCreated.getNano()).build())
                .setPaymentId(paymentResponseDTO.getId())
                .setStatus(getPaymentStatus(paymentResponseDTO.getStatus()))
                .setConfirmationUrl(Optional.ofNullable(paymentResponseDTO.getConfirmation())
                        .map(ConfirmationDTO::getConfirmationUrl)
                        .orElseThrow(NoConfirmationUrlException::new))
                .build();
    }

    public PaymentRefundResponse getPaymentRefundResponse(PaymentRefundResponseDTO responseDTO) {
        String reason = Optional.ofNullable(responseDTO.getCancellationDetails())
                .map(CancellationDetailsDTO::getReason).orElse("");
        return PaymentRefundResponse.newBuilder()
                .setAmount(getAmount(responseDTO.getAmount()))
                .setCreatedAt(responseDTO.getCreatedAt().toString())
                .setPaymentId(responseDTO.getPaymentId())
                .setStatus(getPaymentStatus(responseDTO.getStatus()))
                .setDetails(reason)
                .setRefundId(responseDTO.getId())
                .putAllMetadata(Optional.ofNullable(responseDTO.getMetaData()).orElse(Collections.emptyMap()))
                .build();
    }

    private Amount getAmount(AmountDTO amountDto) {
        return Amount.newBuilder()
                .setCurrency(amountDto.getCurrency())
                .setValue(Double.valueOf(amountDto.getValue()).longValue())
                .build();
    }

    private AmountDTO getAmountDto(Amount amount) {
        return Optional.of(amount)
                .map(it -> {
                    AmountDTO amountDto = new AmountDTO();
                    amountDto.setCurrency(it.getCurrency());
                    amountDto.setValue(String.valueOf(amount.getValue()));
                    return amountDto;
                })
                .orElse(null);
    }

    public PaymentStatus getPaymentStatus(String status) {
        RequestPaymentStatus requestPaymentStatus = RequestPaymentStatus.getStatus(status);
        return switch (requestPaymentStatus) {
            case PENDING -> PaymentStatus.PENDING;
            case CANCELED -> PaymentStatus.CANCELED;
            case SUCCEEDED -> PaymentStatus.SUCCESS;
        };
    }
}

