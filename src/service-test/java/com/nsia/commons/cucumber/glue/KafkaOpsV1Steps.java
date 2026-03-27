package com.nsia.commons.cucumber.glue;

import com.nsia.commons.cucumber.glue.model.PublishKafkaEventSpecs;
import com.nsia.commons.cucumber.glue.model.PublishKafkaEventVerifyResultSpecs;
import com.nsia.commons.cucumber.glue.model.VerifyKafkaEventPublishedSpecs;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Slf4j
@RequiredArgsConstructor
public class KafkaOpsV1Steps extends AbstractSteps
{
    private static final Duration PUBLISH_KAFKA_EVENT_TIMEOUT_DURATION_DEFAULT = Duration.ofSeconds(10);
    private static final Duration AWAIT_AT_MOST_DURATION_DEFAULT = Duration.ofSeconds(10);

    @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

    @SneakyThrows
    @When("^KafkaOpsV1Steps - Publish Kafka Event$")
    public void publishKafkaEvent(Map<String, String> args)
    {
        log.info("Executing publishKafkaEvent...");
        var stepName = "KafkaOpsV1Steps - Publish Kafka Event";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());

            scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.

            var timeoutDuration = getArgAsDuration(args, ARG_KEY_TIMEOUT_DURATION, PUBLISH_KAFKA_EVENT_TIMEOUT_DURATION_DEFAULT);
            var specs = getSpecs(args, ARG_KEY_SPECS_FILE, PublishKafkaEventSpecs.class, true, appAllure);

            var topic = specs.getTopic();
            var key = specs.getKey();
            var headers = specs.getHeaders();

            var value = Optional.ofNullable(specs.getValueRaw())
                .map(scenarioManager::replaceTokens)
                .orElseGet(() -> scenarioManager.getScenarioTestDataContent(specs.getValueFile()));

            var producerRecord = new ProducerRecord<String, Object>(topic, key, value);

            Optional.ofNullable(headers)
                .ifPresent(it -> it.forEach((headerKey,headerValue) ->
                    producerRecord.headers().add(headerKey, SerializationUtils.serialize(headerValue))
                ));

            var kafkaEventInfo = """
                Headers: %s
                Key    : %s
                Value  :
                %s
                """
                .formatted
                    (
                        Optional.ofNullable(headers).map(it -> testAppBeanUtils.toJson(it)).orElse(null),
                        key,
                        value
                    );

            if(specs.getDelayBeforeEventPublished() != null && specs.getDelayBeforeEventPublished().toMillis() > 0)
            {
                log.info("Delaying for '{}' before publishing Kafka event.", specs.getDelayBeforeEventPublished());
                Thread.sleep(specs.getDelayBeforeEventPublished().toMillis());
            }

            appAllure.addAttachment("Publish Kafka Event - %s".formatted(topic), kafkaEventInfo);

            var future = kafkaTemplate.send(producerRecord);

            future.whenComplete((result, ex) ->
            {
                if(ex != null)
                {
                    var errorMessage = "[%s] Kafka event failed to send.".formatted(topic);
                    log.error(errorMessage, ex);
                }
            });

            var publishKafkaEventSuccess = true;

            try
            {
                future.orTimeout(timeoutDuration.toMillis(), TimeUnit.MILLISECONDS)
                    .join();
            }
            catch(Exception ex)
            {
                publishKafkaEventSuccess = false;
                var errorMessage = "[%s] Kafka event failed to send.".formatted(topic);
                log.error(errorMessage, ex);
            }

            assertTrue(publishKafkaEventSuccess, "[%s] Kafka event failed to send.".formatted(topic));

