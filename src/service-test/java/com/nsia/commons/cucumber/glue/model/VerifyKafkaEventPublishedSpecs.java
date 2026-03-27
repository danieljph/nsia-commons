package com.nsia.commons.cucumber.glue.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Joi Partogi Hutapea
 */
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyKafkaEventPublishedSpecs
{
    @Valid @NotNull @Size(min = 1) private List<EventCriteria> listOfCriteria;

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventCriteria
    {
        @NotBlank private String id;
        @NotBlank private String topic;

        private Map<String, String> expectedHeaders;
        private String expectedKey;

        private List<String> eventContains;
        private String eventSchemaFile;

        private int numberOfExpectedEvents = 1; // If value < 0, it means no limit on the number of expected events.

        private List<Event> listOfEvents = new ArrayList<>(); // This field is used by Step-Definition implementation to save the Kafka event found.

        public boolean isEventMatched(String event)
        {
            if(ObjectUtils.isEmpty(eventContains))
            {
                return true;
            }

            return eventContains.stream().allMatch(event::contains);
        }

        public void addEvent(ConsumerRecord<String, String> consumerRecord)
        {
            var event = new Event();
            event.setKey(consumerRecord.key());
            event.setHeaders(consumerRecord.headers());
            event.setValue(consumerRecord.value());
            listOfEvents.add(event);
        }

        public String toStringForAllure()
        {
            var result = new StringBuilder();

            result.append("ID: ").append(id).append('\n');
            result.append("Topic: ").append(topic).append('\n');
            result.append("Event Contains: ").append(eventContains).append('\n');
            result.append("Event Schema File: ").append(eventSchemaFile).append('\n');
            result.append("Number of Expected Events: ").append(numberOfExpectedEvents).append('\n');

            for(int i=0; i<listOfEvents.size(); i++)
            {
                var event = listOfEvents.get(i);

                result.append('\n')
                    .append("Event #").append(i+1).append(":\n")
                    .append("Key: ").append(event.getKey()).append('\n')
                    .append("Headers: ").append(event.getHeaders()).append('\n')
                    .append("Value:\n")
                    .append(event.getValue()).append('\n');
            }

            return result.toString();
        }
    }

    @Setter @Getter
    public static class Event
    {
        private String key;
        private Map<String, String> headers;
        private String value;

        public void setHeaders(Headers headers)
        {
            if(headers != null)
            {
                this.headers = new HashMap<>();
                headers.forEach(header -> this.headers.put(header.key(), new String(header.value())));
            }
        }

        public boolean headersContainsKey(String headerKey)
        {
            return headers != null && headers.containsKey(headerKey);
        }

        public String getHeaderValue(String headerKey)
        {
            if(headers != null && headers.containsKey(headerKey))
            {
                return headers.get(headerKey);
            }

            return null;
        }
    }

    public Set<String> getTopics()
    {
        var topics = new HashSet<String>();

        if(listOfCriteria !=null)
        {
            listOfCriteria.forEach(topicSpec -> topics.add(topicSpec.topic));
        }

        return topics;
    }
}
