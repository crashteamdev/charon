package dev.crashteam.charon.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.crashteam.charon.util.IntegrationUtils;

import java.io.IOException;

public class TbankMock {

    public static void paymentStatus(WireMockServer mockService) throws IOException {
        IntegrationUtils.createPostMockStub(mockService, "/tbank/GetState",
                "tbank/payment-status-response.json");
    }

    public static void createPayment(WireMockServer mockService) throws IOException {
        IntegrationUtils.createPostMockStub(mockService, "/tbank/Init",
                "tbank/payment-create-response.json");
    }

    public static void chargePayment(WireMockServer mockService) throws IOException {
        IntegrationUtils.createPostMockStub(mockService, "/tbank/Charge",
                "tbank/payment-create-response.json");
    }
}
