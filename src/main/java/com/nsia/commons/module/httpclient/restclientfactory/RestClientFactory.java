package com.nsia.commons.module.httpclient.restclientfactory;

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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
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
 * # RestClientFactory Config - defaultRestClient
 * rest-client-factory.map-of-rest-client-config.default.suitable-hosts=*, localhost, .*svc
 * rest-client-factory.map-of-rest-client-config.default.logging-interceptor-config.enable=true
 * rest-client-factory.map-of-rest-client-config.default.logging-interceptor-config.enable-log-request-method-and-uri=true
 * rest-client-factory.map-of-rest-client-config.default.logging-interceptor-config.enable-log-request-headers=true
 * rest-client-factory.map-of-rest-client-config.default.logging-interceptor-config.enable-log-request-body=true
 * rest-client-factory.map-of-rest-client-config.default.logging-interceptor-config.enable-log-response-status=true
 * rest-client-factory.map-of-rest-client-config.default.logging-interceptor-config.enable-log-response-headers=true
 * rest-client-factory.map-of-rest-client-config.default.logging-interceptor-config.enable-log-response-body=true
 * <br/>
 * # RestClientFactory Config - proxyEnabledRestClient
 * rest-client-factory.map-of-rest-client-config.proxyEnabled.suitable-hosts=.*doku.com
 * rest-client-factory.map-of-rest-client-config.proxyEnabled.logging-interceptor-config.enable=true
 * rest-client-factory.map-of-rest-client-config.proxyEnabled.proxy-config.enable=true
 * rest-client-factory.map-of-rest-client-config.proxyEnabled.proxy-config.host=proxy-http.proxy-uat.svc
 * rest-client-factory.map-of-rest-client-config.proxyEnabled.proxy-config.port=8443
 * rest-client-factory.map-of-rest-client-config.proxyEnabled.proxy-config.user=vanessa
 * rest-client-factory.map-of-rest-client-config.proxyEnabled.proxy-config.password=vanessa12!@
 *
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@Configuration
public class RestClientFactory
{
    public static final String ANY_HOST = "*";

    /*
     * Use RestClient.Builder to create RestClient to ensure Micrometer Tracing (formerly Spring Cloud Sleuth) interceptor is added so distributed tracing can run properly.
     *
     * We use ObjectProvider to make sure each call will create a new RestClient.Builder.
     * If we only use 1 RestClient.Builder, interceptors from RestClient created by the RestClient.Builder will be shared to other RestClient created by the same RestClient.Builder,
     * and that is not what we want, because some interceptors are only suitable for certain RestClient.
     */
    private final ObjectProvider<RestClient.Builder> restClientBuilderProvider;
    private final RestClientFactoryProperties restClientFactoryProperties;

    private final Map<String, RestClient> mapOfRestClientPerHost = new HashMap<>();
    private RestClient restClientDefault;
    private final MetricLatency metricLatency;

    private final UriTemplateHandler uriTemplateHandler;

    public RestClientFactory(ConfigurableBeanFactory configurableBeanFactory, ObjectProvider<RestClient.Builder> restClientBuilderProvider, RestClientFactoryProperties restClientFactoryProperties, MetricLatency metricLatency)
    {
        this.restClientBuilderProvider = restClientBuilderProvider;
        this.restClientFactoryProperties = restClientFactoryProperties;
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
        if(!restClientFactoryProperties.isEnable())
        {
            log.warn("RestClientFactory is disabled. Skipping RestClient creation. RestClientFactory will not be usable.");
            return;
        }

        var mapOfRestClientConfig = restClientFactoryProperties.getMapOfRestClientConfig();

        if(mapOfRestClientConfig==null)
        {
            throw new BeanCreationException("Make sure at least 1 RestClientConfig is configured and contains '%s' in their suitableHosts OR set rest-client-factory.enable=false.".formatted(ANY_HOST));
        }

        boolean isDefaultRestClientForAnyHostFound = false;

        for(var it : mapOfRestClientConfig.entrySet())
        {
            var beanName = it.getKey() + "RestClient";

            var restClientConfig = it.getValue();
            restClientConfig.setBeanName(beanName);

            var restClient = buildRestClient(restClientConfig);

            configurableBeanFactory.registerSingleton(beanName, restClient);
            log.info("Bean RestClient with name = '{}' has been registered.", beanName);

            if(restClientConfig.getSuitableHosts()!=null)
            {
                for(String host : restClientConfig.getSuitableHosts())
                {
                    if(ANY_HOST.equals(host))
                    {
                        isDefaultRestClientForAnyHostFound = true;
                        restClientDefault = restClient;
                    }

                    mapOfRestClientPerHost.put(host.toLowerCase(), restClient);
                }
            }
        }

        if(!isDefaultRestClientForAnyHostFound)
        {
            throw new BeanCreationException("Make sure 1 of RestClientConfig contains '%s' in their suitableHosts.".formatted(ANY_HOST));
        }
    }

