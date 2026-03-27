package com.nsia.commons.module.app.controller;

import com.nsia.commons.module.app.dto.CreateVaRequest;
import com.nsia.commons.module.app.dto.CreateVaResponse;
import com.nsia.commons.module.app.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/register")
public class RegisterController
{
    private final RegisterService registerService;

    @SuppressWarnings("UastIncorrectHttpHeaderInspection")
    @PostMapping(value = "/transfer-va/create-va")
    public ResponseEntity<CreateVaResponse> createVa
    (
        @RequestHeader("X-PARTNER-ID") String xPartnerId,
        @Validated @RequestBody CreateVaRequest request
    )
    {
        var srw = registerService.createVa(xPartnerId, request);

        return ResponseEntity
            .status(srw.getHttpStatus())
            .headers(srw.getHttpHeaders())
            .body(srw.getBody());
    }
}
