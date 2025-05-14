package dev.crashteam.charon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dev.crashteam.charon.config.ContainerConfiguration;
import dev.crashteam.charon.config.WireMockConfig;
import dev.crashteam.charon.mock.TbankMock;
import dev.crashteam.charon.model.dto.tbank.GetStateRequestDTO;
import dev.crashteam.charon.model.dto.tbank.InitRequestDTO;
import dev.crashteam.charon.service.feign.TBankClient;
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

import static java.nio.charset.Charset.defaultCharset;
import static org.springframework.util.StreamUtils.copyToString;

@SpringBootTest
@DirtiesContext
@ActiveProfiles({"test"})
@EnableConfigurationProperties
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WireMockConfig.class})
public class TbankIntegrationTest extends ContainerConfiguration {

    @Autowired
    WireMockServer mockTbankClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TBankClient tBankClient;

    @BeforeEach
    public void setup() throws IOException {
        TbankMock.createPayment(mockTbankClient);
        TbankMock.chargePayment(mockTbankClient);
        TbankMock.paymentStatus(mockTbankClient);
    }

    @Test
    public void testPaymentStatus() {
        Assertions.assertDoesNotThrow(() -> {
            tBankClient.getState(createStateRequest());
        });
    }

    @Test
    public void testCreatePayment() {
        Assertions.assertDoesNotThrow(() -> {
            tBankClient.init(createPaymentCreateRequest());
        });
    }

    @Test
    public void testChargePayment() {
        Assertions.assertDoesNotThrow(() -> {
            tBankClient.init(createChargePaymentRequest());
        });
    }

    private GetStateRequestDTO createStateRequest() throws IOException {
        String requestJson = getRequestJson("tbank/payment-status-request.json");
        return objectMapper.readValue(requestJson, GetStateRequestDTO.class);
    }

    private InitRequestDTO createPaymentCreateRequest() throws IOException {
        String requestJson = getRequestJson("tbank/payment-create-request.json");
        return objectMapper.readValue(requestJson, InitRequestDTO.class);
    }

    private InitRequestDTO createChargePaymentRequest() throws IOException {
        String requestJson = getRequestJson("tbank/payment-charge-request.json");
        return objectMapper.readValue(requestJson, InitRequestDTO.class);
    }

    private String getRequestJson(String jsonPath) throws IOException {
        return copyToString(TbankIntegrationTest.class
                .getClassLoader().getResourceAsStream(jsonPath), defaultCharset());
    }
}
