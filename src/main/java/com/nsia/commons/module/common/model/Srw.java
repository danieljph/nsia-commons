package com.nsia.commons.module.common.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

import java.util.Optional;

/**
 * Srw class is stand for Service Response Wrapper.
 * We use this abbreviation so the code is not got too long.
 * This class is needed to wrap:
 * - pure responseBody
 * - HttpStatus
 * - HTTP Response Headers
 * - Any other fields that can be used by the endpoint.
 * <br/>
 * By having this wrapper class, we don't make the pure responseBody dirty by adding additional fields
 * that are not related to the responseBody (e.g.: @JsonIgnore private int httpStatusCode).
 *
 * @author Daniel Joi Partogi Hutapea
 */
@Builder @Getter @Setter
public class Srw<B>
{
    private B body; // Body can be null for HTTP-Method = GET.
    @NonNull private HttpStatus httpStatus;
    private HttpHeaders httpHeaders;

    @SuppressWarnings("unused")
    public static class SrwBuilder<B>
    {
        public SrwBuilder<B> httpStatus(HttpStatus httpStatus)
        {
            this.httpStatus = httpStatus;
            return this;
        }

        public SrwBuilder<B> httpStatus(int httpStatusCode)
        {
            this.httpStatus = Optional.ofNullable(HttpStatus.resolve(httpStatusCode)).orElse(HttpStatus.INTERNAL_SERVER_ERROR);
            return this;
        }
    }
}
