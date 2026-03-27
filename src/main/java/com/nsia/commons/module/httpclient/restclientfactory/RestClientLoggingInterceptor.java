package com.nsia.commons.module.httpclient.restclientfactory;

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
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor
{
    private final String restClientName;
    private final RestClientFactoryProperties.RestClientLoggingInterceptorConfig restClientLoggingInterceptorConfig;

    public RestClientLoggingInterceptor(String restClientName, RestClientFactoryProperties.RestClientLoggingInterceptorConfig restClientLoggingInterceptorConfig)
    {
        this.restClientName = restClientName;
        this.restClientLoggingInterceptorConfig = restClientLoggingInterceptorConfig;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
    {
        doLog(restClientLoggingInterceptorConfig.getDefaultLevel(), "================== CLIENT HTTP REQUEST START ({}) ==================", restClientName);

        if(restClientLoggingInterceptorConfig.isEnableLogRequestMethodAndUri())
        {
            doLog(restClientLoggingInterceptorConfig.getEnableLogRequestMethodAndUriWithLevel(), "Request         : {} {}", request.getMethod(), request.getURI());
        }

        if(restClientLoggingInterceptorConfig.isEnableLogRequestHeaders())
        {
            doLog(restClientLoggingInterceptorConfig.getEnableLogRequestHeadersWithLevel(), "Request Header  : {}", request.getHeaders());
        }

        if(restClientLoggingInterceptorConfig.isEnableLogRequestBody())
        {
            doLog(restClientLoggingInterceptorConfig.getEnableLogRequestBodyWithLevel(), "Request Body    : {}", new String(body, StandardCharsets.UTF_8));
        }

        try
        {
            var response = execution.execute(request, body);

            if(restClientLoggingInterceptorConfig.isEnableLogResponseStatus())
            {
                doLog(restClientLoggingInterceptorConfig.getEnableLogResponseStatusWithLevel(), "Response Status : {} - {}", response.getStatusCode(), response.getStatusText());
            }

            if(restClientLoggingInterceptorConfig.isEnableLogResponseHeaders())
            {
                doLog(restClientLoggingInterceptorConfig.getEnableLogResponseHeadersWithLevel(), "Response Header : {}", response.getHeaders());
            }

            if(restClientLoggingInterceptorConfig.isEnableLogResponseBody())
            {
                doLog(restClientLoggingInterceptorConfig.getEnableLogResponseBodyWithLevel(), "Response Body   : {}", new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
            }

            return response;
        }
        finally
        {
            doLog(restClientLoggingInterceptorConfig.getDefaultLevel(), "------------------ CLIENT HTTP REQUEST FINISH ({}) -----------------", restClientName);
        }
    }

    public void doLog(Level level, String format, Object... arguments)
    {
        level = Optional.ofNullable(level)
            .orElse(
                Optional.ofNullable(restClientLoggingInterceptorConfig.getDefaultLevel())
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
