package com.nsia.commons.cucumber.glue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HitApiSpecs
{
    @NotNull private String method;
    @NotNull private String relativeRef; // URI without schema://domain:port. Read https://www.rfc-editor.org/rfc/rfc3986#section-4.2
    private LinkedMultiValueMap<String, String> headers;
    private String payloadRaw; // This field will be prioritized over payloadFile.
    private String payloadFile; // Will be use if payloadRaw is blank.
    private LinkedMultiValueMap<String, String> payloadFormData; // Will be used for Content-Type = multipart/form-data.
    @Min(1) private int numberOfConcurrentRequests = 1;
    private GenerateSignature generateSignature;

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GenerateSignature
    {
        private boolean enabled = false;
        private String secretKey;
    }

    public boolean isMultipartFormDataOrApplicationFormUrlEncoded()
    {
        var contentType = headers.getFirst("Content-Type");
        return MediaType.MULTIPART_FORM_DATA_VALUE.equalsIgnoreCase(contentType) || MediaType.APPLICATION_FORM_URLENCODED_VALUE.equalsIgnoreCase(contentType);
    }

    public boolean isGenerateSignatureEnabled()
    {
        return generateSignature != null && generateSignature.isEnabled();
    }
}
