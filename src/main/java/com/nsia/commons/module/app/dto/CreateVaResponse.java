package com.nsia.commons.module.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nsia.commons.module.commonsnap.dto.SnapBaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@SuperBuilder @Getter @Setter @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateVaResponse extends SnapBaseResponse
{
    private VirtualAccountData virtualAccountData;

    @Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VirtualAccountData
    {
        @NotBlank @Size(max = 64) private String invoiceNumber;
        @Size(max = 28) private String virtualAccountNo;

        @NotBlank @Size(max = 255) private String virtualAccountName;
        @Size(max = 255) @Email private String virtualAccountEmail;
        @Size(max = 30) private String virtualAccountPhone;

        @Valid @NotNull private Amount amount;

        private Map<String, Object> additionalInfo;
    }
}
