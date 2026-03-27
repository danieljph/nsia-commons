package com.nsia.commons.module.httpclient.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StopWatch;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class MetricLatencyInterceptor implements ClientHttpRequestInterceptor
{
    private final MetricLatency metricLatency;

    @SuppressWarnings({"NullableProblems", "DuplicatedCode"})
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
    {
        log.debug("Metric Latency Start");
        ClientHttpResponse response = null;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try
        {
            response = execution.execute(request, body);
        }
        finally
        {
            stopWatch.stop();

            if(response!=null)
            {
                metricLatency.publishHttpStatus(request.getURI().getHost(), request.getURI().getPath(),response.getStatusCode().value());
            }

            metricLatency.publishHttpLatency(request.getURI().getHost(), request.getURI().getPath(), stopWatch.getTotalTimeMillis());
            log.debug("Metric Latency Done");
        }

        return response;
    }
}
