package dev.crashteam.charon.model.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YkPaymentMethodDTO {
    private String id;
    private String type;
    private Boolean saved;
}
