package dev.crashteam.charon.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.crashteam.charon.YookassaIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static java.nio.charset.Charset.defaultCharset;
import static org.springframework.util.StreamUtils.copyToString;

public class YookassaMock {

    public static void paymentStatus(WireMockServer mockService) throws IOException {
        createGetMockStub(mockService, "/v3/payments/22e12f66-000f-5000-8000-18db351245c7",
                "yookassa/payment-status-response.json");
    }

    public static void createRefundPayment(WireMockServer mockService) throws IOException {
        createPostMockStub(mockService, "/v3/refunds", "yookassa/payment-refund-response.json");
    }

    public static void createPayment(WireMockServer mockService) throws IOException {
        createPostMockStub(mockService, "/v3/payments", "yookassa/payment-create-response.json");
    }

    public static void createPostMockStub(WireMockServer mockService, String url, String jsonPath) throws IOException {
        mockService.stubFor(WireMock.post(WireMock.urlEqualTo(url))
                .withHeader("Content-Type", containing(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                        WireMock.aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(
                                        copyToString(
                                                YookassaIntegrationTest.class
                                                        .getClassLoader()
                                                        .getResourceAsStream(jsonPath),
                                                defaultCharset())
                                )
                )
        );
    }

    public static void createGetMockStub(WireMockServer mockService, String url, String jsonPath) throws IOException {
        mockService.stubFor(WireMock.get(WireMock.urlEqualTo(url))
                .willReturn(
                        WireMock.aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(
                                        copyToString(
                                                YookassaIntegrationTest.class
                                                        .getClassLoader()
                                                        .getResourceAsStream(jsonPath),
                                                defaultCharset())
                                )
                )
        );
    }
}