    private RestClient buildRestClient(RestClientFactoryProperties.RestClientConfig restClientConfig)
    {
        var connectionConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.of(restClientConfig.getConnectTimeout()))
            .setSocketTimeout(Timeout.of(restClientConfig.getSocketTimeout()))
            .build();

        var connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnPerRoute(restClientConfig.getMaxConnPerRoute())
            .setMaxConnTotal(restClientConfig.getMaxConnTotal())
            .setDefaultConnectionConfig(connectionConfig);

        var clientBuilder = HttpClients.custom()
            .useSystemProperties();

        if(restClientConfig.getProxyConfig().isEnable())
        {
            var proxyConfig = restClientConfig.getProxyConfig();
            var credentialsProvider = new BasicCredentialsProvider();

            credentialsProvider.setCredentials(
                new AuthScope(proxyConfig.getHost(), proxyConfig.getPort()),
                new UsernamePasswordCredentials(proxyConfig.getUser(), proxyConfig.getPassword().toCharArray())
            );

            clientBuilder.setProxy(new HttpHost(proxyConfig.getHost(), proxyConfig.getPort()));
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        if(restClientConfig.isDisableSslCertificateVerification())
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
                throw new BeanCreationException("Failed to disable SSL Certificate verification on RestClient with name = '%s'. Cause: %s".formatted(restClientConfig.getBeanName(), ex.getMessage()), ex);
            }
        }

        clientBuilder.setConnectionManager(connectionManagerBuilder.build());

        var httpClient = clientBuilder.build();
        var clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        var bufferingClientHttpRequestFactory = new BufferingClientHttpRequestFactory(clientHttpRequestFactory); // Using this wrapper allows for multiple reads of the response body.

        var listOfInterceptor = new ArrayList<ClientHttpRequestInterceptor>();

        if(restClientConfig.getMetricLatencyInterceptorConfig().isEnable())
        {
            listOfInterceptor.add(new MetricLatencyInterceptor(metricLatency));
        }

        // If RestClientLoggingInterceptor is added after MetricLatencyInterceptor, then the time used to process the logging will be added to latency time, but hopefully the time increment is not too much,
        // BUT the good thing is, the log for Request/Response can be easier to be seen.
        if(restClientConfig.getLoggingInterceptorConfig().isEnable())
        {
            listOfInterceptor.add(new RestClientLoggingInterceptor(restClientConfig.getBeanName(), restClientConfig.getLoggingInterceptorConfig()));
        }

        return restClientBuilderProvider.getObject()
            .requestFactory(bufferingClientHttpRequestFactory)
            .requestInterceptors(it ->
            {
                it.clear(); // We need to clear the list to make sure the interceptors from defaultRestClient are not added to proxyEnabledRestClient.
                it.addAll(listOfInterceptor);
            })
            .build();
    }

    /**
     * We use uriTemplateHandler instead of URI.create(str) to make sure all URL params value is encoded using UrlEncoder.
     * <br/>
     * Old code will not work when urlAsString = https://api-uat.doku.com/doku-virtual-account/v2/inquiry?BANK=ALTO&STEP=INQUIRY&VANUMBER=8000001200000021&TRACENO=70639&DATETIME=2024-07-10 18:09:23
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    public RestClient findByUri(String uriAsString)
    {
        return findByHost(uriTemplateHandler.expand(uriAsString).getHost());
    }

    public RestClient findByHost(String host)
    {
        var hostInLowerCase = Optional.ofNullable(host).map(String::toLowerCase).orElse("");
        var restClient = mapOfRestClientPerHost.get(hostInLowerCase);

        if(restClient==null)
        {
            restClient = mapOfRestClientPerHost.entrySet().stream()
                .filter(it -> !ANY_HOST.equals(it.getKey())) // Remove ANY_HOST from the entry.
                .filter(it -> hostInLowerCase.matches(it.getKey())) // Use regex to match the host.
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(restClientDefault); // If not regex matched, then use the default RestClient.
        }

        return restClient;
    }

    @SuppressWarnings("unused")
    public <T> ResponseEntity<T> postForEntity(String uri, HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) throws RestClientException
    {
        return exchange(uri, HttpMethod.POST, requestEntity, responseType, uriVariables);
    }

    @SuppressWarnings("unused")
    public <T> ResponseEntity<T> getForEntity(String uri, Class<T> responseType, Object... uriVariables) throws RestClientException
    {
        return exchange(uri, HttpMethod.GET, null, responseType, uriVariables);
    }

    @SuppressWarnings("unused")
    public <T> ResponseEntity<T> exchange(String uri, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) throws RestClientException
    {
        var requestBodySpec = findByUri(uri)
            .method(method)
            .uri(uri, uriVariables);

        if(requestEntity != null)
        {
            Optional.of(requestEntity.getHeaders())
                .filter(it -> !it.isEmpty())
                .ifPresent(it ->
                    requestBodySpec.headers(headersConsumer -> headersConsumer.addAll(it))
                );

            Optional.ofNullable(requestEntity.getBody())
                .ifPresent(requestBodySpec::body);
        }

        return requestBodySpec
            .retrieve()
            .toEntity(responseType);
    }
}
