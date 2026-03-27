package com.nsia.commons.cucumber.glue;

import com.nsia.commons.cucumber.glue.exception.TestAppRuntimeException;
import com.nsia.commons.cucumber.glue.model.HitApiVerifyResultSpecs;
import com.nsia.commons.cucumber.glue.model.PrepareDataSpecs;
import com.nsia.commons.cucumber.glue.support.TestAppBeanUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
@Getter
public abstract class AbstractSteps
{
    public static final String ARG_KEY_SPECS_FILE = "specsFile";
    public static final String ARG_KEY_TIMEOUT_DURATION = "timeoutDuration";
    public static final String ARG_KEY_AWAIT_AT_MOST_DURATION = "awaitAtMostDuration";

    protected ScenarioManager scenarioManager;
    protected JdbcTemplate jdbcTemplate;
    protected TestAppBeanUtils testAppBeanUtils;
    protected TestRestTemplate testRestTemplate;

    public static boolean isSpecialArgKey(String argKey)
    {
        return ARG_KEY_SPECS_FILE.equals(argKey) || ARG_KEY_TIMEOUT_DURATION.equals(argKey) || ARG_KEY_AWAIT_AT_MOST_DURATION.equals(argKey);
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public void doPrepareData(Map<String, String> args, AppAllure appAllure)
    {
        PrepareDataSpecs specsInit = null;
        PrepareDataSpecs specs;

        var specsFile = args.get(ARG_KEY_SPECS_FILE);

        if(StringUtils.isBlank(specsFile))
        {
            throw new TestAppRuntimeException("%s must not be blank.".formatted(ARG_KEY_SPECS_FILE));
        }

        // Initiate PrepareDataSpecs from the Init-Folder.
        var specsFileInitContent = scenarioManager.getScenarioTestDataContentFromInitFolder(specsFile, false);

        if(specsFileInitContent != null)
        {
            specsInit = testAppBeanUtils.fromJson(specsFileInitContent, PrepareDataSpecs.class);
            testAppBeanUtils.validate(specsInit);

            scenarioManager.addToken(specsInit.getMapOfTokenExt());
        }

        // Initiate PrepareDataSpecs from the Scenario-Folder.
        var specsFileContent = scenarioManager.getScenarioTestDataContent(specsFile, false);

        specs = testAppBeanUtils.fromJson(specsFileContent, PrepareDataSpecs.class);
        testAppBeanUtils.validate(specs);
        scenarioManager.addToken(specs.getMapOfTokenExt());

        scenarioManager.reInterpolateToken();

        if(specsInit != null)
        {
            appAllure.addAttachment("specsFile - Init-Folder", specsFileInitContent);

            var initSqlFile = specsInit.getInitSqlFile();

            if(StringUtils.isNotBlank(initSqlFile))
            {
                var initSqlFileContent = scenarioManager.getScenarioTestDataContentFromInitFolder(initSqlFile);
                appAllure.addAttachment("initSqlFileContent - Init-Folder", initSqlFileContent);

                if(StringUtils.isNotBlank(initSqlFileContent))
                {
                    jdbcTemplate.execute(initSqlFileContent);
                }
            }
        }

        appAllure.addAttachment(ARG_KEY_SPECS_FILE, specsFileContent);

        var initSqlFile = specs.getInitSqlFile();

        if(StringUtils.isNotBlank(initSqlFile))
        {
            var initSqlFileContent = scenarioManager.getScenarioTestDataContent(initSqlFile);
            appAllure.addAttachment("initSqlFileContent", initSqlFileContent);

            if(StringUtils.isNotBlank(initSqlFileContent))
            {
                jdbcTemplate.execute(initSqlFileContent);
            }
        }

        appAllure.addAttachment("mapOfToken - Combined", scenarioManager.getTokenAsJson());
    }

    public <T> Optional<T> getSpecsFromInitFolder(Map<String, String> args, String specsFileKey, Class<T> specsType, boolean validateSpecs, AppAllure appAllure)
    {
        Optional<T> result = Optional.empty();
        var specsFile = args.get(specsFileKey);

        if(StringUtils.isBlank(specsFile))
        {
            throw new TestAppRuntimeException("%s must not be blank.".formatted(specsFileKey));
        }

        var specsFileContent = scenarioManager.getScenarioTestDataContentFromInitFolder(specsFile);

        if(specsFileContent!=null)
        {
            if(appAllure!=null)
            {
                appAllure.addAttachment("specsFile - Init-Folder", specsFileContent);
            }

            var specs = testAppBeanUtils.fromJson(specsFileContent, specsType);

            if(validateSpecs)
            {
                testAppBeanUtils.validate(specs);
            }

            result = Optional.of(specs);
        }

        return result;
    }

    public <T> T getSpecs(Map<String, String> args, String specsFileKey, Class<T> specsType, boolean validateSpecs, AppAllure appAllure)
    {
        var specsFile = args.get(specsFileKey);

        if(StringUtils.isBlank(specsFile))
        {
            throw new TestAppRuntimeException("%s must not be blank.".formatted(specsFileKey));
        }

        var specsFileContent = scenarioManager.getScenarioTestDataContent(specsFile);

        if(appAllure!=null && StringUtils.isNotBlank(specsFileContent))
        {
            appAllure.addAttachment(specsFileKey, specsFileContent);
        }

        var specs = testAppBeanUtils.fromJson(specsFileContent, specsType);

        if(validateSpecs)
        {
            testAppBeanUtils.validate(specs);
        }

        return specs;
    }

    public Duration getArgAsDuration(Map<String, String> args, String argKey, Duration defaultValue)
    {
        var result = defaultValue;
        var argValue = args.get(argKey);

        if(StringUtils.isNotBlank(argValue))
        {
            result = Duration.parse(argValue);
        }

        return result;
    }

    @SneakyThrows
    public void validateJson(String jsonSchema, String jsonData)
    {
        var jsonSchemaAsJo = new JSONObject(new JSONTokener(jsonSchema));
        var jsonDataAsJo = new JSONObject(new JSONTokener(jsonData));

        log.info("JSON Schema : {}", jsonSchemaAsJo);
        log.info("JSON Data   : {}", jsonDataAsJo);

        var schema = SchemaLoader.load(jsonSchemaAsJo);

        try
        {
            schema.validate(jsonDataAsJo);
        }
        catch(ValidationException ex)
        {
            var listOfInvalidField = ex.getCausingExceptions().stream()
                .map(ValidationException::getMessage)
                .collect(Collectors.joining("\n"));

            log.error("JSON ValidationException: {}\n{}", ex.getMessage(), listOfInvalidField);
            throw ex;
        }
    }

    @SneakyThrows
    public void validateXml(String xsdContent, String xmlData)
    {
        try
        {
            log.info("XSD Content :\n{}", xsdContent);
            log.info("XML Data    :\n{}", xmlData);

            var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            var schema = schemaFactory.newSchema(new StreamSource(new StringReader(xsdContent)));
            var validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xmlData)));
        }
        catch(Exception ex)
        {
            log.error("XML ValidationException: {}", ex.getMessage());
            throw ex;
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public void verifyResponseEntity(HitApiVerifyResultSpecs.HitApiVerifyResultCriteria criteria, ResponseEntity<String> responseEntity)
    {
        //<editor-fold desc="Verify API HTTP Response">
        assertEquals(criteria.getStatusCode(), responseEntity.getStatusCode().value(), "HTTP Status Code");

        if(StringUtils.isNotBlank(criteria.getResponseBodySchemaRaw()) || StringUtils.isNotBlank(criteria.getResponseBodySchemaFile()))
        {
            var responseBodySchemaFileContent = criteria.getResponseBodySchema(scenarioManager);

            switch(criteria.getContentType())
            {
                case MediaType.APPLICATION_JSON_VALUE ->
                    validateJson(responseBodySchemaFileContent, responseEntity.getBody());
                case MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE ->
                    validateXml(responseBodySchemaFileContent, responseEntity.getBody());
                default ->
                {
                    if(responseBodySchemaFileContent.startsWith("neq-")) // neq stands for "not equal"
                    {
                        var responseBodySchemaFileContentSubstring = responseBodySchemaFileContent.trim().substring("neq-".length());
                        assertNotEquals(responseBodySchemaFileContentSubstring, Optional.ofNullable(responseEntity.getBody()).map(String::trim).orElse(null), "Response-Body should not equal for Criteria(id = '%s')".formatted(criteria.getId()));
                    }
                    else
                    {
                        assertEquals(responseBodySchemaFileContent.trim(), Optional.ofNullable(responseEntity.getBody()).map(String::trim).orElse(null), "Response-Body not equal for Criteria(id = '%s')".formatted(criteria.getId()));
                    }
                }
            }
        }
        //</editor-fold>

        //<editor-fold desc="Verify Data on DB">
        if(ObjectUtils.isNotEmpty(criteria.getListOfVerifyDbCriteria()))
        {
            criteria.getListOfVerifyDbCriteria().forEach(it -> {
                var actualCount = jdbcTemplate.queryForObject(it.getQuery(), Long.class);
                var expectedCount = it.getCount();

                try
                {
                    assertEquals(expectedCount, actualCount, "Criteria(id = '%s') - Query[%s]".formatted(criteria.getId(), it.getQuery()));
                }
                catch(AssertionError ex)
                {
                    if(StringUtils.isNotBlank(it.getQueryIfAssertionError()))
                    {
                        var queryIfAssertionFailedResult = testAppBeanUtils.toJson(jdbcTemplate.queryForList(it.getQueryIfAssertionError()));
                        log.error(
                            """
                            Criteria(id = '{}') - AssertionError:
                            Query                          : {}
                            Query If Assertion Error       : {}
                            Query If Assertion Error Result:
                            {}
                            """,
                            criteria.getId(), it.getQuery(), it.getQueryIfAssertionError(), queryIfAssertionFailedResult
                        );
                    }

                    throw ex;
                }
            });
        }
        //</editor-fold>
    }

    public void sleep(Duration duration)
    {
        sleep(duration.toMillis());
    }

    public void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch(InterruptedException ignored)
        {
        }
    }

    @Autowired
    public void setScenarioManager(ScenarioManager scenarioManager)
    {
        this.scenarioManager = scenarioManager;
    }

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate)
    {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Autowired
    public void setTestAppBeanUtils(TestAppBeanUtils testAppBeanUtils)
    {
        this.testAppBeanUtils = testAppBeanUtils;
    }

    @Autowired
    public void setTestRestTemplate(TestRestTemplate testRestTemplate)
    {
        this.testRestTemplate = testRestTemplate;
    }
}
