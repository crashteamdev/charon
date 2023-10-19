package dev.crashteam.charon.util;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import dev.crashteam.charon.YookassaIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static java.nio.charset.Charset.defaultCharset;
import static org.springframework.util.StreamUtils.copyToString;

public class IntegrationUtils {

    public static void createPostMockStub(WireMockServer mockService, String url, String jsonPath) throws IOException {
        mockService.stubFor(WireMock.post(WireMock.urlEqualTo(url))
                .withHeader("Content-Type", containing(MediaType.APPLICATION_JSON_VALUE))
                .willReturn(
                        WireMock.aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(
                                        copyToString(
                                                IntegrationUtils.class
                                                        .getClassLoader()
                                                        .getResourceAsStream(jsonPath),
                                                defaultCharset())
                                )
                )
        );
    }

    public static void createGetMockStub(WireMockServer mockService,
                                         Map<String, StringValuePattern> queryParams,
                                         String urlPath,
                                         String jsonPath) throws IOException {
        mockService.stubFor(WireMock.get(WireMock.urlPathEqualTo(urlPath))
                .withQueryParams(queryParams)
                .willReturn(
                        WireMock.aResponse()
                                .withStatus(HttpStatus.OK.value())
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(
                                        copyToString(
                                                IntegrationUtils.class
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
                                                IntegrationUtils.class
                                                        .getClassLoader()
                                                        .getResourceAsStream(jsonPath),
                                                defaultCharset())
                                )
                )
        );
    }
}
