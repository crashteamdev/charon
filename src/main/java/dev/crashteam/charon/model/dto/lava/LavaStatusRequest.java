package dev.crashteam.charon.model.dto.lava;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class LavaStatusRequest {

    private String orderId;
    private String shopId;
    private String invoiceId;

}
