package com.nsia.commons.cucumber.glue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nsia.commons.cucumber.glue.ScenarioManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HitApiVerifyResultSpecs
{
    @Valid @NotNull @Size(min = 1) private List<HitApiVerifyResultCriteria> listOfCriteria;

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HitApiVerifyResultCriteria
    {
        @NotBlank private String id;
        private int statusCode = 200;
        @NotBlank private String contentType;
        private List<String> responseBodyContains;
        private String responseBodySchemaRaw; // This field will be prioritized over the responseBodySchemaFile.
        private String responseBodySchemaFile; // Will be use if responseBodySchemaRaw is blank.

        @Valid private List<VerifyDbCriteria> listOfVerifyDbCriteria;

        public boolean isResponseEntityMatched(ResponseEntity<String> responseEntity)
        {
            return
                statusCode == responseEntity.getStatusCode().value()
                && MediaType.valueOf(getContentType()).isCompatibleWith(responseEntity.getHeaders().getContentType())
                && (
                    ObjectUtils.isEmpty(responseBodyContains)
                    || responseBodyContains.stream().allMatch(it -> Optional.ofNullable(responseEntity.getBody()).orElse("").contains(it))
                );
        }

        public String getResponseBodySchema(ScenarioManager scenarioManager)
        {
            return StringUtils.isBlank(getResponseBodySchemaRaw())
                ? scenarioManager.getScenarioTestDataContent(getResponseBodySchemaFile())
                : scenarioManager.replaceTokens(getResponseBodySchemaRaw());
        }
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifyDbCriteria
    {
        @NotBlank private String query;
        private int count;
        private String queryIfAssertionError;
    }
}
