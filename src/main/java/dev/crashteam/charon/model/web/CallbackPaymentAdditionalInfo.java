package dev.crashteam.charon.model.web;

import dev.crashteam.charon.model.PromoCodeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallbackPaymentAdditionalInfo {

    private String promoCode;
    private PromoCodeType promoCodeType;
}
