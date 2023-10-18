package dev.crashteam.charon.job;

import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.charon.service.feign.YookassaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCheckJob implements Job {

    private final PaymentService paymentService;
    private final YookassaClient client;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        var payments = paymentService
                .getPaymentByStatus(RequestPaymentStatus.PENDING.getTitle()).stream();
    }

    private void checkPayments(Payment payment) {
        client.paymentStatus(payment.getStatus());
    }
}
