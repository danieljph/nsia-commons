package com.nsia.commons.cucumber.glue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.Map;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishKafkaEventSpecs
{
    private String topic;
    private Map<String, String> headers;
    private String key;
    private String valueRaw; // This field will be prioritized over valueFile.
    private String valueFile; // Will be use if valueRaw is blank.
    private Duration delayBeforeEventPublished = Duration.ofSeconds(0); // Delay before publish the event. This is needed to make sure the Kafka is ready. Sometimes Kafka is need to do rebalancing and it takes time.
    private Duration delayAfterEventPublished = Duration.ofSeconds(1); // Delay after event successfully published. This is needed to make sure the event is processed by the consumer before the next step is executed.
}
