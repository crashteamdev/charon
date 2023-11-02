package dev.crashteam.charon.model.dto.click;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClickRequest {

    @JsonProperty("click_trans_id")
    private Long clickTransId;
    @JsonProperty("service_id")
    private Long serviceId;
    @JsonProperty("click_paydoc_id")
    private Long clickPaydocId;
    @JsonProperty("merchant_trans_id")
    private String merchantTransId;
    @JsonProperty("merchant_prepare_id")
    private Long merchantPrepareId;
    private BigDecimal amount;
    private Long action;
    private Long error;
    @JsonProperty("error_note")
    private String errorNote;
    @JsonProperty("sign_time")
    @JsonFormat(pattern = "YYYY-MM-DD HH:mm:ss")
    private LocalDateTime signTime;
    @JsonProperty("sign_string")
    private String signString;
}