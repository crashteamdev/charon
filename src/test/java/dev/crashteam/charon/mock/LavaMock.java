package dev.crashteam.charon.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import dev.crashteam.charon.util.IntegrationUtils;

import java.io.IOException;

public class LavaMock {

    public static void paymentStatus(WireMockServer mockService) throws IOException {
        IntegrationUtils.createPostMockStub(mockService, "/lava/status",
                "lava/payment-status-response.json");
    }

    public static void createPayment(WireMockServer mockService) throws IOException {
        IntegrationUtils.createPostMockStub(mockService, "/lava/create",
                "lava/payment-create-response.json");
    }
}
