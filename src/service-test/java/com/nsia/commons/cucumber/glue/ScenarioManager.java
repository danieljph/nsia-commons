package com.nsia.commons.cucumber.glue;

import com.doku.au.security.module.http.Constant;
import com.doku.au.security.module.http.SignatureComponentDTO;
import com.doku.au.security.module.http.SignatureHeaderService;
import com.nsia.commons.cucumber.glue.exception.TestAppRuntimeException;
import com.nsia.commons.cucumber.glue.model.HitApiSpecs;
import com.nsia.commons.cucumber.glue.model.VerifyKafkaEventPublishedSpecs;
import com.nsia.commons.cucumber.glue.support.TestAppBeanUtils;
import com.nsia.commons.cucumber.glue.text.lookup.AppStringLookupFactory;
import io.cucumber.java.Scenario;
import io.cucumber.spring.CucumberTestContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@Getter @Setter
@Scope(CucumberTestContext.SCOPE_CUCUMBER_GLUE)
@Component
public class ScenarioManager
{
    public static final String INIT_FOLDER_NAME = "01-Init";

    public static final String SHARED_VARS_ZDT_NOW = "SHARED_VARS_ZDT_NOW";

    @Setter(AccessLevel.NONE) private Scenario scenario;
    @Setter(AccessLevel.NONE) private String featureName;
    @Setter(AccessLevel.NONE) private String featureFolderPath;
    @Setter(AccessLevel.NONE) private String scenarioTestDataFolderPath;

    @Value("${server.servlet.context-path}") private String contextPath;
    @LocalServerPort private int port;

    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Map<String, Object> mapOfToken;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private Map<String, Object> mapOfSharedVars; // Map that can be used to shared variables between steps on the same scenario.

    @Value("${spring.kafka.consumer.bootstrap-servers}") private String kafkaConsumerBootstrapServers;
    @Value("${spring.kafka.consumer.group-id}-service-test") private String kafkaConsumerGroupId;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private KafkaConsumer<String, String> kafkaConsumer;
    private VerifyKafkaEventPublishedSpecs verifyKafkaEventPublishedSpecs;

