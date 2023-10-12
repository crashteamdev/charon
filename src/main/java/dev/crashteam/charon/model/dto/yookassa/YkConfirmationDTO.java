package dev.crashteam.charon.model.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YkConfirmationDTO {

    private String type;
    @JsonProperty("return_url")
    private String returnUrl;
    @JsonProperty("confirmation_url")
    private String confirmationUrl;
}
