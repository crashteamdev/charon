package dev.crashteam.charon.mapper.integration;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.yookassa.YkAmountDTO;
import dev.crashteam.charon.model.dto.yookassa.YkConfirmationDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentCreateRequestDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundRequestDTO;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentRefundRequest;
import dev.crashteam.payment.RecurrentPaymentCreateRequest;
import org.springframework.stereotype.Service;

@Service
public class YookassaPaymentMapper {

    public YkPaymentCreateRequestDTO getCreatePaymentRequestDto(PaymentCreateRequest createRequest, String amount) {
        YkPaymentCreateRequestDTO requestDTO = new YkPaymentCreateRequestDTO();

        YkConfirmationDTO redirectConfirmation = YkConfirmationDTO.builder()
                .type("redirect")
                .returnUrl(PaymentProtoUtils.getUrlFromRequest(createRequest))
                .build();
        requestDTO.setAmount(getAmountDto("RUB", amount));
        requestDTO.setConfirmation(redirectConfirmation);
        requestDTO.setDescription(PaymentProtoUtils.getDescriptionFromRequest(createRequest));
        requestDTO.setMetaData(createRequest.getMetadataMap());
        requestDTO.setCapture(true);
        return requestDTO;
    }

    @Deprecated
    public YkPaymentCreateRequestDTO getRecurrentPaymentRequestDto(RecurrentPaymentCreateRequest createRequest) {
        YkPaymentCreateRequestDTO requestDTO = new YkPaymentCreateRequestDTO();
        YkConfirmationDTO redirectConfirmation = YkConfirmationDTO.builder()
                .type("redirect")
                .returnUrl(createRequest.getReturnUrl())
                .build();
        //requestDTO.setAmount(getAmountDto(createRequest.getAmount()));
        requestDTO.setConfirmation(redirectConfirmation);
        requestDTO.setDescription(createRequest.getDescription());
        requestDTO.setMetaData(createRequest.getMetadataMap());
        requestDTO.setPaymentMethodId(createRequest.getPaymentMethodId());
        requestDTO.setSavePaymentMethod(createRequest.getSavePaymentMethod());
        return requestDTO;
    }

    @Deprecated
    public YkPaymentRefundRequestDTO getPaymentRefundRequestDto(PaymentRefundRequest refundRequest) {
        YkPaymentRefundRequestDTO requestDTO = new YkPaymentRefundRequestDTO();
        requestDTO.setPaymentId(refundRequest.getPaymentId());
        //requestDTO.setAmount(getAmountDto(refundRequest.getAmount()));
        requestDTO.setMetaData(refundRequest.getMetadataMap());
        return requestDTO;
    }

    private YkAmountDTO getAmountDto(String currency, String value) {
        YkAmountDTO ykAmountDto = new YkAmountDTO();
        ykAmountDto.setCurrency(currency);
        ykAmountDto.setValue(value);
        return ykAmountDto;
    }

    public RequestPaymentStatus getPaymentStatus(String status) {
        return switch (status) {
            case "waiting_for_capture", "pending" -> RequestPaymentStatus.PENDING;
            case "succeeded" -> RequestPaymentStatus.SUCCESS;
            case "canceled" -> RequestPaymentStatus.CANCELED;
            default -> throw new IllegalArgumentException("No such status - %s".formatted(status));
        };
    }

}

