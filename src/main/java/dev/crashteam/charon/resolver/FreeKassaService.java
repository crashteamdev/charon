package dev.crashteam.charon.resolver;

import dev.crashteam.charon.component.FreeKassaProperties;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.service.CurrencyService;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FreeKassaService implements PaymentResolver {

    private final CurrencyService currencyService;

    private final FreeKassaProperties freeKassaProperties;

    private final static String FREEKASSA_BASE_URL = "https://pay.freekassa.ru";

    @Override
    public PaymentSystem getPaymentSystem() {
        return PaymentSystem.PAYMENT_SYSTEM_FREEKASSA;
    }

    @Override
    public PaymentData createPayment(PaymentCreateRequest request, String amount) {
        StringBuilder sb = new StringBuilder();
        String paymentId = UUID.randomUUID().toString();
        String convertedAmount = currencyService.getConvertedAmount("USD", "RUB", amount);
        String email = PaymentProtoUtils.getEmailFromRequest(request);
        String sign;
        if (StringUtils.hasText(email)) {
            sign = DigestUtils.md5Hex("%s:%s:%s:%s:%s".formatted(freeKassaProperties.getShopId(),
                    convertedAmount, freeKassaProperties.getSecretWordFirst(), "RUB", email));
        } else {
            sign = DigestUtils.md5Hex("%s:%s:%s:%s".formatted(freeKassaProperties.getShopId(), convertedAmount,
                    freeKassaProperties.getSecretWordFirst(), "RUB"));
        }

        sb.append(FREEKASSA_BASE_URL).append("/?m=").append(freeKassaProperties.getShopId())
                .append("&oa=").append(convertedAmount)
                .append("&currency=RUB")
                .append("&o=").append(email)
                .append("&pay=PAY")
                .append("&s=").append(sign)
                .append("&em=").append(email)
                .append("&us_paymentid=").append(paymentId);

        BigDecimal moneyAmount = PaymentProtoUtils.getMinorMoneyAmount(convertedAmount);
        PaymentData paymentData = new PaymentData();
        paymentData.setPaymentId(paymentId);
        paymentData.setCreatedAt(LocalDateTime.now());
        paymentData.setProviderId("");
        paymentData.setStatus(RequestPaymentStatus.PENDING);
        paymentData.setProviderCurrency("RUB");
        paymentData.setDescription("");
        paymentData.setProviderAmount(String.valueOf(moneyAmount));
        paymentData.setConfirmationUrl(sb.toString());
        return paymentData;
    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {
        return RequestPaymentStatus.NOT_ACCEPTABLE;
    }


}
