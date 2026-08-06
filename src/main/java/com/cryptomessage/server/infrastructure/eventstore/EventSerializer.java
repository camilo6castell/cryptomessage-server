package com.cryptomessage.server.infrastructure.eventstore;

import com.cryptomessage.server.domain.generic.DomainEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class EventSerializer {

    // FAIL_ON_UNKNOWN_PROPERTIES is off deliberately: each event's @JsonCreator
    // constructor only declares its own fields (see ChatCreated, MessageSent...).
    // occurredOn/aggregateRootId/eventId also get serialized here (DomainEvent's
    // getters) but are restored separately by ChatAggregateRepositoryAdapter from
    // StoredEvent's own columns — see the adapter for why.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String toJson(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new EventSerializationException("Could not serialize event " + event.getClass().getSimpleName(), e);
        }
    }

    public DomainEvent fromJson(String json, String eventType) {
        try {
            Class<?> eventClass = Class.forName(eventType);
            return (DomainEvent) objectMapper.readValue(json, eventClass);
        } catch (ClassNotFoundException e) {
            throw new EventSerializationException(
                    "Cannot deserialize event — class not found: '" + eventType
                            + "'. This may indicate a renamed or deleted event class.", e);
        } catch (Exception e) {
            throw new EventSerializationException("Could not deserialize event of type " + eventType, e);
        }
    }
}
