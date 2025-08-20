package dev.crashteam.charon.job;

import dev.crashteam.charon.model.PaymentSystemType;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Constant;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.publisher.handler.StreamPublisherHandler;
import dev.crashteam.charon.resolver.PaymentResolver;
import dev.crashteam.charon.service.PaymentJobService;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.charon.service.UserService;
import dev.crashteam.payment.PaymentSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class BalancePaymentJob implements Job {

    private final PaymentService paymentService;
    private final StreamPublisherHandler publisherHandler;
    private final UserService userService;
    private final List<PaymentResolver> resolvers;
    private final PaymentJobService paymentJobService;
    private int seconds;

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
        if (payment.getCreated() != null && LocalDateTime.now().plusMinutes(10).isBefore(payment.getCreated())) {
            log.info("Balance payment with id [{}] timed out to be processed for some reason", payment.getPaymentId());
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
        if (RequestPaymentStatus.SUCCESS.equals(paymentStatus)) {
            log.info("Payment with id [{}] successful, processing balance deposit", payment.getPaymentId());
            User user = userService.getUser(payment.getUser().getId());
            if (user == null) {
                log.warn("User with id {} not found", payment.getUser().getId());
                return;
            }
            long newBalance = user.getBalance() + payment.getAmount();
            user.setBalance(newBalance);

            userService.saveUser(user);

            payment.setStatus(RequestPaymentStatus.SUCCESS);
            paymentService.save(payment);
            publisherHandler.publishPaymentStatusChangeMessage(payment);
        } else if (RequestPaymentStatus.FAILED.equals(paymentStatus)) {
            log.info("Payment with id [{}] failed for some reason", payment.getPaymentId());
            payment.setStatus(RequestPaymentStatus.FAILED);
            paymentService.save(payment);
            publisherHandler.publishPaymentStatusChangeMessage(payment);
        } else if (RequestPaymentStatus.CANCELED.equals(paymentStatus)) {
            log.info("Payment with id [{}] canceled", payment.getPaymentId());
            payment.setStatus(RequestPaymentStatus.CANCELED);
            paymentService.save(payment);
            publisherHandler.publishPaymentStatusChangeMessage(payment);
        } else {
            paymentJobService.schedulePaymentJob(payment.getPaymentId(), BalancePaymentJob.class, seconds, Constant.BALANCE_PAYMENT_JOB_NAME);
        }
    }
}
