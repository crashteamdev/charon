package dev.crashteam.charon.resolver;

import dev.crashteam.charon.component.FreeKassaProperties;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.service.CurrencyService;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreeKassaService implements PaymentResolver {

    private final CurrencyService currencyService;

    private final FreeKassaProperties freeKassaProperties;

    private final static String FREEKASSA_BASE_URL = "https://pay.freekassa.com";

    @Override
    public PaymentSystem getPaymentSystem() {
        return PaymentSystem.PAYMENT_SYSTEM_FREEKASSA;
    }

    @Override
    public PaymentData createPayment(PaymentCreateRequest request, String amount) {
        log.info("Processing freekassa payment");
        StringBuilder sb = new StringBuilder();
        String paymentId = UUID.randomUUID().toString();
        //BigDecimal exchangeRate = currencyService.getExchangeRate("RUB");
        BigDecimal convertedAmount = BigDecimal.valueOf(Double.parseDouble(amount));
        String email = PaymentProtoUtils.getEmailFromRequest(request);
        String phone = PaymentProtoUtils.getPhoneFromRequest(request);
        String sign = DigestUtils.md5Hex("%s:%s:%s:%s:%s".formatted(freeKassaProperties.getShopId(), convertedAmount,
                freeKassaProperties.getSecretWordFirst(), "RUB", paymentId));

        sb.append(FREEKASSA_BASE_URL).append("/?m=").append(freeKassaProperties.getShopId())
                .append("&oa=").append(convertedAmount)
                .append("&currency=RUB")
                .append("&o=").append(paymentId)
                .append("&pay=PAY")
                .append("&s=").append(sign)
                .append("&us_paymentid=").append(paymentId);
        if (StringUtils.hasText(email)) {
            sb.append("&em=").append(email);
        }

        BigDecimal moneyAmount = PaymentProtoUtils.getMinorMoneyAmount(String.valueOf(convertedAmount));
        PaymentData paymentData = new PaymentData();
        paymentData.setPaymentId(paymentId);
        paymentData.setCreatedAt(LocalDateTime.now());
        paymentData.setProviderId("");
        paymentData.setStatus(RequestPaymentStatus.PENDING);
        paymentData.setProviderCurrency("RUB");
        paymentData.setDescription("");
        paymentData.setPhone(phone);
        paymentData.setEmail(email);
        paymentData.setProviderAmount(String.valueOf(moneyAmount));
        paymentData.setConfirmationUrl(sb.toString());
        //paymentData.setExchangeRate(exchangeRate);
        return paymentData;
    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {
        return RequestPaymentStatus.NOT_ACCEPTABLE;
    }


}
