package dev.crashteam.charon.model.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentRefundResponseDTO {
    private String id;
    private String status;
    private AmountDTO amount;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonProperty(value = "created_at")
    private LocalDateTime createdAt;
    @JsonProperty(value = "payment_id")
    private String paymentId;
    @JsonProperty(value = "cancellation_details")
    private CancellationDetailsDTO cancellationDetails;
    private Map<String, String> metaData;
}
