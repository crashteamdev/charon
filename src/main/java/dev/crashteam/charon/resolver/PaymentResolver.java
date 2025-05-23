package dev.crashteam.charon.resolver;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.dto.UserSavedPaymentResolverDto;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.payment.PaymentCreateRequest;
import dev.crashteam.payment.PaymentSystem;

public interface PaymentResolver {

    PaymentSystem getPaymentSystem();

    /***
     *
     * @param request grpc request
     * @param amount major value of initial amount with dot, for example - 40.00
     * @return PaymentData - object will be mapped to Payment entity
     */
    PaymentData createPayment(PaymentCreateRequest request, String amount);

    /***
     * paymentId provider payment id
     * savedPaymentDto provider additionalId (nullable)
     * amount major value of initial amount with dot, for example - 40.00
     * PaymentData - object will be mapped to Payment entity
     */
    PaymentData recurrentPayment(UserSavedPaymentResolverDto savedPaymentDto);

    /***
     * Get status from integration service
     * @param paymentId set null if payment system on callbacks
     * @return RequestPaymentStatus.NOT_ACCEPTABLE if payment system on callbacks
     */
    RequestPaymentStatus getPaymentStatus(String paymentId);
}
