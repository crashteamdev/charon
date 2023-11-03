package dev.crashteam.charon.model.dto.click;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ClickResponse {

    @JsonProperty("click_trans_id")
    private Long clickTransId;
    @JsonProperty("merchant_trans_id")
    private String merchantTransId;
    @JsonProperty("merchant_prepare_id")
    private Long merchantPrepareId;
    @JsonProperty("merchant_confirm_id")
    private String merchantConfirmId;
    private Long error;
    @JsonProperty("error_note")
    private String errorNote;
}
