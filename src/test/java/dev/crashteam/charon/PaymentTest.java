package dev.crashteam.charon;

import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import dev.crashteam.charon.config.ContainerConfiguration;
import dev.crashteam.charon.config.WireMockConfig;
import dev.crashteam.charon.grpc.PaymentServiceImpl;
import dev.crashteam.charon.job.BalancePaymentJob;
import dev.crashteam.charon.job.PurchaseServiceJob;
import dev.crashteam.charon.job.RecurrentPaymentJob;
import dev.crashteam.charon.mock.LavaMock;
import dev.crashteam.charon.mock.YookassaMock;
import dev.crashteam.charon.model.Operation;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.model.domain.User;
import dev.crashteam.charon.model.domain.UserSavedPayment;
import dev.crashteam.charon.model.dto.FkCallbackData;
import dev.crashteam.charon.model.dto.currency.ExchangeDto;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.repository.PromoCodeRepository;
import dev.crashteam.charon.repository.UserRepository;
import dev.crashteam.charon.service.*;
import dev.crashteam.charon.service.feign.CurrencyApiClient;
import dev.crashteam.charon.service.feign.NinjaClient;
import dev.crashteam.charon.publisher.handler.AwsStreamPublisherHandler;
import dev.crashteam.charon.util.PaymentProtoUtils;
import dev.crashteam.payment.*;
import io.grpc.internal.testing.StreamRecorder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
@DirtiesContext
@SpringBootTest
@ActiveProfiles({"test"})
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ImportAutoConfiguration({FeignAutoConfiguration.class})
@ContextConfiguration(classes = {WireMockConfig.class})
public class PaymentTest extends ContainerConfiguration {

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

    @Autowired
    CallbackService callbackService;

    @Autowired
    OperationTypeService operationTypeService;

    @Autowired
    UserSavedPaymentService savedPaymentService;

    @Autowired
    RecurrentPaymentJob recurrentPaymentJob;

    @Autowired
    UserService userService;

    @MockBean
    AwsStreamPublisherHandler awsStreamPublisherHandler;

    @MockBean
    NinjaClient ninjaClient;

    @MockBean
    CurrencyApiClient currencyApiClient;

    @BeforeEach
    public void setup() throws IOException {
        Mockito.when(awsStreamPublisherHandler.publishPaymentCreatedMessage(Mockito.any())).thenReturn(new PutRecordsResult());
        Mockito.when(awsStreamPublisherHandler.publishPaymentStatusChangeMessage(Mockito.any())).thenReturn(new PutRecordsResult());

        Map<String, ExchangeDto.ExchangeData> data = new HashMap<>();
        data.put("RUB", new ExchangeDto.ExchangeData("RUB", BigDecimal.valueOf(92.027499)));
        Mockito.when(currencyApiClient.exchangeRate(Mockito.any(), Mockito.any())).thenReturn(new ExchangeDto(data));

        YookassaMock.createPayment(mockServer);
        YookassaMock.createRefundPayment(mockServer);
        YookassaMock.paymentStatus(mockServer);

        LavaMock.paymentStatus(mockServer);
        LavaMock.createPayment(mockServer);

        paymentRepository.deleteAll();
    }

