package dev.crashteam.charon.job;

import dev.crashteam.charon.mapper.ProtoMapper;
import dev.crashteam.charon.model.*;
import dev.crashteam.charon.model.domain.*;
import dev.crashteam.charon.model.dto.UserSavedPaymentResolverDto;
import dev.crashteam.charon.model.dto.resolver.PaymentData;
import dev.crashteam.charon.publisher.handler.StreamPublisherHandler;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.resolver.PaymentResolver;
import dev.crashteam.charon.service.OperationTypeService;
import dev.crashteam.charon.service.UserSavedPaymentService;
import dev.crashteam.charon.service.UserService;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.PaymentSystem;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@DisallowConcurrentExecution
public class RecurrentPaymentJob implements Job {

    @Autowired
    UserSavedPaymentService savedPaymentService;
    @Autowired
    UserService userService;
    @Autowired
    List<PaymentResolver> resolvers;
    @Autowired
    StreamPublisherHandler publisherHandler;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    OperationTypeService operationTypeService;
    @Autowired
    ProtoMapper protoMapper;

    @Override
    @Transactional
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        for (User user : userService.findTodaySubscriptionEnds()) {
            UserSavedPayment savedPayment = savedPaymentService.findByUserId(user.getId());
            processPayment(savedPayment, user);
        }
    }

    @Transactional
    public void processPayment(UserSavedPayment savedPayment, User user) {
        if (savedPayment != null) {
            PaymentSystemType systemType = PaymentSystemType.getByTitle(savedPayment.getPaymentSystem());
            PaymentResolver paymentResolver = resolvers.stream()
                    .filter(it -> it.getPaymentSystem().equals(PaymentSystem.forNumber(systemType.getNumberValue())))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);

            PaidService paidService = savedPayment.getPaidService();
            Long monthPaid = savedPayment.getMonthPaid();
            long multipliedAmount = paidService.getAmount() * monthPaid;
            Long amount = PaymentProtoUtils
                    .multiplyDiscount(multipliedAmount, monthPaid, paidService.getSubscriptionType());
            BigDecimal moneyAmount = PaymentProtoUtils.getMajorMoneyAmount(amount);
            UserSavedPaymentResolverDto savedPaymentDto = UserSavedPaymentResolverDto.builder()
                    .paymentId(savedPayment.getPaymentId())
                    .userId(savedPayment.getUserId())
                    .amount(String.valueOf(moneyAmount))
                    .build();
            PaymentData response = paymentResolver.recurrentPayment(savedPaymentDto);

            Optional<Payment> optionalPayment = paymentRepository.findByExternalId(response.getProviderId());

            Payment payment;
            if (optionalPayment.isPresent()) {
                payment = optionalPayment.get();
            } else {
                payment = new Payment();
                payment.setPaymentId(response.getPaymentId());
            }
            payment.setExternalId(response.getProviderId());
            payment.setStatus(RequestPaymentStatus.PENDING);
            payment.setCurrency(Currency.RUB.getTitle());
            payment.setAmount(amount);
            payment.setProviderAmount(Long.valueOf(response.getProviderAmount()));
            payment.setProviderCurrency(response.getProviderCurrency());
            payment.setUser(user);
            payment.setCreated(LocalDateTime.now());
            payment.setUpdated(LocalDateTime.now());
            payment.setOperationType(operationTypeService.getOperationType(Operation.PURCHASE_SERVICE.getTitle()));
            payment.setMonthPaid(monthPaid);
            payment.setEmail(response.getEmail());
            payment.setPhone(response.getPhone());
            payment.setPaymentSystem(systemType.getTitle());
            payment.setExchangeRate(response.getExchangeRate());
            paymentRepository.save(payment);
            log.info("Saving payment with paymentId - {}", response.getPaymentId());

            publisherHandler.publishPaymentCreatedMessage(payment);
        }
    }
}
