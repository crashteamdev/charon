package dev.crashteam.charon.model.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YkPaymentResponseDTO {
    private String id;
    private String status;
    private String paid;
    private YkAmountDTO amount;
    @JsonProperty("captured_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime capturedAt;
    @JsonProperty("created_at")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAt;
    private String description;
    @JsonProperty(value = "payment_method")
    private YkPaymentMethodDTO paymentMethod;
    private boolean refundable;
    private YkConfirmationDTO confirmation;
    private Map<String, String> metaData;

}
