package dev.crashteam.charon.mapper;

import com.google.protobuf.Timestamp;
import dev.crashteam.charon.exception.NoConfirmationUrlException;
import dev.crashteam.charon.model.domain.PromoCode;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.model.PaymentSystemType;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.model.dto.yookassa.YkCancellationDetailsDTO;
import dev.crashteam.charon.model.dto.yookassa.YkConfirmationDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundResponseDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentResponseDTO;
import dev.crashteam.payment.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProtoMapper {

    public GetBalanceResponse getBalanceResponse(User user) {
        return GetBalanceResponse.newBuilder()
                .setAmount(BalanceAmount.newBuilder().setValue(user.getBalance()).build())
                .setUserId(user.getId())
                .build();
    }

    public UserPayment getUserPayment(Payment payment) {
        Amount amount = Amount.newBuilder()
                .setCurrency(payment.getCurrency())
                .setValue(payment.getAmount())
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

    public CreatePromoCodeResponse getPromoCodeResponse(PromoCode promoCode) {
        PromoCodeContext.Builder promocodeContext = PromoCodeContext.newBuilder()
                .setDiscountPromocodeContext(DiscountPromoCodeContext.newBuilder()
                        .setDiscountPercentage(promoCode.getDiscountPercentage())
                        .build());
        Instant instant = promoCode.getValidUntil().toInstant(ZoneOffset.UTC);
        Timestamp validUntil = Timestamp.newBuilder().setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano()).build();
        var grpcPromoCode = dev.crashteam.payment.PromoCode.newBuilder()
                .setCode(promoCode.getCode())
                .setDescription(promoCode.getDescription())
                .setUsageLimit(Math.toIntExact(promoCode.getUsageLimit()))
                .setValidUntil(validUntil)
                .setPromoCodeContext(promocodeContext)
                .build();
        return CreatePromoCodeResponse.newBuilder().setPromoCode(grpcPromoCode).build();
    }

    public CheckPromoCodeResponse getCheckPromoCodeResponse(PromoCode promoCode) {
        if (promoCode == null) {
            return getErrorPromoCodeResponse(CheckPromoCodeResponse.ErrorResponse.ErrorCode.ERROR_CODE_NOT_FOUND);
        }
        if (LocalDateTime.now().isAfter(promoCode.getValidUntil())) {
            return getErrorPromoCodeResponse(CheckPromoCodeResponse.ErrorResponse.ErrorCode.ERROR_CODE_EXPIRED_PROMO_CODE);
        }
        if (promoCode.getUsageLimit() <= 0) {
            return getErrorPromoCodeResponse(CheckPromoCodeResponse.ErrorResponse.ErrorCode.ERROR_CODE_MAX_USAGE_LIMIT);
        }
        return CheckPromoCodeResponse.newBuilder()
                .setSuccessResponse(CheckPromoCodeResponse.SuccessResponse.newBuilder()
                        .setPromoCode(promoCode.getCode())
                        .build())
                .build();
    }

    public PaymentSystemType getPaymentSystemType(PaymentSystem paymentSystem) {
        return switch (paymentSystem) {
            case PAYMENT_SYSTEM_UNKNOWN -> PaymentSystemType.PAYMENT_SYSTEM_UNKNOWN;
            case PAYMENT_SYSTEM_YOOKASSA -> PaymentSystemType.PAYMENT_SYSTEM_YOOKASSA;
            case PAYMENT_SYSTEM_FREEKASSA -> PaymentSystemType.PAYMENT_SYSTEM_FREEKASSA;
            case PAYMENT_SYSTEM_UZUM_BANK -> PaymentSystemType.PAYMENT_SYSTEM_UZUM_BANK;
            case UNRECOGNIZED -> PaymentSystemType.UNRECOGNIZED;
        };
    }

    public PaymentStatus getPaymentStatus(String status) {
        RequestPaymentStatus requestPaymentStatus = RequestPaymentStatus.getStatus(status);
        return getPaymentStatus(requestPaymentStatus);
    }

    public PaymentStatus getPaymentStatus(RequestPaymentStatus requestPaymentStatus) {
        return switch (requestPaymentStatus) {
            case FAILED -> PaymentStatus.PAYMENT_STATUS_FAILED;
            case UNKNOWN -> PaymentStatus.PAYMENT_STATUS_UNKNOWN;
            case PENDING -> PaymentStatus.PAYMENT_STATUS_PENDING;
            case CANCELED -> PaymentStatus.PAYMENT_STATUS_CANCELED;
            case SUCCESS -> PaymentStatus.PAYMENT_STATUS_SUCCESS;
            default -> throw new IllegalStateException("Unexpected value: " + requestPaymentStatus);
        };
    }

    public PaymentCreateResponse getPaymentResponse(PaymentData response, Payment payment) {
        return getPaymentResponse(response, payment, payment.getAmount());
    }

    public PaymentCreateResponse getPaymentResponse(PaymentData response, Payment payment, long amount) {
        Instant instantCreated = response.getCreatedAt().toInstant(ZoneOffset.UTC);
        return PaymentCreateResponse.newBuilder()
                .setAmount(getAmount("USD", amount))
                .setDescription(response.getDescription())
                .setCreatedAt(Timestamp.newBuilder().setSeconds(instantCreated.getEpochSecond())
                        .setNanos(instantCreated.getNano()).build())
                .setPaymentId(payment.getPaymentId())
                .setStatus(getPaymentStatus(payment.getStatus()))
                .setConfirmationUrl(response.getConfirmationUrl())
                .build();
    }

    public PurchaseServiceResponse getPurchaseServiceResponse(Payment payment, Long amount) {
        BalanceAmount balanceAmount = BalanceAmount.newBuilder().setValue(amount).build();
        if (payment.getStatus().equals(RequestPaymentStatus.FAILED.getTitle())) {
            PurchaseServiceResponse.ErrorResponse errorResponse = PurchaseServiceResponse.ErrorResponse.newBuilder()
                    .setErrorCode(PurchaseServiceResponse.ErrorResponse.ErrorCode.ERROR_CODE_INSUFFICIENT_FUNDS)
                    .setDescription("Not enough money")
                    .setUserBalanceAmount(balanceAmount).build();
            return PurchaseServiceResponse.newBuilder().setErrorResponse(errorResponse).build();
        } else {
            PurchaseServiceResponse.SuccessResponse successResponse = PurchaseServiceResponse.SuccessResponse.newBuilder()
                    .setUserBalanceAmount(balanceAmount)
                    .setOperationId(payment.getOperationId())
                    .build();
            return PurchaseServiceResponse.newBuilder()
                    .setSuccessResponse(successResponse)
                    .build();
        }
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

    private CheckPromoCodeResponse getErrorPromoCodeResponse(CheckPromoCodeResponse.ErrorResponse.ErrorCode errorCode) {
        CheckPromoCodeResponse.ErrorResponse errorResponse = CheckPromoCodeResponse.ErrorResponse.newBuilder()
                .setErrorCode(errorCode)
                .build();
        return CheckPromoCodeResponse.newBuilder()
                .setErrorResponse(errorResponse)
                .build();
    }
}
