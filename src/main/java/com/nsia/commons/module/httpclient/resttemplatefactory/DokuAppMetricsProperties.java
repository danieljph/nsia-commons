package com.nsia.commons.module.httpclient.resttemplatefactory;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "doku.app-metrics")
public class DokuAppMetricsProperties
{
    private Metrics httpLatency = new Metrics();
    private Metrics httpStatus = new Metrics();
    private Metrics transactionStatus = new Metrics();

    @Getter @Setter
    public static class Metrics
    {
        private boolean enabled;
        private List<String> enabledFor;
    }
}
