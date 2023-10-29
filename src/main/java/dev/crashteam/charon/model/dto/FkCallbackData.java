package dev.crashteam.charon.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FkCallbackData {
    private String merchantId;
    private String amount;
    private String orderId;
    private String paymentId;
    private String curId;
}
