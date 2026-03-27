package com.nsia.commons.cucumber.glue;

import com.nsia.commons.cucumber.glue.exception.TestAppRuntimeException;
import com.nsia.commons.cucumber.glue.model.FetchDataFromDbSpecs;
import com.nsia.commons.cucumber.glue.model.HitApiSpecs;
import com.nsia.commons.cucumber.glue.model.HitApiVerifyResultSpecs;
import com.nsia.commons.cucumber.glue.model.PrepareDataSpecs;
import com.nsia.commons.cucumber.glue.model.PrepareMockServerSpecs;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mockserver.model.Delay;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RegexBody;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Slf4j
@RequiredArgsConstructor
public class StdOpsV1Steps extends AbstractSteps
{
    private static final Duration HIT_API_TIMEOUT_DURATION_DEFAULT = Duration.ofSeconds(10);


    /**
     * This step file is deprecated, please use the latest version instead.
     * Some old scenarios still need to use this step file.
     */
    @SuppressWarnings("SqlSourceToSinkFlow")
    @Given("^StdOpsV1Steps - Prepare Data V1$")
    public void prepareDataV1(Map<String, String> args)
    {
        log.info("Executing prepareDataV1...");
        var stepName = "StdOpsV1Steps - Prepare Data V1";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());

            //<editor-fold desc="Prepare Data from Init-Folder">
            {
                var specsOpt = getSpecsFromInitFolder(args, ARG_KEY_SPECS_FILE, PrepareDataSpecs.class, true, appAllure);

                specsOpt.ifPresent(it ->
                {
                    var specs = specsOpt.get();
                    scenarioManager.addToken(specs.getMapOfTokenExt());

                    var initSqlFile = specs.getInitSqlFile();

                    if(StringUtils.isNotBlank(initSqlFile))
                    {
                        var initSqlFileContent = scenarioManager.getScenarioTestDataContentFromInitFolder(initSqlFile);
                        appAllure.addAttachment("initSqlFileContent - Init-Folder", initSqlFileContent);

                        if(StringUtils.isNotBlank(initSqlFileContent))
                        {
                            jdbcTemplate.execute(initSqlFileContent);
                        }
                    }
                });
            }
            //</editor-fold>

