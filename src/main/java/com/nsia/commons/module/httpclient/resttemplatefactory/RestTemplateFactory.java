package com.nsia.commons.module.httpclient.resttemplatefactory;

import com.nsia.commons.module.httpclient.common.MetricLatency;
import com.nsia.commons.module.httpclient.common.MetricLatencyInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Sample Config:
 * <br/>
 * # RestTemplateFactory Config - defaultRestTemplate
 * rest-template-factory.map-of-rest-template-config.default.suitable-hosts=*, localhost, .*svc
 * rest-template-factory.map-of-rest-template-config.default.logging-interceptor-config.enable=true
 * rest-template-factory.map-of-rest-template-config.default.logging-interceptor-config.enable-log-request-method-and-uri=true
 * rest-template-factory.map-of-rest-template-config.default.logging-interceptor-config.enable-log-request-headers=true
 * rest-template-factory.map-of-rest-template-config.default.logging-interceptor-config.enable-log-request-body=true
 * rest-template-factory.map-of-rest-template-config.default.logging-interceptor-config.enable-log-response-status=true
 * rest-template-factory.map-of-rest-template-config.default.logging-interceptor-config.enable-log-response-headers=true
 * rest-template-factory.map-of-rest-template-config.default.logging-interceptor-config.enable-log-response-body=true
 * <br/>
 * # RestTemplateFactory Config - proxyEnabledRestTemplate
 * rest-template-factory.map-of-rest-template-config.proxyEnabled.suitable-hosts=.*doku.com
 * rest-template-factory.map-of-rest-template-config.proxyEnabled.logging-interceptor-config.enable=true
 * rest-template-factory.map-of-rest-template-config.proxyEnabled.proxy-config.enable=true
 * rest-template-factory.map-of-rest-template-config.proxyEnabled.proxy-config.host=proxy-http.proxy-uat.svc
 * rest-template-factory.map-of-rest-template-config.proxyEnabled.proxy-config.port=8443
 * rest-template-factory.map-of-rest-template-config.proxyEnabled.proxy-config.user=vanessa
 * rest-template-factory.map-of-rest-template-config.proxyEnabled.proxy-config.password=vanessa12!@
 *
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@Configuration
public class RestTemplateFactory
{
    public static final String ANY_HOST = "*";

    private final RestTemplateBuilder restTemplateBuilder; // Use this class to create RestTemplate to ensure Micrometer Tracing (formerly Spring Cloud Sleuth) interceptor is added so distributed tracing can run properly.
    private final RestTemplateFactoryProperties restTemplateFactoryProperties;

    private final Map<String, RestTemplate> mapOfRestTemplatePerHost = new HashMap<>();
    private RestTemplate restTemplateDefault;
    private final MetricLatency metricLatency;

    private final UriTemplateHandler uriTemplateHandler;

    public RestTemplateFactory(ConfigurableBeanFactory configurableBeanFactory, RestTemplateBuilder restTemplateBuilder, RestTemplateFactoryProperties restTemplateFactoryProperties, MetricLatency metricLatency)
    {
        this.restTemplateBuilder = restTemplateBuilder;
        this.restTemplateFactoryProperties = restTemplateFactoryProperties;
        this.metricLatency = metricLatency;
        this.uriTemplateHandler = initUriTemplateHandler();
        createAndRegisterBean(configurableBeanFactory);
    }

