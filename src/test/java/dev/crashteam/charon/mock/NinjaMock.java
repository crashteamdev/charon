package dev.crashteam.charon.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import dev.crashteam.charon.util.IntegrationUtils;

import java.io.IOException;
import java.util.Map;

public class NinjaMock {

    public static void currencyResponse(WireMockServer mockService, Map<String, StringValuePattern> queryParams) throws IOException {
        IntegrationUtils.createGetMockStub(mockService, queryParams, "/v1/convertcurrency",
                "ninja/convert-currency-response.json");
    }
}
