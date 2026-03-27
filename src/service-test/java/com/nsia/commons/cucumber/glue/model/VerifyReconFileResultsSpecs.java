package com.nsia.commons.cucumber.glue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyReconFileResultsSpecs
{
    @Valid @NotNull @Size(min = 1) private List<String> matchesFileContains;
    @Valid @NotNull @Size(min = 1) private List<String> disputeFileContains;
}
