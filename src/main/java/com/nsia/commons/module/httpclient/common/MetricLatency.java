package com.nsia.commons.module.httpclient.common;

import com.nsia.commons.module.httpclient.resttemplatefactory.DokuAppMetricsProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Copied from va-core-system.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MetricLatency
{

    private final DokuAppMetricsProperties metricsProperties;
    private final MeterRegistry meterRegistry;

    private final ConcurrentHashMap<String, Timer> recordTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> recordCounter = new ConcurrentHashMap<>();

    private Timer getTimer(String hostName, String servicePath)
    {
        var key = hostName + "." + servicePath;
        log.debug("Publish timer HTTP latency [{}]", key);

        return recordTimers.computeIfAbsent(
            key,
            it ->
                Timer.builder("http_client_request_latency")
                    .tags("host", hostName, "path", servicePath)
                    .publishPercentiles(0.95, 0.99, 0.999)
                    .publishPercentileHistogram()
                    .register(meterRegistry)
        );
    }

    private void setTimerValue(String hostName, String servicePath, long elapsedTime)
    {
        var timer = getTimer(hostName, servicePath);
        timer.record(elapsedTime, TimeUnit.MILLISECONDS);
    }

    public void publishHttpLatency(String hostName, String servicePath, long elapsedTime)
    {
        var isMetricEnabled = metricsProperties.getHttpLatency().isEnabled();

        log.debug("Metric HTTP Latency is {}.", isMetricEnabled? "enabled" : "disabled");

        if(!isMetricEnabled)
        {
            return;
        }

        // If empty that means produce matrix for all outbound requests.
        if(ObjectUtils.isEmpty(metricsProperties.getHttpLatency().getEnabledFor()))
        {
            setTimerValue(hostName, servicePath, elapsedTime);
            return;
        }

        // Create latency for specific host.
        var isMetricEnabledForSpecificHost = metricsProperties.getHttpLatency()
            .getEnabledFor()
            .stream()
            .anyMatch(h -> h.equals(hostName));

        if(isMetricEnabledForSpecificHost)
        {
            setTimerValue(hostName, servicePath, elapsedTime);
        }
    }

    private void setCounterHttpStatus(String hostName, String servicePath, int status)
    {
        var key = hostName + "." + servicePath + ".http." + status;
        log.debug("publish counter http status code [{}]", key);

        recordCounter.computeIfAbsent(key,
                k -> Counter.builder("http_client_request_status")
                        .tags("host", hostName,
                                "uri", servicePath,
                                "outcome", Optional.of(status)
                                        .filter(s -> s != 0)
                                        .map(s -> HttpStatus.valueOf(status).series().name())
                                        .orElse("UNKNOWN"),
                                "status", String.valueOf(status))
                        .register(meterRegistry));
        recordCounter.get(key).increment();
    }

    /**
     * publish metrics http status code from external
     *
     * @param hostName    the dist hostname
     * @param servicePath the service path
     * @param status      the http status code
     */
    public void publishHttpStatus(String hostName, String servicePath, int status)
    {
        if (!metricsProperties.getHttpStatus().isEnabled()) {
            return;
        }

        // if empty that means produce matrix for all outbound request
        if (ObjectUtils.isEmpty(metricsProperties.getHttpStatus().getEnabledFor())) {
            setCounterHttpStatus(hostName, servicePath, status);
            return;
        }

        // create latency for specific host..
        if (metricsProperties.getHttpStatus().getEnabledFor().stream().anyMatch(h -> h.equals(hostName))) {
            setCounterHttpStatus(hostName, servicePath, status);
        }
    }

    private void setCounterTransactionStatus(String channelName, String statusName)
    {
        var key = channelName + ".transaction.status." + statusName;
        log.debug("publish counter transaction status code [{}]", key);
        recordCounter.computeIfAbsent(key,
                k -> Counter.builder("transaction_status")
                        .tags("channel", channelName,
                                "status", statusName)
                        .register(meterRegistry));
        recordCounter.get(key).increment();
    }
}
