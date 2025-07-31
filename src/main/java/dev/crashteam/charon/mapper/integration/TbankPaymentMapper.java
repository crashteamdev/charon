package dev.crashteam.charon.mapper.integration;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.tbank.ChargeRequestDTO;
import dev.crashteam.charon.model.dto.tbank.InitRequestDTO;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.charon.util.TbankTokenGenerator;
import dev.crashteam.payment.PaymentCreateRequest;
import org.springframework.stereotype.Component;

@Component
public class TbankPaymentMapper {

    public InitRequestDTO getPaymentRequestDTO(PaymentCreateRequest createRequest,
                                               String terminalKey,
                                               String secretKey,
                                               String paymentId,
                                               Long amount) {
        String description = PaymentProtoUtils.getDescriptionFromRequest(createRequest);
        String customerKey = PaymentProtoUtils.getUserIdFromRequest(createRequest);
        String recurrent = null;
        
        if (createRequest.hasPaymentPurchaseService() && createRequest.getPaymentPurchaseService().getSavePaymentMethod()) {
            recurrent = "Y";
        }
        
        String token = TbankTokenGenerator.generateInitToken(
            amount, customerKey, description, paymentId, recurrent, terminalKey, secretKey
        );

        InitRequestDTO.InitRequestDTOBuilder dtoBuilder = InitRequestDTO.builder()
                .amount(amount)
                .terminalKey(terminalKey)
                .token(token)
                .description(description)
                .orderId(paymentId)
                .customerKey(customerKey);
                
        if (recurrent != null) {
            dtoBuilder.recurrent(recurrent);
        }
        
        return dtoBuilder.build();
    }

    public InitRequestDTO getPaymentRecurrentRequestDTO(String terminalKey,
                                                        String secretKey,
                                                        String paymentId,
                                                        Long amount) {
        String description = "Recurrent payment";
        
        String token = TbankTokenGenerator.generateInitToken(
            amount, null, description, paymentId, null, terminalKey, secretKey
        );

        return InitRequestDTO.builder()
                .amount(amount)
                .terminalKey(terminalKey)
                .token(token)
                .description(description)
                .orderId(paymentId)
                .build();
    }

    public ChargeRequestDTO getChargeRequestDTO(String terminalKey,
                                                String secretKey,
                                                String rebillId,
                                                String externalPaymentId) {
        String token = TbankTokenGenerator.generateChargeToken(
            externalPaymentId, rebillId, terminalKey, secretKey
        );

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
