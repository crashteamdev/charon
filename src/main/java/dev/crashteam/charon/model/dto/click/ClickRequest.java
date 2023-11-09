package dev.crashteam.charon.model.dto.click;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
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
    private String signTime;
    @JsonProperty("sign_string")
    private String signString;
}