package com.nsia.commons.module.httpclient.restclientfactory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.event.Level;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "rest-client-factory")
public class RestClientFactoryProperties
{
    private boolean enable = true;
    private Map<String, RestClientConfig> mapOfRestClientConfig;

    @Getter @Setter
    public static class RestClientConfig
    {
        private String beanName; // This field will be generated. Do not set this value on application.properties.
        private List<String> suitableHosts;

        private Duration connectTimeout = Duration.ofMillis(60000);
        private Duration socketTimeout = Duration.ofMillis(60000); // To configure read timeout.

        private int maxConnTotal = 200;
        private int maxConnPerRoute = 40;

        private boolean disableSslCertificateVerification = false;

        private RestClientProxyConfig proxyConfig = new RestClientProxyConfig();

        private RestClientLoggingInterceptorConfig loggingInterceptorConfig = new RestClientLoggingInterceptorConfig();
        private RestClientMetricLatencyInterceptorConfig metricLatencyInterceptorConfig = new RestClientMetricLatencyInterceptorConfig();
    }

    @Getter @Setter
    public static class RestClientProxyConfig
    {
        private boolean enable = false;
        private String host;
        private int port = 8443;
        private String user;
        private String password;
    }

    @Getter @Setter
    public static class RestClientLoggingInterceptorConfig
    {
        private boolean enable = true;
        private Level defaultLevel = Level.INFO;

        private boolean enableLogRequestMethodAndUri = true;
        private Level enableLogRequestMethodAndUriWithLevel; // If null, the defaultLevel will be used.

        private boolean enableLogRequestHeaders = true;
        private Level enableLogRequestHeadersWithLevel; // If null, the defaultLevel will be used.

        private boolean enableLogRequestBody = true;
        private Level enableLogRequestBodyWithLevel; // If null, the defaultLevel will be used.

        private boolean enableLogResponseStatus = true;
        private Level enableLogResponseStatusWithLevel; // If null, the defaultLevel will be used.

        private boolean enableLogResponseHeaders = true;
        private Level enableLogResponseHeadersWithLevel; // If null, the defaultLevel will be used.

        private boolean enableLogResponseBody = true;
        private Level enableLogResponseBodyWithLevel; // If null, the defaultLevel will be used.
    }

    @Builder
    @Getter @Setter @NoArgsConstructor
    @AllArgsConstructor
    public static class RestClientMetricLatencyInterceptorConfig
    {
        private boolean enable = true;
    }
}
