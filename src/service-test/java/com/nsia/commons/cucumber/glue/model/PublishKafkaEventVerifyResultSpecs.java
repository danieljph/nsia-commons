package com.nsia.commons.cucumber.glue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishKafkaEventVerifyResultSpecs
{
    @Valid private List<VerifyDbCriteria> listOfVerifyDbCriteria;

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifyDbCriteria
    {
        private String id;
        @NotBlank private String query;
        private int count;
        private String queryIfAssertionError;
    }
}
