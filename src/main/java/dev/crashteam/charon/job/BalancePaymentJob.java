package dev.crashteam.charon.job;

import dev.crashteam.charon.model.Operation;
import dev.crashteam.charon.model.PaymentSystemType;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.charon.service.UserService;
import dev.crashteam.charon.resolver.PaymentResolver;
import dev.crashteam.payment.PaymentSystem;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@DisallowConcurrentExecution
public class BalancePaymentJob implements Job {

    @Autowired
    PaymentService paymentService;
    @Autowired
    UserService userService;
    @Autowired
    List<PaymentResolver> resolvers;

    @Override
    @Transactional
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        var payments = paymentService
                .getPaymentByStatusAndOperationType(RequestPaymentStatus.PENDING, Operation.DEPOSIT_BALANCE.getTitle()).stream();
        payments.forEach(this::checkPaymentStatus);
    }

    @Transactional
    public void checkPaymentStatus(Payment payment) {
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
            long newBalance = user.getBalance() + payment.getAmount();
            user.setBalance(newBalance);
            userService.saveUser(user);

            payment.setStatus(RequestPaymentStatus.SUCCESS);
            paymentService.save(payment);
        }
    }
}
