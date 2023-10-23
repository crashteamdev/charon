package dev.crashteam.charon;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import dev.crashteam.charon.config.WireMockConfig;
import dev.crashteam.charon.grpc.PaymentServiceImpl;
import dev.crashteam.charon.job.BalancePaymentJob;
import dev.crashteam.charon.job.PurchaseServiceJob;
import dev.crashteam.charon.mock.NinjaMock;
import dev.crashteam.charon.mock.YookassaMock;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.repository.PromoCodeRepository;
import dev.crashteam.charon.repository.UserRepository;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.payment.*;
import io.grpc.internal.testing.StreamRecorder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;


@Slf4j
@SpringBootTest
@ActiveProfiles({"test"})
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WireMockConfig.class})
public class PaymentTest {

    @Autowired
    WireMockServer mockServer;

    @Autowired
    PaymentServiceImpl grpcService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BalancePaymentJob balancePaymentJob;

    @Autowired
    PurchaseServiceJob purchaseServiceJob;

    @Autowired
    PromoCodeRepository promoCodeRepository;

    @BeforeEach
    public void setup() throws IOException {
        YookassaMock.createPayment(mockServer);
        YookassaMock.createRefundPayment(mockServer);
        YookassaMock.paymentStatus(mockServer);
        NinjaMock.currencyResponse(mockServer, Map.of("have", equalTo("USD"),
                "want", equalTo("RUB"), "amount", equalTo("30")));
        NinjaMock.currencyResponse(mockServer, Map.of("have", equalTo("USD"),
                "want", equalTo("RUB"), "amount", equalTo("21")));
    }

    @BeforeEach
    public void clearPayments() {
        paymentRepository.deleteAll();
    }

    @BeforeEach
    public void clearUsers() {
        userRepository.deleteAll();
    }

    @BeforeEach
    public void clearPromoCodes() {
        promoCodeRepository.deleteAll();
    }

    @Test
    public void createPurchaseServicePaymentWithPromoCodeTest() {
        String userId = UUID.randomUUID().toString();

        DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate ld = LocalDate.parse("2099-01-01", FORMATTER);

        LocalDateTime ldt = LocalDateTime.of(ld, LocalDateTime.now().toLocalTime());
        Instant instantCreated = ldt.toInstant(ZoneOffset.UTC);
        CreatePromoCodeRequest promoCodeRequest = CreatePromoCodeRequest.newBuilder()
                .setPromoCodeContext(PromoCodeContext.newBuilder()
                        .setDiscountPromocodeContext(DiscountPromoCodeContext.newBuilder()
                                .setDiscountPercentage(30).build()))
                .setValidUntil(Timestamp.newBuilder().setSeconds(instantCreated.getEpochSecond())
                        .setNanos(instantCreated.getNano()).build())
                .setUsageLimit(1)
                .build();
        StreamRecorder<CreatePromoCodeResponse> promoCodeResponse = StreamRecorder.create();
        grpcService.createPromoCode(promoCodeRequest, promoCodeResponse);
        Assertions.assertNull(promoCodeResponse.getError());
        String createdCode = promoCodeResponse.getValues().get(0).getPromoCode().getCode();
        Optional<dev.crashteam.charon.model.domain.PromoCode> promoCode = promoCodeRepository.findByCode(createdCode);
        Assertions.assertTrue(promoCode.isPresent());

        var purchaseService = PaymentCreateRequest.PaymentPurchaseService.newBuilder()
                .setUserId(userId)
                .setPromoCode(promoCode.get().getCode())
                .setPaidService(PaidService.newBuilder().setContext(PaidServiceContext.newBuilder()
                        .setMultiply(2).setKeAnalyticsContext(KeAnalyticsContext.newBuilder()
                                .setPlan(KeAnalyticsContext.KeAnalyticsPlan.newBuilder()
                                        .setDefaultPlan(KeAnalyticsContext.KeAnalyticsPlan.KeAnalyticsDefaultPlan
                                                .newBuilder().buildPartial()).build()).build())).build())
                .setPaymentSystem(PaymentSystem.PAYMENT_SYSTEM_YOOKASSA)
                .setReturnUrl("return-test.test")
                .build();
        PaymentCreateRequest paymentCreateRequest = PaymentCreateRequest.newBuilder()
                .setPaymentPurchaseService(purchaseService)
                .build();
        StreamRecorder<PaymentCreateResponse> paymentCreateObserver = StreamRecorder.create();
        grpcService.createPayment(paymentCreateRequest, paymentCreateObserver);
        Assertions.assertNull(paymentCreateObserver.getError());

        Assertions.assertTrue(promoCodeRepository.findByCodeAndUserId(promoCode.get().getCode(), userId).isPresent());
    }

