package dev.crashteam.charon.job;

import com.posthog.java.PostHog;
import dev.crashteam.charon.model.Operation;
import dev.crashteam.charon.model.PaymentSystemType;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Constant;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.publisher.handler.StreamPublisherHandler;
import dev.crashteam.charon.service.PaymentJobService;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.charon.service.UserService;
import dev.crashteam.charon.resolver.PaymentResolver;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class PurchaseServiceJob implements Job {

    private final PaymentService paymentService;
    private final StreamPublisherHandler publisherHandler;
    private final UserService userService;
    private final List<PaymentResolver> resolvers;
    private final PostHog postHog;
    private final PaymentJobService paymentJobService;
    private int seconds;

    @Value("${app.payment-check.timeout}")
    private int paymentTimeoutMinutes;

    @Override
    @Transactional
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        String paymentId = String.valueOf(jobDetail.getJobDataMap().get("payment_id"));
        Payment payment = paymentService.findByPaymentId(paymentId);
        seconds = Integer.parseInt(jobDetail.getJobDataMap().get("seconds").toString());
        checkPaymentStatus(payment);
    }

    @Transactional
    public void checkPaymentStatus(Payment payment) {
        if (!RequestPaymentStatus.SUCCESS.equals(payment.getStatus())
                && (payment.getCreated() != null && LocalDateTime.now().isAfter(payment.getCreated().plusMinutes(paymentTimeoutMinutes)))) {
            log.info("Purchase service payment with id [{}] timed out to be processed for some reason", payment.getPaymentId());
            payment.setStatus(RequestPaymentStatus.CANCELED);
            paymentService.save(payment);
            publisherHandler.publishPaymentStatusChangeMessage(payment);
            return;
        }
        PaymentSystemType systemType = PaymentSystemType.getByTitle(payment.getPaymentSystem());
        if (systemType.isCallback()) {
            return;
        }
        PaymentResolver paymentResolver = resolvers.stream()
                .filter(it -> it.getPaymentSystem().equals(PaymentSystem.forNumber(systemType.getNumberValue())))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
        RequestPaymentStatus paymentStatus = paymentResolver.getPaymentStatus(payment.getExternalId());
        if (!RequestPaymentStatus.PENDING.equals(paymentStatus)
                && !RequestPaymentStatus.UNKNOWN.equals(paymentStatus)
                && !RequestPaymentStatus.NOT_ACCEPTABLE.equals(paymentStatus)) {
            if (RequestPaymentStatus.SUCCESS.equals(paymentStatus)) {
                log.info("Payment with id [{}] successful, purchasing service", payment.getPaymentId());
                User user = userService.getUser(payment.getUser().getId());
                payment.setStatus(RequestPaymentStatus.SUCCESS);
                if (payment.getPromoCode() != null) {
                    paymentService.savePromoCodeRestrictions(payment.getPromoCode(), payment.getUser());
                }
                if (payment.getOperationType() != null
                        && !payment.getOperationType().getType().equals(Operation.GENERIC_PURCHASE.getTitle())) {
                    if (user.getSubscriptionValidUntil() == null) {
                        user.setSubscriptionValidUntil(LocalDateTime.now().plusMonths(payment.getMonthPaid()));
                    } else {
                        if (user.getSubscriptionValidUntil().isBefore(LocalDateTime.now())) {
                            user.setSubscriptionValidUntil(LocalDateTime.now().plusMonths(payment.getMonthPaid()));
                        } else {
                            LocalDateTime plusMonths = user.getSubscriptionValidUntil().plusMonths(payment.getMonthPaid());
                            user.setSubscriptionValidUntil(plusMonths);
                        }
                    }
                }
                userService.saveUser(user);
                sendUserPurchaseAnalyticsEvent(payment.getUserId(), payment);
            } else if (RequestPaymentStatus.FAILED.equals(paymentStatus)) {
                log.info("Payment with id [{}] failed for some reason", payment.getPaymentId());
                payment.setStatus(RequestPaymentStatus.FAILED);
            } else if (RequestPaymentStatus.CANCELED.equals(paymentStatus)) {
                log.info("Payment with id [{}] canceled", payment.getPaymentId());
                payment.setStatus(RequestPaymentStatus.CANCELED);
            }
            paymentService.save(payment);
            publisherHandler.publishPaymentStatusChangeMessage(payment);
        } else {
            paymentJobService.schedulePaymentJob(payment.getPaymentId(), PurchaseServiceJob.class, seconds, Constant.PURCHASE_SERVICE_JOB_NAME);
        }
    }

    private void sendUserPurchaseAnalyticsEvent(String userId, Payment payment) {
        try {
            boolean isPromo = payment.getPromoCode() != null;
            Map<String, Object> properties = Map.of(
                    "amount", payment.getAmount().toString(),
                    "paid-service", payment.getPaidService() != null
                            ? payment.getPaidService().getName() : "Generic payment",
                    "promo", isPromo);
            postHog.capture(userId, "payment_complete", properties);
        } catch (Exception ex) {
            log.warn("Exception during send user analytics data", ex);
        }
    }
}
