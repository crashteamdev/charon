package dev.crashteam.charon.resolver;

import dev.crashteam.charon.component.ClickProperties;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.currency.ExchangeDto;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.service.CurrencyService;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClickService implements PaymentResolver {

    private final CurrencyService currencyService;
    private final ClickProperties clickProperties;

    @Override
    public PaymentSystem getPaymentSystem() {
        return PaymentSystem.PAYMENT_SYSTEM_CLICK;
    }

    @Override
    public PaymentData createPayment(PaymentCreateRequest request, String amount) {
        log.info("Processing click payment");
        StringBuilder sb = new StringBuilder();

        String paymentId = UUID.randomUUID().toString();

        BigDecimal exchangeRate = currencyService.getExchangeRate("UZS");
        BigDecimal convertedAmount = (BigDecimal.valueOf(Double.parseDouble(amount))
                .multiply(exchangeRate.setScale(2, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        String email = PaymentProtoUtils.getEmailFromRequest(request);
        String phone = PaymentProtoUtils.getPhoneFromRequest(request);

        String url = sb.append(clickProperties.getBaseUrl())
                .append("?service_id=").append(clickProperties.getServiceId())
                .append("&merchant_id=").append(clickProperties.getMerchantId())
                .append("&amount=").append(convertedAmount)
                .append("&transaction_param=").append(paymentId)
                .append("&merchant_user_id=").append(clickProperties.getMerchantUserId())
                .toString();

        BigDecimal moneyAmount = PaymentProtoUtils.getMinorMoneyAmount(String.valueOf(convertedAmount));

        PaymentData paymentData = new PaymentData();
        paymentData.setPaymentId(paymentId);
        paymentData.setCreatedAt(LocalDateTime.now());
        paymentData.setProviderCurrency("UZS");
        paymentData.setProviderAmount(String.valueOf(moneyAmount));
        paymentData.setStatus(RequestPaymentStatus.PENDING);
        paymentData.setDescription("");
        paymentData.setEmail(email);
        paymentData.setPhone(phone);
        paymentData.setProviderId("");
        paymentData.setConfirmationUrl(url);
        paymentData.setExchangeRate(exchangeRate);
        return paymentData;
    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {
        return RequestPaymentStatus.NOT_ACCEPTABLE;
    }
}
