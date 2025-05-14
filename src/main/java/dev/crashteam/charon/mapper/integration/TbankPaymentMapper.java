package dev.crashteam.charon.mapper.integration;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.tbank.ChargeRequestDTO;
import dev.crashteam.charon.model.dto.tbank.InitRequestDTO;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import org.springframework.stereotype.Component;

@Component
public class TbankPaymentMapper {

    public InitRequestDTO getPaymentRequestDTO(PaymentCreateRequest createRequest,
                                               String terminalKey,
                                               String token,
                                               String paymentId,
                                               Long amount) {
        InitRequestDTO.InitRequestDTOBuilder dtoBuilder = InitRequestDTO.builder()
                .amount(amount)
                .terminalKey(terminalKey)
                .token(token)
                .description(PaymentProtoUtils.getDescriptionFromRequest(createRequest))
                .orderId(paymentId)
                .customerKey(PaymentProtoUtils.getUserIdFromRequest(createRequest));
        if (createRequest.hasPaymentPurchaseService() && createRequest.getPaymentPurchaseService().getSavePaymentMethod()) {
            dtoBuilder.recurrent("Y");
        }
        return dtoBuilder.build();
    }

    public InitRequestDTO getPaymentRecurrentRequestDTO(String terminalKey,
                                                        String token,
                                                        String paymentId,
                                                        Long amount) {
        InitRequestDTO.InitRequestDTOBuilder dtoBuilder = InitRequestDTO.builder()
                .amount(amount)
                .terminalKey(terminalKey)
                .token(token)
                .description("Recurrent payment")
                .orderId(paymentId);
        return dtoBuilder.build();
    }

    public ChargeRequestDTO getChargeRequestDTO(String terminalKey,
                                                String token,
                                                String rebillId,
                                                String externalPaymentId) {
        return ChargeRequestDTO.builder()
                .rebillId(rebillId)
                .paymentId(externalPaymentId)
                .terminalKey(terminalKey)
                .token(token)
                .build();
    }

    public RequestPaymentStatus getPaymentStatus(String status) {
        return switch (status) {
            case "NEW", "FORM_SHOWED", "AUTHORIZING", "3DS_CHECKING", "3DS_CHECKED", "AUTHORIZED", "CONFIRMING"
                    -> RequestPaymentStatus.PENDING;
            case "CONFIRMED" -> RequestPaymentStatus.SUCCESS;
            case "CANCELED", "DEADLINE_EXPIRED", "REJECTED" -> RequestPaymentStatus.CANCELED;
            case "AUTH_FAIL" -> RequestPaymentStatus.FAILED;
            default -> throw new IllegalArgumentException("No such status - %s".formatted(status));
        };
    }
}