    @Test
    public void createPurchaseServicePaymentTest() {
        String userId = UUID.randomUUID().toString();
        var purchaseService = PaymentCreateRequest.PaymentPurchaseService.newBuilder()
                .setUserId(userId)
                .setPaidService(PaidService.newBuilder().setContext(PaidServiceContext.newBuilder()
                        .setMultiply(2).setKeAnalyticsContext(KeAnalyticsContext.newBuilder()
                                .setPlan(KeAnalyticsContext.KeAnalyticsPlan.newBuilder()
                                        .setDefaultPlan(KeAnalyticsContext.KeAnalyticsPlan.KeAnalyticsDefaultPlan
                                                .newBuilder().buildPartial()).build()).build())).build())
                .setPaymentSystem(PaymentSystem.PAYMENT_SYSTEM_YOOKASSA)
                .setReturnUrl("return-test.test")
                .build();
        PaymentCreateRequest paymentCreateRequest = PaymentCreateRequest.newBuilder()
                .setPaymentPurchaseService(purchaseService)
                .build();
        StreamRecorder<PaymentCreateResponse> paymentCreateObserver = StreamRecorder.create();
        grpcService.createPayment(paymentCreateRequest, paymentCreateObserver);
        Assertions.assertNull(paymentCreateObserver.getError());

        String paymentId = paymentCreateObserver.getValues().get(0).getPaymentId();
        Optional<Payment> payment = paymentRepository.findByPaymentId(paymentId);
        Assertions.assertTrue(payment.isPresent());

        purchaseServiceJob.checkPaymentStatus(payment.get());

        Optional<Payment> successPayment = paymentRepository.findByPaymentId(paymentId);
        Assertions.assertEquals(successPayment.get().getStatus(), RequestPaymentStatus.SUCCESS);
    }

    @Test
    public void createBalancePaymentTest() {
        String userId = UUID.randomUUID().toString();
        var depositUserBalance = PaymentCreateRequest.PaymentDepositUserBalance.newBuilder()
                .setUserId(userId)
                .setAmount(30)
                .setCurrency(PaymentCurrency.PAYMENT_CURRENCY_USD)
                .setPaymentSystem(PaymentSystem.PAYMENT_SYSTEM_YOOKASSA)
                .setReturnUrl("return-test.test")
                .build();
        PaymentCreateRequest paymentCreateRequest = PaymentCreateRequest.newBuilder()
                .setPaymentDepositUserBalance(depositUserBalance)
                .build();
        StreamRecorder<PaymentCreateResponse> paymentCreateObserver = StreamRecorder.create();
        grpcService.createPayment(paymentCreateRequest, paymentCreateObserver);
        Assertions.assertNull(paymentCreateObserver.getError());

        String paymentId = paymentCreateObserver.getValues().get(0).getPaymentId();
        Optional<Payment> payment = paymentRepository.findByPaymentId(paymentId);
        Assertions.assertTrue(payment.isPresent());

        balancePaymentJob.checkPaymentStatus(payment.get());

        Assertions.assertEquals(30L, (long) userRepository.getById(userId).getBalance());
        Optional<Payment> successPayment = paymentRepository.findByPaymentId(paymentId);
        Assertions.assertEquals(successPayment.get().getStatus(), RequestPaymentStatus.SUCCESS);
    }

