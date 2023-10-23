package dev.crashteam.charon.model.dto.resolver;

import dev.crashteam.charon.model.RequestPaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentData {

    private String id;
    private RequestPaymentStatus status;
    private String amount;
    private String currency;
    private LocalDateTime createdAt;
    private String description;
    private String confirmationUrl;
}
