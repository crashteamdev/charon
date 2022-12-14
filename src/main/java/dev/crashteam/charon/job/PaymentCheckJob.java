package dev.crashteam.charon.job;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.charon.service.feign.YookassaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.spring.annotations.Recurring;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCheckJob {

    private final PaymentService historyService;
    private final YookassaClient client;

    @Recurring(id = "payment-status-check", cron = "*/10 * * * * *")
    @Job(name = "Payment status check")
    public void check() {
        var paymentHistories = historyService
                .getPaymentByStatus(RequestPaymentStatus.PENDING.getTitle()).stream();
        BackgroundJob.enqueue(paymentHistories, payment -> {
            client.paymentStatus(payment.getStatus());
        });
    }
}
