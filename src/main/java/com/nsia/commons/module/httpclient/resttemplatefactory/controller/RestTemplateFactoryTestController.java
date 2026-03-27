package com.nsia.commons.module.httpclient.resttemplatefactory.controller;

import com.nsia.commons.module.httpclient.resttemplatefactory.RestTemplateFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/rest-template-factory-test")
public class RestTemplateFactoryTestController
{
    private final RestTemplateFactory restTemplateFactory;

    @PostMapping(value = "/api1", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Api1Response> api1(@RequestBody Api1Request request)
    {
        var api2Uri = "http://localhost:8080/nsia-commons/rest-template-factory-test/api2";

        var api2RequestHeaders = new HttpHeaders();
        api2RequestHeaders.add("X-Request-ID", request.getId());

        var api2Request = new Api2Request(request.getId());

        var api2RequestHttpEntity = new HttpEntity<>(api2Request, api2RequestHeaders);

        var responseApi2 = restTemplateFactory.postForEntity(api2Uri, api2RequestHttpEntity, Api2Response.class);

        return ResponseEntity
            .ok(Api1Response.builder()
                .id(request.getId())
                .message(
                    Optional.ofNullable(responseApi2.getBody())
                        .map(Api2Response::getMessage)
                        .orElse(null)
                )
                .build()
            );
    }

    @PostMapping(value = "/api2", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Api2Response> api2(@RequestBody Api2Request request)
    {
        try
        {
            Thread.sleep(500);
        }
        catch(Exception ignored)
        {
        }

        return ResponseEntity
            .ok(Api2Response.builder()
                .id(request.getId())
                .message("Response from API-2. Response-ID: %s".formatted(System.currentTimeMillis()))
                .build()
            );
    }

    @Builder @Setter @Getter @NoArgsConstructor @AllArgsConstructor
    public static class Api1Request
    {
        private String id;
    }

    @Builder @Setter @Getter @NoArgsConstructor @AllArgsConstructor
    public static class Api1Response
    {
        private String id;
        private String message;
    }

    @Builder @Setter @Getter @NoArgsConstructor @AllArgsConstructor
    public static class Api2Request
    {
        private String id;
    }

    @Builder @Setter @Getter @NoArgsConstructor @AllArgsConstructor
    public static class Api2Response
    {
        private String id;
        private String message;
    }
}
