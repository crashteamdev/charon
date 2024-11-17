package dev.crashteam.charon.model.dto.yookassa;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class YkReceipt {
    private Customer customer;
    private List<ReceiptItem> items;

    @Data
    private static class Customer {
        private String email;
    }

    @Data
    private static class ReceiptItem {
        private String description;
        private YkAmountDTO amount;
        @JsonProperty("vat_code")
        private Integer vatCode;
        private Integer quantity;
    }
}