            //<editor-fold desc="Prepare Data from Scenario-Folder">
            {
                scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.

                var specs = getSpecs(args, ARG_KEY_SPECS_FILE, PrepareDataSpecs.class, true, appAllure);

                scenarioManager.addToken(specs.getMapOfTokenExt());

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
            }
            //</editor-fold>
        }
        finally
        {
            appAllure.writeStep();
        }
    }

    @Given("^StdOpsV1Steps - Prepare Data$")
    public void prepareData(Map<String, String> args)
    {
        log.info("Executing prepareData...");
        var stepName = "StdOpsV1Steps - Prepare Data";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());
            scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.
            doPrepareData(args, appAllure);
        }
        finally
        {
            appAllure.writeStep();
        }
    }

    @SuppressWarnings("resource")
    @Given("^StdOpsV1Steps - Prepare MockServer$")
    public void prepareMockServer(Map<String, String> args)
    {
        log.info("Executing prepareMockServer...");
        var stepName = "StdOpsV1Steps - Prepare MockServer";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());

            scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.

            var mockServerClient = scenarioManager.getMockServerClient().reset();
            var specs = getSpecs(args, ARG_KEY_SPECS_FILE, PrepareMockServerSpecs.class, true, appAllure);

            specs.getListOfCriteria()
                .forEach(it ->
                    {
                        var responseBody = it.getResponseBody(scenarioManager);

                        if(it.getResponseBodyFile()!=null)
                        {
                            appAllure.addAttachment(it.getResponseBodyFile(), responseBody);
                        }

                        mockServerClient
                            .when(
                                HttpRequest.request()
                                    .withPath(it.getRequestPath())
                                    .withMethod(it.getRequestMethod())
                                    .withBody(
                                        StringUtils.isNotBlank(it.getRequestBodyMatchedWithRegex())
                                            ? RegexBody.regex(it.getRequestBodyMatchedWithRegex())
                                            : null
                                    )
                            )
                            .respond(
                                HttpResponse.response()
                                    .withDelay(
                                        Optional.ofNullable(it.getResponseDelay())
                                            .map(Duration::toMillis)
                                            .map(Delay::milliseconds)
                                            .orElse(null)
                                    )
                                    .withStatusCode(it.getResponseStatusCode())
                                    .withHeaders(it.getResponseHeadersAsMockServerHeaders())
                                    .withBody(responseBody)
                                    .withHeaders()
                            );
                    }
                );
        }
        finally
        {
            appAllure.writeStep();
        }
    }

    @When("^StdOpsV1Steps - Hit API$")
    public void hitApi(Map<String, String> args)
    {
        log.info("Executing hitApi...");
        var stepName = "StdOpsV1Steps - Hit API";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());

            scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.

            var timeoutDuration = getArgAsDuration(args, ARG_KEY_TIMEOUT_DURATION, HIT_API_TIMEOUT_DURATION_DEFAULT);
            var specs = getSpecs(args, ARG_KEY_SPECS_FILE, HitApiSpecs.class, true, appAllure);

            var apiUri = scenarioManager.createApiUrl(specs.getRelativeRef());
            var method = HttpMethod.valueOf(specs.getMethod().toUpperCase());
            var payload = specs.isMultipartFormDataOrApplicationFormUrlEncoded()
                ? specs.getPayloadFormData()
                : Optional.ofNullable(specs.getPayloadRaw())
                .map(scenarioManager::replaceTokens)
                .orElseGet(() ->
                {
                    var uri = URI.create(apiUri);
                    var path = uri.getPath();
                    var requestBody = scenarioManager.getScenarioTestDataContent(specs.getPayloadFile());

                    if(specs.isGenerateSignatureEnabled())
                    {
                        var signature = scenarioManager.generateSignatureV2(specs, path, requestBody);
                        specs.getHeaders().add("Signature", signature);
                    }

                    return requestBody;
                });

            var httpEntity = new HttpEntity<>(payload, specs.getHeaders());

            var httpRequestCounter = new AtomicInteger(1);

            var listOfResponseEntity = Stream.iterate(0, i -> i<specs.getNumberOfConcurrentRequests(), i -> i+1)
                .map(it ->
                {
                    var cf = CompletableFuture.supplyAsync(() ->
                    {
                        var index = httpRequestCounter.getAndIncrement();
                        var responseEntity = testRestTemplate.exchange(apiUri, method, httpEntity, String.class);

                        var httpInfo =
                            "===== HTTP Request =====" + "\n" +
                                "URI    : " + apiUri + "\n" +
                                "Method : " + method + "\n" +
                                "Headers: " + httpEntity.getHeaders() + "\n" +
                                "Body   :\n" + Optional.ofNullable(httpEntity.getBody()).map(Object::toString).orElse("") + "\n" +
                                "------------------------\n\n" +
                                "===== HTTP Response =====" + "\n" +
                                "Status Code: " + responseEntity.getStatusCode().value() + "\n" +
                                "Headers    : " + responseEntity.getHeaders() + "\n" +
                                "Body       :\n" + Optional.ofNullable(responseEntity.getBody()).map(Object::toString).orElse("") + "\n" +
                                "-------------------------";

                        appAllure.addAttachment("HTTP Request & Response #%d".formatted(index), httpInfo);
                        return responseEntity;
                    });

                    cf.orTimeout(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS);
                    return cf;
                })
                .toList()
                .stream()
                .map(CompletableFuture::join) // Make sure all CompletableFuture is created first before you call join().
                .toList();

            scenarioManager.setListOfHitApiResponseEntity(listOfResponseEntity);
        }
        finally
        {
            appAllure.writeStep();
        }
    }

    @Then("^StdOpsV1Steps - Hit API - Verify Result$")
    public void hitApiVerifyResult(Map<String, String> args)
    {
        log.info("Executing hitApiVerifyResult...");
        var stepName = "StdOpsV1Steps - Hit API - Verify Result";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());

            scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.

            var specs = getSpecs(args, ARG_KEY_SPECS_FILE, HitApiVerifyResultSpecs.class, true, appAllure);

            if(scenarioManager.getListOfHitApiResponseEntity().size()==1)
            {
                // Validate Single Hit API Response
                var responseEntity = scenarioManager.getListOfHitApiResponseEntity().getFirst();
                var criteria = specs.getListOfCriteria().getFirst();

                appAllure.addAttachment("Response Schema for Criteria(id = '%s')".formatted(criteria.getId()), criteria.getResponseBodySchema(scenarioManager));
                verifyResponseEntity(criteria, responseEntity);
            }
            else
            {
                // Validate Multiple Hit API Response
                var tempResponseEntity = new ArrayList<>(scenarioManager.getListOfHitApiResponseEntity());

                specs.getListOfCriteria()
                    .forEach(criteria ->
                    {
                        var responseEntityOpt = tempResponseEntity.stream()
                            .filter(criteria::isResponseEntityMatched)
                            .findFirst();

                        assertTrue(responseEntityOpt.isPresent(), "No ResponseEntity found for Criteria(id = '%s').".formatted(criteria.getId()));

                        appAllure.addAttachment("Response Schema for Criteria(id = '%s')".formatted(criteria.getId()), criteria.getResponseBodySchema(scenarioManager));
                        verifyResponseEntity(criteria, responseEntityOpt.get());
                        tempResponseEntity.remove(responseEntityOpt.get());
                    });
            }
        }
        finally
        {
            appAllure.writeStep();
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    @When("^StdOpsV1Steps - Fetch Data from DB$")
    public void fetchDataFromDb(Map<String, String> args)
    {
        log.info("Executing fetchDataFromDb...");
        var stepName = "StdOpsV1Steps - Fetch Data from DB";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());

            scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.

            var specs = getSpecs(args, ARG_KEY_SPECS_FILE, FetchDataFromDbSpecs.class, true, appAllure);

            specs.getListOfCriteria()
                .forEach(it ->
                {
                    try
                    {
                        var resultList = jdbcTemplate.queryForList(it.getQuery());
                        var expectedCount = 1;
                        var actualCount = resultList.size();

                        assertEquals(expectedCount, actualCount, "Criteria(id = '%s') - Query[%s]".formatted(it.getId(), it.getQuery()));

                        var result = resultList.getFirst();

                        it.getQueryResultMapping()
                            .forEach((key, valueAsTokenName) -> scenarioManager.addToken(valueAsTokenName, result.get(key)));
                    }
                    catch(Exception ex)
                    {
                        throw new TestAppRuntimeException("[Criteria(id = '%s')] Failed to execute Query[%s].".formatted(it.getId(), it.getQuery()), ex);
                    }
                });
        }
        finally
        {
            appAllure.writeStep();
        }
    }
}
