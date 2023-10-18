package dev.crashteam.charon;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.protobuf.StringValue;
import dev.crashteam.charon.config.WireMockConfig;
import dev.crashteam.charon.grpc.PaymentServiceImpl;
import dev.crashteam.charon.mock.YookassaMock;
import dev.crashteam.charon.model.RequestPaymentStatus;
import dev.crashteam.charon.model.domain.Payment;
import dev.crashteam.charon.repository.PaymentRepository;
import dev.crashteam.charon.service.PaymentService;
import dev.crashteam.payment.*;
import io.grpc.internal.testing.StreamRecorder;
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

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@SpringBootTest
@ActiveProfiles({"test"})
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WireMockConfig.class})
public class PaymentTest {

    @Autowired
    WireMockServer mockYookassaClient;

    @Autowired
    PaymentServiceImpl grpcService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    PaymentRepository paymentRepository;

    @BeforeEach
    public void setup() throws IOException {
        YookassaMock.createPayment(mockYookassaClient);
        YookassaMock.createRefundPayment(mockYookassaClient);
        YookassaMock.paymentStatus(mockYookassaClient);
    }

    @BeforeEach
    public void clearPayments() {
        paymentRepository.deleteAll();
    }

    @Test
    public void createPaymentTest() {
        PaymentCreateRequest paymentCreateRequest = PaymentCreateRequest.newBuilder()
                .setUserId("test_user_id")
                .setAmount(Amount.newBuilder().setCurrency("RUB").setValue(1000L).build())
                .setDescription("Some test")
                .setReturnUrl("return.com")
                .build();
        StreamRecorder<PaymentCreateResponse> paymentCreateObserver = StreamRecorder.create();
        grpcService.createPayment(paymentCreateRequest, paymentCreateObserver);
        Assertions.assertNull(paymentCreateObserver.getError());
    }

    @Test
    public void getPaymentTest() {
        Payment payment = new Payment();
        payment.setPaymentId("request_payment_id");
        payment.setExternalId("22e12f66-000f-5000-8000-18db351245c7");
        payment.setStatus("pending");
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
        payment.setStatus("pending");
        payment.setCurrency("RUB");
        payment.setAmount(1000L);
        payment.setUserId("user_id");
        payment.setCreated(LocalDate.parse("2022-07-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
        payment.setUpdated(LocalDateTime.now());
        paymentRepository.save(payment);

        Payment secondPayment = new Payment();
        secondPayment.setPaymentId("second_request_payment_id");
        secondPayment.setExternalId("23e12f62-000f-5000-8000-18db351245c7");
        secondPayment.setStatus("pending");
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

    @Test
    public void testRefundPayment() {
        Payment payment = new Payment();
        payment.setPaymentId("request_refund_payment_id");
        payment.setExternalId("22e12f66-000f-5000-8000-18db351245c7");
        payment.setStatus("pending");
        payment.setCurrency("RUB");
        payment.setAmount(1000L);
        payment.setUserId("user_id");
        payment.setCreated(LocalDate.parse("2022-07-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay());
        payment.setUpdated(LocalDateTime.now());
        paymentRepository.save(payment);

        StreamRecorder<PaymentRefundResponse> refundObserver = StreamRecorder.create();
        PaymentRefundRequest refundRequest = PaymentRefundRequest.newBuilder()
                .setAmount(Amount.newBuilder().setValue(1000L).setCurrency("RUB").build())
                .setPaymentId("request_refund_payment_id")
                .setUserId("user_id").build();
        grpcService.refundPayment(refundRequest, refundObserver);
        Assertions.assertNull(refundObserver.getError());

        Payment refundedPayment = paymentRepository.findByPaymentId("request_refund_payment_id")
                .orElseThrow(EntityNotFoundException::new);
        Assertions.assertEquals(RequestPaymentStatus.SUCCESS.getTitle(), refundedPayment.getStatus());
    }
}
