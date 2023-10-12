package dev.crashteam.charon.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentData {

    private String id;
    private String status;
    private String value;
    private String currency;
    private LocalDateTime createdAt;
    private String description;
    private String confirmationUrl;
}
