package dev.crashteam.charon.model.dto.lava;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class LavaRequest {

    private BigDecimal sum;
    private String orderId;
    private String shopId;
    private String failUrl;
    private String successUrl;
    private Integer expire;
    private String customFields;
}
