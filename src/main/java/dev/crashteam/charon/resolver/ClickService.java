package dev.crashteam.charon.resolver;

import dev.crashteam.charon.component.ClickProperties;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.service.CurrencyService;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
        StringBuilder sb = new StringBuilder();

        String paymentId = UUID.randomUUID().toString();
        String convertedAmount = currencyService.getConvertedAmount("USD", "UZS", amount);
        String email = PaymentProtoUtils.getEmailFromRequest(request);
        String phone = PaymentProtoUtils.getPhoneFromRequest(request);

        String url = sb.append(clickProperties.getBaseUrl())
                .append("?service_id=").append(clickProperties.getServiceId())
                .append("&merchant_id=").append(clickProperties.getMerchantId())
                .append("&amount=").append(convertedAmount)
                .append("&transaction_param=").append(paymentId).toString();

        BigDecimal moneyAmount = PaymentProtoUtils.getMinorMoneyAmount(convertedAmount);

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
        return paymentData;
    }

    @Override
    public RequestPaymentStatus getPaymentStatus(String paymentId) {
        return RequestPaymentStatus.NOT_ACCEPTABLE;
    }
}
