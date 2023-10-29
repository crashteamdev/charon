package dev.crashteam.charon.model.dto.yookassa;

import lombok.Data;

import java.util.Map;

@Data
public class YkPaymentRefundRequestDTO {

    private String paymentId;
    private YkAmountDTO amount;
    private Map<String, String> metaData;
}
