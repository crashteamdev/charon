package dev.crashteam.charon.model.dto.resolver;

import dev.crashteam.charon.model.RequestPaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentData {

    private String paymentId;
    private String providerId;
    private RequestPaymentStatus status;
    private String providerAmount;
    private String providerCurrency;
    private LocalDateTime createdAt;
    private String description;
    private String confirmationUrl;
    private String email;
    private String phone;
}