    @Value("${mockserver.host}") public String mockServerHost;
    @Value("${mockserver.port}") public int mockServerPort;
    @Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) private MockServerClient mockServerClient;

    private List<ResponseEntity<String>> listOfHitApiResponseEntity;

    @Autowired private AppStringLookupFactory appStringLookupFactory;
    @Autowired private TestAppBeanUtils appBeanUtils;
    @Autowired private SignatureHeaderService signatureHeaderService;

    public ScenarioManager()
    {
        var now = Instant.now();

        var uniqueId = now.getEpochSecond() + "";
        var uniqueIdLong = now.toEpochMilli() + "";

        mapOfToken = new TreeMap<>();
        mapOfToken.put("uniqueId", uniqueId);
        mapOfToken.put("uniqueIdLong", uniqueIdLong);
        mapOfToken.put("invoiceNo", "INV_"+uniqueIdLong);

        for(int i=2; i<=5; i++)
        {
            var nowAlt = now.plusSeconds(i-1).plusMillis(i-1);
            var uniqueIdAlt = nowAlt.getEpochSecond() + "";
            var uniqueIdLongAlt = nowAlt.toEpochMilli() + "";

            mapOfToken.put("uniqueId" + i, uniqueIdAlt);
            mapOfToken.put("uniqueIdLong" + i, uniqueIdLongAlt);
            mapOfToken.put("invoiceNo" + i, "INV_"+uniqueIdLongAlt);
        }

        mapOfSharedVars = new HashMap<>();
        mapOfSharedVars.put(SHARED_VARS_ZDT_NOW, now.atZone(ZoneId.systemDefault()));
    }

    /**
     * Notes:
     * - Special arg-key (e.g.: specsFile, timeoutDuration) will be excluded.
     * - Value from a key that starts with "z-ac-" be loaded from the file. The "z" is added to make sure the file is listed at the bottom. The "ac" stands for "argument content".
     */
    public void addTokenFromStepArgs(Map<String, String> args)
    {
        if(ObjectUtils.isNotEmpty(args))
        {
            args.forEach((key, value) ->
            {
                if(!AbstractSteps.isSpecialArgKey(key))
                {
                    if(value != null && value.startsWith("z-ac-"))
                    {
                        var fileContentThatAlreadyInterpolated = getScenarioTestDataContent(value);
                        mapOfToken.put(key, fileContentThatAlreadyInterpolated);
                    }
                    else
                    {
                        mapOfToken.put(key, replaceTokens(value));
                    }
                }
            });
        }
    }

    public void addToken(Map<String, Object> mapOfTokenExt)
    {
        if(ObjectUtils.isNotEmpty(mapOfTokenExt))
        {
            mapOfToken.putAll(mapOfTokenExt);
        }
    }

    public void addToken(String key, Object value)
    {
        mapOfToken.put(key, value);
    }

    public void reInterpolateToken()
    {
        if(mapOfToken != null)
        {
            mapOfToken.forEach((key, value) ->
            {
                if(value instanceof String valueAsString)
                {
                    mapOfToken.put(key, replaceTokens(valueAsString));
                }
                else
                {
                    mapOfToken.put(key, value);
                }
            });
        }
    }

    public String getTokenAsJson()
    {
        return replaceTokens(appBeanUtils.toJsonPretty(mapOfToken));
    }

    /**
     * This method will be called by DefaultHooks for each scenario.
     */
    public void setScenario(Scenario scenario)
    {
        this.scenario = scenario;

        try
        {
            var scenarioUri = scenario.getUri();

            if("classpath".equals(scenarioUri.getScheme()))
            {
                /*
                 * Run using JUnit-5.
                 *
                 * Sample Scenario URI: classpath:features/VA-Permata/VA-Permata.feature
                 */
                var scenarioUriSchemeSpecificPart = scenarioUri.getSchemeSpecificPart(); // Sample result: features/Snap_doku_va_payment/Snap_doku_va_payment.feature
                featureFolderPath = scenarioUriSchemeSpecificPart.substring(0, scenarioUriSchemeSpecificPart.lastIndexOf('/')); // Sample result: features/Snap_doku_va_payment

                featureName = scenarioUriSchemeSpecificPart.split("features", 2)[1]; // Sample result: /Snap_doku_va_payment/Snap_doku_va_payment.feature
            }
            else
            {
                /*
                 * Run using JUnit-4.
                 *
                 * Sample Scenario URI        : file:///Users/daniel/Documents/Git-Repo/GTW-Repo/va-core-system/src/service-test/resources/features/Snap_doku_va_payment/Snap_doku_va_payment.feature
                 * Sample Scenario URI as Path: /Users/daniel/Documents/Git-Repo/GTW-Repo/va-core-system/src/service-test/resources/features/Snap_doku_va_payment/Snap_doku_va_payment.feature
                 */

                featureFolderPath = Paths.get(scenario.getUri())
                    .getParent()
                    .toString()
                    .split("/src/service-test/resources/", 2)[1];

                featureName = Paths.get(scenario.getUri())
                    .toString()
                    .split("/src/service-test/resources/features", 2)[1];
            }

            // Sample scenario name: TC0001 - Success inquiry FixBill ADGPC
            var scenarioNameSplit = scenario.getName().trim().split(" - ", 2);
            var tcId = scenarioNameSplit[0].trim();
            var tcDesc = scenarioNameSplit[1].trim();

            scenarioTestDataFolderPath = featureFolderPath + "/" + tcId;

            mapOfToken.put("tcId", tcId);
            mapOfToken.put("tcDesc", tcDesc);
        }
        catch(Exception ex)
        {
            log.warn("Failed to parse cucumber Scenario. Possibly due to scenario is generated from code-generator.");
        }
    }

    public String getScenarioTestDataFilePath(String filename)
    {
        return getScenarioTestDataFolderPath() + "/" + filename;
    }

    @SneakyThrows
    public String getScenarioTestDataContent(String filename)
    {
        return getScenarioTestDataContent(filename, true);
    }

    @SneakyThrows
    public String getScenarioTestDataContent(String filename, boolean replaceTokens)
    {
        if(StringUtils.isBlank(filename))
        {
            log.warn("Test data content filename must no be blank.");
            return null;
        }

        var testDataFilePath = getScenarioTestDataFilePath(filename);
        var testDataContent = StreamUtils.copyToString(new ClassPathResource(testDataFilePath).getInputStream(), Charset.defaultCharset());

        return replaceTokens? replaceTokens(testDataContent) : testDataContent;
    }

    public String getScenarioTestDataContentFromInitFolder(String filename)
    {
        return getScenarioTestDataContentFromInitFolder(filename, true);
    }

    @SneakyThrows
    public String getScenarioTestDataContentFromInitFolder(String filename, boolean replaceTokens)
    {
        if(StringUtils.isBlank(filename))
        {
            log.warn("Test data content filename from init-folder must no be blank.");
            return null;
        }

        var testDataFilePath = featureFolderPath + "/" + INIT_FOLDER_NAME + "/" + filename;

        try
        {
            var testDataContent = StreamUtils.copyToString(new ClassPathResource(testDataFilePath).getInputStream(), Charset.defaultCharset());
            return replaceTokens? replaceTokens(testDataContent) : testDataContent;
        }
        catch(Exception ex)
        {
            return null;
        }
    }

    public String replaceTokens(String data)
    {
        data = StringSubstitutor.createInterpolator().replace(data); // StringSubstitutor not thread-safe.
        data = new StringSubstitutor(mapOfToken).replace(data);
        data = new StringSubstitutor(appStringLookupFactory).setEnableSubstitutionInVariables(true).replace(data);
        return data;
    }

    public String createApiUrl(String relativeRef)
    {
        var contextPathNormalized = contextPath.charAt(0)=='/'? contextPath.substring(1) : contextPath;
        var relativeRefNormalized = relativeRef.charAt(0)=='/' ? relativeRef.substring(1) : relativeRef;

        var path = contextPathNormalized.endsWith("/")
            ? contextPathNormalized + relativeRefNormalized
            : contextPathNormalized + '/' + relativeRefNormalized;

        path = path.charAt(0)=='/' ? path.substring(1) : path;

        return "http://localhost:%d/%s".formatted(port, path);
    }

    @SneakyThrows
    public String generateSignatureV2(HitApiSpecs specs, String path, String requestBody)
    {
        var headers = specs.getHeaders();
        var method = specs.getMethod();
        var secretKey = Optional.ofNullable(specs.getGenerateSignature())
            .map(HitApiSpecs.GenerateSignature::getSecretKey)
            .orElseThrow(() -> new TestAppRuntimeException("GenerateSignature.secretKey must be provided when signature generation is enabled."));

        var signatureComponentDTO = SignatureComponentDTO.builder()
            .clientId(headers.getFirst(Constant.CLIENT_ID))
            .requestId(headers.getFirst(Constant.REQUEST_ID))
            .timestamp(headers.getFirst(Constant.REQUEST_TIMESTAMP))
            .requestTarget(path)
            .secretKey(secretKey)
            .httpMethod(method)
            .messageBody(requestBody)
            .build();

        return signatureHeaderService.createSignatureRequest(signatureComponentDTO);
    }

    public MockServerClient getMockServerClient()
    {
        if(mockServerClient == null)
        {
            mockServerClient = new MockServerClient(mockServerHost, mockServerPort);
        }

        return mockServerClient;
    }

    public KafkaConsumer<String, String> getKafkaConsumer()
    {
        if(kafkaConsumer == null)
        {
            kafkaConsumer = new KafkaConsumer<>(
                Map.of(
                    ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerBootstrapServers,
                    ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerGroupId,
                    ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(),
                new StringDeserializer()
            );
        }

        return kafkaConsumer;
    }

    @SuppressWarnings("unused")
    public void putSharedVar(String key, Object value)
    {
        mapOfSharedVars.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getSharedVar(String key)
    {
        return (T) mapOfSharedVars.get(key);
    }
}
