package dev.crashteam.charon.model.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentCancelResponseDTO {
    private String id;
    private String status;
    private AmountDTO amount;
    private String paymentId;
    private Map<String, String> metaData;
}
