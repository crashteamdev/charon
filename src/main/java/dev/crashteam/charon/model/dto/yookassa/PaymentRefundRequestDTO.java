package dev.crashteam.charon.model.dto.yookassa;

import lombok.Data;

import java.util.Map;

@Data
public class PaymentRefundRequestDTO {

    private String paymentId;
    private AmountDTO amount;
    private Map<String, String> metaData;
}
