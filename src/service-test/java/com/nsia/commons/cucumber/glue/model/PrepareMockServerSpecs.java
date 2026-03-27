package com.nsia.commons.cucumber.glue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nsia.commons.cucumber.glue.ScenarioManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.mockserver.model.Headers;
import org.springframework.util.LinkedMultiValueMap;

import java.time.Duration;
import java.util.List;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrepareMockServerSpecs
{
    @Valid @NotNull private List<MockServerCriteria> listOfCriteria;

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MockServerCriteria
    {
        @NotBlank private String requestPath;
        @NotBlank private String requestMethod;
        private String requestBodyMatchedWithRegex;

        private Duration responseDelay;
        private int responseStatusCode = 200;
        private LinkedMultiValueMap<String, String> responseHeaders;
        private String responseBodyRaw; // This field will be prioritized over responseBodyFile.
        private String responseBodyFile; // Will be use if responseBody is blank.

        public Headers getResponseHeadersAsMockServerHeaders()
        {
            var headers = new Headers();

            if(ObjectUtils.isNotEmpty(responseHeaders))
            {
                responseHeaders.forEach(headers::withEntry);
            }

            return headers;
        }

        public String getResponseBody(ScenarioManager scenarioManager)
        {
            return StringUtils.isBlank(responseBodyRaw)
                ? scenarioManager.getScenarioTestDataContent(responseBodyFile)
                : scenarioManager.replaceTokens(responseBodyRaw);
        }
    }
}
