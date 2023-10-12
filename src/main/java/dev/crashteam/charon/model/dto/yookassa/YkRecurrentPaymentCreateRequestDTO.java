package dev.crashteam.charon.model.dto.yookassa;

import lombok.Data;

import java.util.Map;

@Data
public class YkRecurrentPaymentCreateRequestDTO {

    private YkAmountDTO amount;
    private YkConfirmationDTO confirmation;
    private String description;
    private boolean savePaymentMethod;
    private String paymentMethodId;
    private Map<String, String> metaData;
}
