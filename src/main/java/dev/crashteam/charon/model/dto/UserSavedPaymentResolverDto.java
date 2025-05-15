package dev.crashteam.charon.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSavedPaymentResolverDto {

    private String paymentId;
    private String amount;
    private String userId;
    private Long monthPaid;
}
