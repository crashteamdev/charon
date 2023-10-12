package dev.crashteam.charon.mapper;

import com.google.protobuf.Timestamp;
import dev.crashteam.charon.exception.NoConfirmationUrlException;
import dev.crashteam.charon.model.PaymentData;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.dto.yookassa.YkCancellationDetailsDTO;
import dev.crashteam.charon.model.dto.yookassa.YkConfirmationDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundResponseDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentResponseDTO;
import dev.crashteam.payment.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.crashteam.charon.util.PaymentProtoUtils.getPaymentStatus;

@Service
public class ProtoMapper {

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

    public PaymentStatus getPaymentStatus(String status) {
        RequestPaymentStatus requestPaymentStatus = RequestPaymentStatus.getStatus(status);
        return switch (requestPaymentStatus) {
            case FAILED -> PaymentStatus.PAYMENT_STATUS_FAILED;
            case UNKNOWN -> PaymentStatus.PAYMENT_STATUS_UNKNOWN;
            case PENDING -> PaymentStatus.PAYMENT_STATUS_PENDING;
            case CANCELED -> PaymentStatus.PAYMENT_STATUS_CANCELED;
            case SUCCESS -> PaymentStatus.PAYMENT_STATUS_SUCCESS;
        };
    }

    public PaymentCreateResponse getPaymentResponse(PaymentData response) {
        Instant instantCreated = response.getCreatedAt().toInstant(ZoneOffset.UTC);
        return PaymentCreateResponse.newBuilder()
                .setAmount(getAmount(response.getCurrency(),
                        Long.valueOf(response.getValue())))
                .setDescription(response.getDescription())
                .setCreatedAt(Timestamp.newBuilder().setSeconds(instantCreated.getEpochSecond())
                        .setNanos(instantCreated.getNano()).build())
                .setPaymentId(response.getId())
                .setStatus(getPaymentStatus(response.getStatus()))
                .setConfirmationUrl(response.getConfirmationUrl())
                .build();
    }

    @Deprecated
    public PaymentRecurrentResponse getRecurrentPaymentResponse(Object response) {
        YkPaymentResponseDTO responseDTO = (YkPaymentResponseDTO) response;
        Instant instantCreated = responseDTO.getCreatedAt().toInstant(ZoneOffset.UTC);
        return PaymentRecurrentResponse.newBuilder()
                .setAmount(getAmount(responseDTO.getAmount().getCurrency(),
                        Long.valueOf(responseDTO.getAmount().getValue())))
                .setDescription(responseDTO.getDescription())
                .setCreatedAt(Timestamp.newBuilder().setSeconds(instantCreated.getEpochSecond())
                        .setNanos(instantCreated.getNano()).build())
                .setPaymentId(responseDTO.getId())
                .setStatus(getPaymentStatus(responseDTO.getStatus()))
                .setConfirmationUrl(Optional.ofNullable(responseDTO.getConfirmation())
                        .map(YkConfirmationDTO::getConfirmationUrl)
                        .orElseThrow(NoConfirmationUrlException::new))
                .build();
    }

    @Deprecated
    private PaymentRefundResponse getPaymentRefundResponse(Object response) {
        YkPaymentRefundResponseDTO responseDTO = (YkPaymentRefundResponseDTO) response;
        String reason = Optional.ofNullable(responseDTO.getCancellationDetails())
                .map(YkCancellationDetailsDTO::getReason).orElse("");
        return PaymentRefundResponse.newBuilder()
                .setAmount(getAmount(responseDTO.getAmount().getCurrency(),
                        Long.valueOf(responseDTO.getAmount().getValue())))
                .setCreatedAt(responseDTO.getCreatedAt().toString())
                .setPaymentId(responseDTO.getPaymentId())
                .setStatus(getPaymentStatus(responseDTO.getStatus()))
                .setDetails(reason)
                .setRefundId(responseDTO.getId())
                .putAllMetadata(Optional.ofNullable(responseDTO.getMetaData()).orElse(Collections.emptyMap()))
                .build();
    }

    private Amount getAmount(String currency, Long value) {
        return Amount.newBuilder()
                .setCurrency(currency)
                .setValue(value)
                .build();
    }

    public List<UserPayment> getUserPaymentResponse(List<Payment> payments) {
        return payments.stream()
                .map(this::getUserPayment)
                .collect(Collectors.toList());
    }
}