    @Test
    public void createPurchaseServicePaymentWithPromoCodeTest() {
        String userId = UUID.randomUUID().toString();

        LocalDateTime ldt = LocalDate.parse("2099-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
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

        List<PaidService> paidServices = getPaidServices();
        var purchaseService = PaymentCreateRequest.PaymentPurchaseService.newBuilder()
                .setUserId(userId)
                .setPromoCode(promoCode.get().getCode())
                .setMultiply(2)
                .setPaymentSystem(PaymentSystem.PAYMENT_SYSTEM_FREEKASSA)
                .setReturnUrl("return-test.test")
                .addAllPaidServices(paidServices)
                .build();

        PaymentCreateRequest paymentCreateRequest = PaymentCreateRequest.newBuilder()
                .setPaymentPurchaseService(purchaseService)
                .build();
        StreamRecorder<PaymentCreateResponse> paymentCreateObserver = StreamRecorder.create();
        grpcService.createPayment(paymentCreateRequest, paymentCreateObserver);

        PaymentCreateResponse payment = paymentCreateObserver.getValues().get(0);
        Payment paymentEntity = paymentService.findByPaymentId(payment.getPaymentId());

        FkCallbackData fkCallbackData = new FkCallbackData();
        fkCallbackData.setMerchantId("test");
        fkCallbackData.setAmount(String.valueOf(PaymentProtoUtils.getMajorMoneyAmount(paymentEntity.getProviderAmount())));
        fkCallbackData.setPaymentId(payment.getPaymentId());
        fkCallbackData.setCurId("test");
        fkCallbackData.setOrderId("1");
        callbackService.freeKassaCallback(fkCallbackData);

        Assertions.assertNull(paymentCreateObserver.getError());

        Assertions.assertTrue(promoCodeRepository.findByCodeAndUserId(promoCode.get().getCode(), userId).isPresent());
    }

    @Test
    public void purchaseServiceTest() {
        User user = new User();
        user.setCurrency("USD");
        user.setBalance(20000L);
        user.setId(UUID.randomUUID().toString());
        userRepository.save(user);
        List<PaidService> paidServices = getPaidServices();

        PurchaseServiceRequest purchaseServiceRequest = PurchaseServiceRequest.newBuilder()
                .addAllPaidServices(paidServices)
                .setUserId(user.getId())
                .setMultiply(1)
                .setOperationId(UUID.randomUUID().toString())
                .build();

        StreamRecorder<PurchaseServiceResponse> responseStreamRecorder = StreamRecorder.create();
        grpcService.purchaseService(purchaseServiceRequest, responseStreamRecorder);
    }

    @Test
    public void purchaseServiceTestIdempotentError() {
        User user = new User();
        user.setCurrency("RUB");
        user.setBalance(1000000L);
        user.setId(UUID.randomUUID().toString());
        userRepository.save(user);

        String operationId = UUID.randomUUID().toString();

        PurchaseServiceRequest purchaseServiceRequest = PurchaseServiceRequest.newBuilder()
                .setMultiply(2)
                .addAllPaidServices(getPaidServices())
                .setUserId(user.getId())
                .setOperationId(operationId)
                .build();
        PurchaseServiceRequest secondServiceRequest = PurchaseServiceRequest.newBuilder()
                .setMultiply(1)
                .addAllPaidServices(getPaidServices())
                .setUserId(user.getId())
                .setOperationId(operationId)
                .build();

        StreamRecorder<PurchaseServiceResponse> responseStreamRecorder = StreamRecorder.create();
        grpcService.purchaseService(purchaseServiceRequest, responseStreamRecorder);
        grpcService.purchaseService(secondServiceRequest, responseStreamRecorder);
        Assertions.assertNotNull(responseStreamRecorder.getValues().get(0).getErrorResponse());

    }

    @Test
    public void createPurchaseServiceLavaPaymentTest() {
        String userId = UUID.randomUUID().toString();
        var purchaseService = PaymentCreateRequest.PaymentPurchaseService.newBuilder()
                .setUserId(userId)
                .setMultiply(2)
                .addAllPaidServices(getPaidServices())
                .setPaymentSystem(PaymentSystem.PAYMENT_SYSTEM_LAVA)
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
        Optional<Payment> paymentAfterJob = paymentRepository.findByPaymentId(paymentId);
        Assertions.assertEquals(paymentAfterJob.get().getStatus(), RequestPaymentStatus.SUCCESS);

    }

    @Test
    public void createPurchaseServiceFreeKassaPaymentTest() {
        String userId = UUID.randomUUID().toString();
        var purchaseService = PaymentCreateRequest.PaymentPurchaseService.newBuilder()
                .setUserId(userId)
                .setMultiply(1)
                .addAllPaidServices(getPaidServices())
                .setPaymentSystem(PaymentSystem.PAYMENT_SYSTEM_FREEKASSA)
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
        Optional<Payment> paymentAfterJob = paymentRepository.findByPaymentId(paymentId);
        Assertions.assertEquals(paymentAfterJob.get().getStatus(), RequestPaymentStatus.PENDING);

//        callbackService.freeKassaCallback(new FkCallbackData("", "1500.0", "order-id", paymentId, "cur"));
//
//        Optional<Payment> successPayment = paymentRepository.findByPaymentId(paymentId);
//        Assertions.assertEquals(successPayment.get().getStatus(), RequestPaymentStatus.SUCCESS);
    }

    @Test
    public void createPurchaseServicePaymentTest() {
        String userId = UUID.randomUUID().toString();
        var purchaseService = PaymentCreateRequest.PaymentPurchaseService.newBuilder()
                .setUserId(userId)
                .setMultiply(1)
                .addAllPaidServices(getPaidServices())
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
                .setAmount(3000)
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

        Assertions.assertEquals(3000L, (long) userRepository.getById(userId).getBalance());
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

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setBalance(0L);
        user.setCurrency("USD");

        payment.setUser(userRepository.save(user));
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
    public void paymentsEnumTypeTest() {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID().toString());
        payment.setExternalId("22e12f66-000f-5000-8000-18db351245c7");
        payment.setStatus(RequestPaymentStatus.PENDING);
        payment.setCurrency("USD");
        payment.setAmount(1000L);
        payment.setOperationType(operationTypeService.getOperationType(Operation.PURCHASE_SERVICE.getTitle()));
        payment.setStatus(RequestPaymentStatus.PENDING);
        paymentRepository.save(payment);

        List<Payment> paymentByType = paymentService
                .getPaymentByPendingStatusAndOperationType(Operation.PURCHASE_SERVICE.getTitle());

        Assertions.assertNotNull(paymentByType);
    }

    @Test
    public void getPaymentsTest() {
        Payment payment = new Payment();
        payment.setPaymentId("request_payment_id");
        payment.setExternalId("22e12f66-000f-5000-8000-18db351245c7");
        payment.setStatus(RequestPaymentStatus.PENDING);
        payment.setCurrency("RUB");
        payment.setAmount(1000L);

        Payment paymentSuccess = new Payment();
        paymentSuccess.setPaymentId("22e12f66-000f-5000-8000-18db351245c4");
        paymentSuccess.setExternalId("22e12f66-000f-5000-8000-18db351245c7");
        paymentSuccess.setStatus(RequestPaymentStatus.SUCCESS);
        paymentSuccess.setCurrency("RUB");
        paymentSuccess.setAmount(1000L);

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setBalance(0L);
        user.setCurrency("USD");

        User saved = userRepository.save(user);

        paymentSuccess.setUser(user);
        paymentSuccess.setCreated(LocalDate.parse("2022-07-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
        paymentSuccess.setUpdated(LocalDateTime.now());
        paymentRepository.save(paymentSuccess);

        payment.setUser(saved);
        payment.setCreated(LocalDate.parse("2022-07-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
        payment.setUpdated(LocalDateTime.now());
        paymentRepository.save(payment);

        Payment secondPayment = new Payment();
        secondPayment.setPaymentId("second_request_payment_id");
        secondPayment.setExternalId("23e12f62-000f-5000-8000-18db351245c7");
        secondPayment.setStatus(RequestPaymentStatus.PENDING);
        secondPayment.setCurrency("RUB");
        secondPayment.setAmount(1000L);
        User user2 = new User();
        user2.setId(UUID.randomUUID().toString());
        user2.setBalance(0L);
        user2.setCurrency("USD");

        secondPayment.setUser(userRepository.save(user2));
        secondPayment.setCreated(LocalDate.parse("2022-07-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
        secondPayment.setUpdated(LocalDateTime.now());
        paymentRepository.save(secondPayment);

        PaymentsQuery paymentsQuery = PaymentsQuery.newBuilder()
                .setPagination(LimitOffsetPagination.newBuilder().setLimit(100L).setOffset(0).build())
                .build();

        PaymentsQuery datePaymentsQuery = PaymentsQuery.newBuilder()
                .setPagination(LimitOffsetPagination.newBuilder().setLimit(100L).setOffset(0).build())
                .setDateTo(StringValue.newBuilder().setValue("2019-07-19").build())
                .build();

        StreamRecorder<PaymentsResponse> paymentsResponseObserver = StreamRecorder.create();
        grpcService.getPayments(paymentsQuery, paymentsResponseObserver);
        Assertions.assertNull(paymentsResponseObserver.getError());
        List<PaymentsResponse> paymentResponses = paymentsResponseObserver.getValues();
        Assertions.assertFalse(CollectionUtils.isEmpty(paymentResponses));

        Page<Payment> payments = paymentService.getPayments(paymentsQuery);
        Assertions.assertNotNull(payments.getContent());
        Assertions.assertTrue(payments.stream().allMatch(it -> it.getStatus().equals(RequestPaymentStatus.SUCCESS)));

        Page<Payment> secondPayments = paymentService.getPayments(datePaymentsQuery);
        Assertions.assertEquals(0L, secondPayments.getTotalElements());
    }

    private List<PaidService> getPaidServices() {
        PaidService kePaidService = PaidService.newBuilder().setContext(PaidServiceContext.newBuilder()
                .setKeAnalyticsContext(KeAnalyticsContext.newBuilder()
                        .setPlan(KeAnalyticsContext.KeAnalyticsPlan.newBuilder()
                                .setDefaultPlan(KeAnalyticsContext.KeAnalyticsPlan.KeAnalyticsDefaultPlan
                                        .newBuilder().buildPartial()).build()).build())).build();
        PaidService uzumPaidService = PaidService.newBuilder().setContext(PaidServiceContext.newBuilder()
                .setUzumAnalyticsContext(UzumAnalyticsContext.newBuilder()
                        .setPlan(UzumAnalyticsContext.UzumAnalyticsPlan.newBuilder()
                                .setDefaultPlan(UzumAnalyticsContext.UzumAnalyticsPlan.UzumAnalyticsDefaultPlan
                                        .newBuilder().buildPartial()).build()).build())).build();
        return List.of(kePaidService, uzumPaidService);
    }

    @Test
    public void testRecurrentPayment() {
        String userId = UUID.randomUUID().toString();
        PaidService paidService = PaidService.newBuilder().setContext(PaidServiceContext.newBuilder()
                .setKeAnalyticsContext(KeAnalyticsContext.newBuilder()
                        .setPlan(KeAnalyticsContext.KeAnalyticsPlan.newBuilder()
                                .setDefaultPlan(KeAnalyticsContext.KeAnalyticsPlan.KeAnalyticsDefaultPlan
                                        .newBuilder().buildPartial()).build()).build())).build();
        var purchaseService = PaymentCreateRequest.PaymentPurchaseService.newBuilder()
                .setUserId(userId)
                .setMultiply(1)
                .setPaidService(paidService)
                .setPaymentSystem(PaymentSystem.PAYMENT_SYSTEM_YOOKASSA)
                .setReturnUrl("return-test.test")
                .setSavePaymentMethod(true)
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

        Assertions.assertTrue(successPayment.isPresent());

        UserSavedPayment userSavedPayment = savedPaymentService.findByUserId(successPayment.get().getUser().getId());
        Assertions.assertNotNull(userSavedPayment);
        Assertions.assertEquals(successPayment.get().getStatus(), RequestPaymentStatus.SUCCESS);

        Optional<User> userOptional = userRepository.findById(userId);
        Assertions.assertTrue(userOptional.isPresent());

        User user = userOptional.get();
        Assertions.assertNotNull(user.getSubscriptionValidUntil());
        user.setSubscriptionValidUntil(LocalDateTime.now()); //Set subscription valid on today for recurrent job to work
        LocalDateTime oldSubscriptionValidUntil = user.getSubscriptionValidUntil();
        userRepository.save(user);

        List<User> todaySubscriptionEnds = userService.findTodaySubscriptionEnds();
        Assertions.assertFalse(todaySubscriptionEnds.isEmpty());

        paymentRepository.delete(payment.get()); // Delete old payment (for test)

        for (User todaySubscriptionEndUser : todaySubscriptionEnds) {
            recurrentPaymentJob.processPayment(userSavedPayment, todaySubscriptionEndUser); //creating recurrent payment
        }

        List<Payment> betweenTimeRange = paymentService
                .getPaymentByPendingStatusAndOperationTypeBetweenTimeRange(Operation.PURCHASE_SERVICE.getTitle());

        Assertions.assertFalse(betweenTimeRange.isEmpty());
        for (Payment pendingPayment : betweenTimeRange) {
            purchaseServiceJob.checkPaymentStatus(pendingPayment); // processing payment
        }
        Optional<User> updatedUser = userRepository.findById(userId);
        Assertions.assertTrue(updatedUser.isPresent());
        Assertions.assertNotEquals(updatedUser.get().getSubscriptionValidUntil(), oldSubscriptionValidUntil);

        // cancel recurrent payment
        RecurrentPaymentCancelRequest cancelRequest = RecurrentPaymentCancelRequest
                .newBuilder()
                .setUserId(userId)
                .build();
        StreamRecorder<RecurrentPaymentCancelResponse> paymentCancelObserver = StreamRecorder.create();
        grpcService.recurrentPaymentCancel(cancelRequest, paymentCancelObserver);
        Assertions.assertNull(paymentCreateObserver.getError());

        Assertions.assertNull(savedPaymentService.findByUserId(userId));
    }

    @Test
    public void testOldRecurrentPayment() {
        User user = new User();
        user.setCurrency("RUB");
        user.setId("XNWAhWBTT4QeDtNnikroYZKO6jx4");
        user.setBalance(400000L);
        user.setSubscriptionValidUntil(LocalDateTime.now().minusMonths(3));
        userRepository.save(user);

        PaidService paidService = PaidService.newBuilder().setContext(PaidServiceContext.newBuilder()
                .setKeAnalyticsContext(KeAnalyticsContext.newBuilder()
                        .setPlan(KeAnalyticsContext.KeAnalyticsPlan.newBuilder()
                                .setDefaultPlan(KeAnalyticsContext.KeAnalyticsPlan.KeAnalyticsDefaultPlan
                                        .newBuilder().buildPartial()).build()).build())).build();
        var purchaseService = PaymentCreateRequest.PaymentPurchaseService.newBuilder()
                .setUserId(user.getId())
                .setMultiply(1)
                .setPaidService(paidService)
                .setPaymentSystem(PaymentSystem.PAYMENT_SYSTEM_YOOKASSA)
                .setReturnUrl("return-test.test")
                .setSavePaymentMethod(true)
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
        User userWithPurchase = userRepository.findById(user.getId()).get();
        Assertions.assertTrue(userWithPurchase.getSubscriptionValidUntil().isAfter(LocalDateTime.now()));

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
