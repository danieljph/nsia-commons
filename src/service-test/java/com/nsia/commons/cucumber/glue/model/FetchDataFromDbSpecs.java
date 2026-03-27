package com.nsia.commons.cucumber.glue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchDataFromDbSpecs
{
    @Valid @NotNull @Size(min = 1) private List<FetchDataFromDbCriteria> listOfCriteria;

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FetchDataFromDbCriteria
    {
        @NotBlank private String id;
        @NotBlank private String query;

        /*
         * key: Database table column name.
         * value: Token name. This token can be used later by using ${tokenName}.
         */
        private Map<String, String> queryResultMapping;
    }
}
