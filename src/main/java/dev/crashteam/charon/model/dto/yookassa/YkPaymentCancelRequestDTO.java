package dev.crashteam.charon.model.dto.yookassa;

import lombok.Data;

import java.util.Map;

@Data
public class YkPaymentCancelRequestDTO {
    private YkAmountDTO amount;
    private String paymentId;
    private Map<String, String> metaData;
}
