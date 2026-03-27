package com.nsia.commons.module.httpclient.resttemplatefactory;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor
{
    private final String restTemplateName;
    private final RestTemplateFactoryProperties.RestTemplateLoggingInterceptorConfig restTemplateLoggingInterceptorConfig;

    public RestTemplateLoggingInterceptor(String restTemplateName, RestTemplateFactoryProperties.RestTemplateLoggingInterceptorConfig restTemplateLoggingInterceptorConfig)
    {
        this.restTemplateName = restTemplateName;
        this.restTemplateLoggingInterceptorConfig = restTemplateLoggingInterceptorConfig;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
    {
        doLog(restTemplateLoggingInterceptorConfig.getDefaultLevel(), "================== CLIENT HTTP REQUEST START ({}) ==================", restTemplateName);

        if(restTemplateLoggingInterceptorConfig.isEnableLogRequestMethodAndUri())
        {
            doLog(restTemplateLoggingInterceptorConfig.getEnableLogRequestMethodAndUriWithLevel(), "Request         : {} {}", request.getMethod(), request.getURI());
        }

        if(restTemplateLoggingInterceptorConfig.isEnableLogRequestHeaders())
        {
            doLog(restTemplateLoggingInterceptorConfig.getEnableLogRequestHeadersWithLevel(), "Request Header  : {}", request.getHeaders());
        }

        if(restTemplateLoggingInterceptorConfig.isEnableLogRequestBody())
        {
            doLog(restTemplateLoggingInterceptorConfig.getEnableLogRequestBodyWithLevel(), "Request Body    : {}", new String(body, StandardCharsets.UTF_8));
        }

        try
        {
            var response = execution.execute(request, body);

            if(restTemplateLoggingInterceptorConfig.isEnableLogResponseStatus())
            {
                doLog(restTemplateLoggingInterceptorConfig.getEnableLogResponseStatusWithLevel(), "Response Status : {} - {}", response.getStatusCode(), response.getStatusText());
            }

            if(restTemplateLoggingInterceptorConfig.isEnableLogResponseHeaders())
            {
                doLog(restTemplateLoggingInterceptorConfig.getEnableLogResponseHeadersWithLevel(), "Response Header : {}", response.getHeaders());
            }

            if(restTemplateLoggingInterceptorConfig.isEnableLogResponseBody())
            {
                doLog(restTemplateLoggingInterceptorConfig.getEnableLogResponseBodyWithLevel(), "Response Body   : {}", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
            }

            return response;
        }
        finally
        {
            doLog(restTemplateLoggingInterceptorConfig.getDefaultLevel(), "------------------ CLIENT HTTP REQUEST FINISH ({}) -----------------", restTemplateName);
        }
    }

    public void doLog(Level level, String format, Object... arguments)
    {
        level = Optional.ofNullable(level)
            .orElse(
                Optional.ofNullable(restTemplateLoggingInterceptorConfig.getDefaultLevel())
                    .orElse(Level.DEBUG)
            );

        switch(level)
        {
            case ERROR -> log.error(format, arguments);
            case WARN -> log.warn(format, arguments);
            case INFO -> log.info(format, arguments);
            case DEBUG -> log.debug(format, arguments);
            case TRACE -> log.trace(format, arguments);
        }
    }
}
