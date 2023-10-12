package dev.crashteam.charon.model.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YkCancellationDetailsDTO {

    private String party;
    private String reason;
}
