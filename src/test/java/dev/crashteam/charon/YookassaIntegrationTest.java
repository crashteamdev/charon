package dev.crashteam.charon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.crashteam.charon.config.WireMockConfig;
import dev.crashteam.charon.mock.YookassaMock;
import dev.crashteam.charon.model.dto.yookassa.PaymentCreateRequestDTO;
import dev.crashteam.charon.model.dto.yookassa.PaymentRefundRequestDTO;
import dev.crashteam.charon.service.feign.YookassaClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static java.nio.charset.Charset.defaultCharset;
import static org.springframework.util.StreamUtils.copyToString;

@SpringBootTest
@ActiveProfiles({"test"})
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WireMockConfig.class})
public class YookassaIntegrationTest {

    @Autowired
    WireMockServer mockYookassaClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    YookassaClient yookassaClient;

    @BeforeEach
    public void setup() throws IOException {
        YookassaMock.createPayment(mockYookassaClient);
        YookassaMock.createRefundPayment(mockYookassaClient);
        YookassaMock.paymentStatus(mockYookassaClient);
    }

    @Test
    public void testPaymentStatus() {
        Assertions.assertDoesNotThrow(() -> {
            yookassaClient.paymentStatus("22e12f66-000f-5000-8000-18db351245c7");
        });
    }

    @Test
    public void testRefund() {
        Assertions.assertDoesNotThrow(() -> {
            PaymentRefundRequestDTO refundRequest = createRefundRequest();
            yookassaClient.refund(refundRequest);
        });
    }

    @Test
    public void testCreatePayment() {
        Assertions.assertDoesNotThrow(() -> {
            PaymentCreateRequestDTO request = createPaymentCreateRequest();
            yookassaClient.createPayment(request);
        });
    }

    private PaymentRefundRequestDTO createRefundRequest() throws IOException {
        String requestJson = getRequestJson("yookassa/payment-refund-request.json");
        return objectMapper.readValue(requestJson, PaymentRefundRequestDTO.class);
    }

    private PaymentCreateRequestDTO createPaymentCreateRequest() throws IOException {
        String requestJson = getRequestJson("yookassa/payment-create-request.json");
        return objectMapper.readValue(requestJson, PaymentCreateRequestDTO.class);
    }

    private String getRequestJson(String jsonPath) throws IOException {
        return copyToString(YookassaIntegrationTest.class
                .getClassLoader().getResourceAsStream(jsonPath), defaultCharset());
    }
}
