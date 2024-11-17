package dev.crashteam.charon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.crashteam.charon.config.ContainerConfiguration;
import dev.crashteam.charon.config.WireMockConfig;
import dev.crashteam.charon.mock.YookassaMock;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentCreateRequestDTO;
import dev.crashteam.charon.model.dto.yookassa.YkPaymentRefundRequestDTO;
import dev.crashteam.charon.service.feign.YookassaClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.UUID;

import static java.nio.charset.Charset.defaultCharset;
import static org.springframework.util.StreamUtils.copyToString;

@SpringBootTest
@DirtiesContext
@ActiveProfiles({"test"})
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WireMockConfig.class})
public class YookassaIntegrationTest extends ContainerConfiguration {

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
            YkPaymentRefundRequestDTO refundRequest = createRefundRequest();
            yookassaClient.refund(refundRequest);
        });
    }

    @Test
    public void testCreatePayment() {
        Assertions.assertDoesNotThrow(() -> {
            YkPaymentCreateRequestDTO request = createPaymentCreateRequest();
            yookassaClient.createPayment(request);
        });
    }

    private YkPaymentRefundRequestDTO createRefundRequest() throws IOException {
        String requestJson = getRequestJson("yookassa/payment-refund-request.json");
        return objectMapper.readValue(requestJson, YkPaymentRefundRequestDTO.class);
    }

    private YkPaymentCreateRequestDTO createPaymentCreateRequest() throws IOException {
        String requestJson = getRequestJson("yookassa/payment-create-request.json");
        return objectMapper.readValue(requestJson, YkPaymentCreateRequestDTO.class);
    }

    private String getRequestJson(String jsonPath) throws IOException {
        return copyToString(YookassaIntegrationTest.class
                .getClassLoader().getResourceAsStream(jsonPath), defaultCharset());
    }
}