    private DefaultUriBuilderFactory initUriTemplateHandler()
    {
        var uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);  // For backwards compatibility.
        return uriFactory;
    }

    private void createAndRegisterBean(ConfigurableBeanFactory configurableBeanFactory) throws BeansException
    {
        if(!restTemplateFactoryProperties.isEnable())
        {
            log.warn("RestTemplateFactory is disabled. Skipping rest template creation. RestTemplateFactory will not be usable.");
            return;
        }

        var mapOfRestTemplateConfig = restTemplateFactoryProperties.getMapOfRestTemplateConfig();

        if(mapOfRestTemplateConfig==null)
        {
            throw new BeanCreationException("Make sure at least 1 RestTemplateConfig is configured and contains '%s' in their suitableHosts OR set rest-template-factory.enable=false.".formatted(ANY_HOST));
        }

        boolean isDefaultRestTemplateForAnyHostFound = false;

        for(var it : mapOfRestTemplateConfig.entrySet())
        {
            var beanName = it.getKey() + "RestTemplate";

            var restTemplateConfig = it.getValue();
            restTemplateConfig.setBeanName(beanName);

            var restTemplate = buildRestTemplate(restTemplateConfig);

            configurableBeanFactory.registerSingleton(beanName, restTemplate);
            log.info("Bean RestTemplate with name = '{}' has been registered.", beanName);

            if(restTemplateConfig.getSuitableHosts()!=null)
            {
                for(String host : restTemplateConfig.getSuitableHosts())
                {
                    if(ANY_HOST.equals(host))
                    {
                        isDefaultRestTemplateForAnyHostFound = true;
                        restTemplateDefault = restTemplate;
                    }

                    mapOfRestTemplatePerHost.put(host.toLowerCase(), restTemplate);
                }
            }
        }

        if(!isDefaultRestTemplateForAnyHostFound)
        {
            throw new BeanCreationException("Make sure 1 of RestTemplateConfig contains '%s' in their suitableHosts.".formatted(ANY_HOST));
        }
    }

    private RestTemplate buildRestTemplate(RestTemplateFactoryProperties.RestTemplateConfig restTemplateConfig)
    {
        var connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.of(restTemplateConfig.getConnectTimeout()))
            .setSocketTimeout(Timeout.of(restTemplateConfig.getSocketTimeout()))
            .build();

        var connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnPerRoute(restTemplateConfig.getMaxConnPerRoute())
            .setMaxConnTotal(restTemplateConfig.getMaxConnTotal())
            .setDefaultConnectionConfig(connectionConfig);

        var clientBuilder = HttpClients.custom()
            .useSystemProperties();

        if(restTemplateConfig.getProxyConfig().isEnable())
        {
            var proxyConfig = restTemplateConfig.getProxyConfig();
            var credentialsProvider = new BasicCredentialsProvider();

            credentialsProvider.setCredentials(
                new AuthScope(proxyConfig.getHost(), proxyConfig.getPort()),
                new UsernamePasswordCredentials(proxyConfig.getUser(), proxyConfig.getPassword().toCharArray())
            );

            clientBuilder.setProxy(new HttpHost(proxyConfig.getHost(), proxyConfig.getPort()));
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        if(restTemplateConfig.isDisableSslCertificateVerification())
        {
            try
            {
                var acceptingTrustStrategy = (TrustStrategy) (X509Certificate[] chain, String authType) -> true;
                var sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();

                var cts = new DefaultClientTlsStrategy(sslContext, new NoopHostnameVerifier());
                connectionManagerBuilder.setTlsSocketStrategy(cts);
            }
            catch(NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex)
            {
                throw new BeanCreationException("Failed to disable SSL Certificate verification on RestTemplate with name = '%s'. Cause: %s".formatted(restTemplateConfig.getBeanName(), ex.getMessage()), ex);
            }
        }

        clientBuilder.setConnectionManager(connectionManagerBuilder.build());

        var httpClient = clientBuilder.build();
        var clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        var bufferingClientHttpRequestFactory = new BufferingClientHttpRequestFactory(clientHttpRequestFactory); // Using this wrapper allows for multiple reads of the response body.

        var listOfInterceptor = new ArrayList<ClientHttpRequestInterceptor>();

        if(restTemplateConfig.getMetricLatencyInterceptorConfig().isEnable())
        {
            listOfInterceptor.add(new MetricLatencyInterceptor(metricLatency));
        }

        // If RestTemplateLoggingInterceptor is added after MetricLatencyInterceptor, then the time used to process the logging will be added to latency time, but hopefully the time increment is not too much,
        // BUT the good thing is, the log for Request/Response can be easier to be seen.
        if(restTemplateConfig.getLoggingInterceptorConfig().isEnable())
        {
            listOfInterceptor.add(new RestTemplateLoggingInterceptor(restTemplateConfig.getBeanName(), restTemplateConfig.getLoggingInterceptorConfig()));
        }

        return restTemplateBuilder
            .requestFactory(() -> bufferingClientHttpRequestFactory)
            .interceptors(listOfInterceptor)
            .build();
    }

    /**
     * We use uriTemplateHandler instead of URI.create(str) to make sure all URL params value is encoded using UrlEncoder.
     * <br/>
     * Old code will not work when urlAsString = https://api-uat.doku.com/doku-virtual-account/v2/inquiry?BANK=ALTO&STEP=INQUIRY&VANUMBER=8000001200000021&TRACENO=70639&DATETIME=2024-07-10 18:09:23
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public RestTemplate findByUrl(String urlAsString)
    {
        return findByHost(uriTemplateHandler.expand(urlAsString).getHost());
    }

    public RestTemplate findByHost(String host)
    {
        var hostInLowerCase = Optional.ofNullable(host).map(String::toLowerCase).orElse("");
        var restTemplate = mapOfRestTemplatePerHost.get(hostInLowerCase);

        if(restTemplate==null)
        {
            restTemplate = mapOfRestTemplatePerHost.entrySet().stream()
                .filter(it -> !ANY_HOST.equals(it.getKey())) // Remove ANY_HOST from the entry.
                .filter(it -> hostInLowerCase.matches(it.getKey())) // Use regex to match the host.
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(restTemplateDefault); // If not regex matched, then use the default RestTemplate.
        }

        return restTemplate;
    }

    @SuppressWarnings("unused")
    public <T> ResponseEntity<T> postForEntity(String url, @Nullable Object request, Class<T> responseType, Object... uriVariables) throws RestClientException
    {
        var restTemplate = findByUrl(url);
        return restTemplate.postForEntity(url, request, responseType, uriVariables);
    }

    @SuppressWarnings("unused")
    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables) throws RestClientException
    {
        var restTemplate = findByUrl(url);
        return restTemplate.getForEntity(url, responseType, uriVariables);
    }

    @SuppressWarnings("unused")
    public <T> ResponseEntity<T> exchange(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) throws RestClientException
    {
        var restTemplate = findByUrl(url);
        return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }
}