            if(specs.getDelayAfterEventPublished() != null && specs.getDelayAfterEventPublished().toMillis() > 0)
            {
                log.info("Delaying for '{}' after publishing Kafka event.", specs.getDelayAfterEventPublished());
                Thread.sleep(specs.getDelayAfterEventPublished().toMillis());
            }
        }
        finally
        {
            appAllure.writeStep();
        }
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Then("^KafkaOpsV1Steps - Publish Kafka Event - Verify Result$")
    public void publishKafkaEventVerifyResult(Map<String, String> args)
    {
        log.info("Executing publishKafkaEventVerifyResult...");
        var stepName = "KafkaOpsV1Steps - Publish Kafka Event - Verify Result";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());

            scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.

            var specs = getSpecs(args, ARG_KEY_SPECS_FILE, PublishKafkaEventVerifyResultSpecs.class, true, appAllure);

            //<editor-fold desc="Verify Data on DB">
            if(ObjectUtils.isNotEmpty(specs.getListOfVerifyDbCriteria()))
            {
                specs.getListOfVerifyDbCriteria().forEach(it -> {
                    var actualCount = jdbcTemplate.queryForObject(it.getQuery(), Long.class);
                    var expectedCount = it.getCount();

                    try
                    {
                        assertEquals(expectedCount, actualCount, "VerifyDbCriteria(id = '%s') - Query[%s]".formatted(it.getId(), it.getQuery()));
                    }
                    catch(AssertionError ex)
                    {
                        if(StringUtils.isNotBlank(it.getQueryIfAssertionError()))
                        {
                            var queryIfAssertionFailedResult = jdbcTemplate.queryForList(it.getQueryIfAssertionError());
                            log.error(
                                """
                                VerifyDbCriteria(id = '{}') - AssertionError:
                                Query                          : {}
                                Query If Assertion Error       : {}
                                Query If Assertion Error Result:
                                {}
                                """,
                                it.getId(), it.getQuery(), it.getQueryIfAssertionError(), queryIfAssertionFailedResult
                            );
                        }

                        throw ex;
                    }
                });
            }
            //</editor-fold>
        }
        finally
        {
            appAllure.writeStep();
        }
    }

    @SneakyThrows
    @Then("^KafkaOpsV1Steps - Verify Kafka Event Published$")
    public void verifyKafkaEventPublished(Map<String, String> args)
    {
        log.info("Executing verifyKafkaEventPublished...");
        var stepName = "KafkaOpsV1Steps - Verify Kafka Event Published";
        var appAllure = new AppAllure(stepName);

        try
        {
            appAllure.addAttachment("args", args.toString());

            scenarioManager.addTokenFromStepArgs(args); // This must be added before getting specs from the scenario folder.

            var awaitAtMostDuration = getArgAsDuration(args, ARG_KEY_AWAIT_AT_MOST_DURATION, AWAIT_AT_MOST_DURATION_DEFAULT);
            var specs = getSpecs(args, ARG_KEY_SPECS_FILE, VerifyKafkaEventPublishedSpecs.class, true, appAllure);

            scenarioManager.getKafkaConsumer().subscribe(specs.getTopics());
            log.info("Waiting Kafka Event for {}.", awaitAtMostDuration);

            await()
                .atMost(awaitAtMostDuration)
                .untilAsserted(() ->
                {
                    var consumerRecords = scenarioManager.getKafkaConsumer().poll(Duration.ofMillis(100));

                    if(ObjectUtils.isNotEmpty(consumerRecords))
                    {
                        specs.getTopics()
                            .forEach(topic ->
                                consumerRecords.records(topic)
                                    .forEach(record ->
                                        specs.getListOfCriteria()
                                            .stream()
                                            .filter(it -> it.getTopic().equals(record.topic()) && it.isEventMatched(record.value()))
                                            .forEach(it -> it.addEvent(record))
                                    )
                            );

                        scenarioManager.getKafkaConsumer().commitSync();
                    }

                    specs.getListOfCriteria()
                        .forEach(it ->
                        {
                            if(it.getNumberOfExpectedEvents() >= 0)
                            {
                                assertEquals(it.getNumberOfExpectedEvents(), it.getListOfEvents().size(), "EventCriteria(id = '%s') with topic = '%s' has unexpected size.".formatted(it.getId(), it.getTopic()));
                            }
                        });
                });

            specs.getListOfCriteria()
                .forEach(it ->
                {
                    appAllure.addAttachment("[%s] %s".formatted(it.getId(), it.getTopic()), it.toStringForAllure());

                    if(ObjectUtils.isNotEmpty(it.getEventSchemaFile()) && ObjectUtils.isNotEmpty(it.getListOfEvents()))
                    {
                        var eventSchemaFileContent = scenarioManager.getScenarioTestDataContent(it.getEventSchemaFile());
                        appAllure.addAttachment("[%s] %s - Event Schema".formatted(it.getId(), it.getTopic()), eventSchemaFileContent);

                        it.getListOfEvents()
                            .forEach(event ->
                            {
                                if(ObjectUtils.isNotEmpty(it.getExpectedKey()))
                                {
                                    assertNotNull(event.getKey(), "EventCriteria(id = '%s') with topic = '%s' has unexpected event-key. Event-key must not be null.".formatted(it.getId(), it.getTopic()));
                                    assertTrue(event.getKey().matches(it.getExpectedKey()), "EventCriteria(id = '%s') with topic = '%s' has unexpected event-key. Expected '%s' to match pattern '%s'".formatted(it.getId(), it.getTopic(), event.getKey(), it.getExpectedKey()));
                                }

                                if(ObjectUtils.isNotEmpty(it.getExpectedHeaders()))
                                {
                                    it.getExpectedHeaders()
                                        .forEach((headerKey, headerValue) ->
                                        {
                                            assertTrue(event.headersContainsKey(headerKey), "EventCriteria(id = '%s') with topic = '%s' does not contain headers['%s'].".formatted(it.getId(), it.getTopic(), headerKey));
                                            assertEquals(headerValue, event.getHeaderValue(headerKey), "EventCriteria(id = '%s') with topic = '%s' has unexpected headers['%s'] value.".formatted(it.getId(), it.getTopic(), headerKey));
                                        });
                                }

                                var validateJsonSuccess = true;

                                try
                                {
                                    validateJson(eventSchemaFileContent, event.getValue());
                                }
                                catch(Exception ex)
                                {
                                    validateJsonSuccess = false;
                                }

                                assertTrue(validateJsonSuccess, "EventCriteria(id = '%s') with topic = '%s' has invalid JSON schema.".formatted(it.getId(), it.getTopic()));
                            });
                    }
                });

            scenarioManager.setVerifyKafkaEventPublishedSpecs(specs);
        }
        finally
        {
            appAllure.writeStep();
            scenarioManager.getKafkaConsumer().unsubscribe();
        }
    }
}
