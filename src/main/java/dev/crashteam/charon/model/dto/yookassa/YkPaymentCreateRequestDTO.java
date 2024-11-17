package dev.crashteam.charon.model.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class YkPaymentCreateRequestDTO {

    private YkAmountDTO amount;
    private YkConfirmationDTO confirmation;
    private String description;
    private YkReceipt receipt;
    private boolean savePaymentMethod;
    private String paymentMethodId;
    private Map<String, String> metaData;
}
