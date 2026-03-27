package com.nsia.commons.module.commonsnap.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@SuperBuilder @Getter @Setter @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapBaseResponse
{
    private String responseCode;
    private String responseMessage;
}
