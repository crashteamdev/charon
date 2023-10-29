package dev.crashteam.charon.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.crashteam.charon.YookassaIntegrationTest;
import dev.crashteam.charon.util.IntegrationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static java.nio.charset.Charset.defaultCharset;
import static org.springframework.util.StreamUtils.copyToString;

public class YookassaMock {

    public static void paymentStatus(WireMockServer mockService) throws IOException {
        IntegrationUtils.createGetMockStub(mockService, "/v3/payments/22e12f66-000f-5000-8000-18db351245c7",
                "yookassa/payment-status-response.json");
    }

    public static void createRefundPayment(WireMockServer mockService) throws IOException {
        IntegrationUtils.createPostMockStub(mockService, "/v3/refunds", "yookassa/payment-refund-response.json");
    }

    public static void createPayment(WireMockServer mockService) throws IOException {
        IntegrationUtils.createPostMockStub(mockService, "/v3/payments", "yookassa/payment-create-response.json");
    }
}