    @Test
    public void getPaymentTest() {
        Payment payment = new Payment();
        payment.setPaymentId("request_payment_id");
        payment.setExternalId("22e12f66-000f-5000-8000-18db351245c7");
        payment.setStatus(RequestPaymentStatus.PENDING);
        payment.setCurrency("RUB");
        payment.setAmount(1000L);
        payment.setUserId("user_id");
        payment.setCreated(LocalDateTime.now());
        payment.setUpdated(LocalDateTime.now());
        paymentRepository.save(payment);

        PaymentQuery paymentQuery = PaymentQuery.newBuilder().setPaymentId("request_payment_id").build();
        StreamRecorder<PaymentResponse> paymentResponseObserver = StreamRecorder.create();
        grpcService.getPayment(paymentQuery, paymentResponseObserver);
        Assertions.assertNull(paymentResponseObserver.getError());

        UserPayment userPayment = paymentService
                .getUserPaymentByPaymentId(paymentQuery);
        Assertions.assertNotNull(userPayment);
    }

    @Test
    public void getPaymentsTest() {
        Payment payment = new Payment();
        payment.setPaymentId("request_payment_id");
        payment.setExternalId("22e12f66-000f-5000-8000-18db351245c7");
        payment.setStatus(RequestPaymentStatus.PENDING);
        payment.setCurrency("RUB");
        payment.setAmount(1000L);
        payment.setUserId("user_id");
        payment.setCreated(LocalDate.parse("2022-07-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
        payment.setUpdated(LocalDateTime.now());
        paymentRepository.save(payment);

        Payment secondPayment = new Payment();
        secondPayment.setPaymentId("second_request_payment_id");
        secondPayment.setExternalId("23e12f62-000f-5000-8000-18db351245c7");
        secondPayment.setStatus(RequestPaymentStatus.PENDING);
        secondPayment.setCurrency("RUB");
        secondPayment.setAmount(1000L);
        secondPayment.setUserId("other_user_id");
        secondPayment.setCreated(LocalDate.parse("2022-07-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
        secondPayment.setUpdated(LocalDateTime.now());
        paymentRepository.save(secondPayment);

        PaymentsQuery paymentsQuery = PaymentsQuery.newBuilder()
                .setPagination(LimitOffsetPagination.newBuilder().setLimit(100L).setOffset(0).build())
                .build();

        PaymentsQuery datePaymentsQuery = PaymentsQuery.newBuilder()
                .setPagination(LimitOffsetPagination.newBuilder().setLimit(100L).setOffset(0).build())
                .setDateTo(StringValue.newBuilder().setValue("2022-07-19").build())
                .build();

        StreamRecorder<PaymentsResponse> paymentsResponseObserver = StreamRecorder.create();
        grpcService.getPayments(paymentsQuery, paymentsResponseObserver);
        Assertions.assertNull(paymentsResponseObserver.getError());

        Page<Payment> payments = paymentService.getPayments(paymentsQuery);
        Assertions.assertNotNull(payments.getContent());
        Page<Payment> secondPayments = paymentService.getPayments(datePaymentsQuery);
        Assertions.assertEquals(0L, secondPayments.getTotalElements());
    }

//    @Test
//    public void testRefundPayment() {
//        Payment payment = new Payment();
//        payment.setPaymentId("request_refund_payment_id");
//        payment.setExternalId("22e12f66-000f-5000-8000-18db351245c7");
//        payment.setStatus("pending");
//        payment.setCurrency("RUB");
//        payment.setAmount(1000L);
//        payment.setUserId("user_id");
//        payment.setCreated(LocalDate.parse("2022-07-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
//        payment.setUpdated(LocalDateTime.now());
//        paymentRepository.save(payment);
//
//        StreamRecorder<PaymentRefundResponse> refundObserver = StreamRecorder.create();
//        PaymentRefundRequest refundRequest = PaymentRefundRequest.newBuilder()
//                .setAmount(Amount.newBuilder().setValue(1000L).setCurrency("RUB").build())
//                .setPaymentId("request_refund_payment_id")
//                .setUserId("user_id").build();
//        grpcService.refundPayment(refundRequest, refundObserver);
//        Assertions.assertNull(refundObserver.getError());
//
//        Payment refundedPayment = paymentRepository.findByPaymentId("request_refund_payment_id")
//                .orElseThrow(EntityNotFoundException::new);
//        Assertions.assertEquals(RequestPaymentStatus.SUCCESS.getTitle(), refundedPayment.getStatus());
//    }
}
