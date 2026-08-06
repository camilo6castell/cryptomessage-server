package com.cryptomessage.server.domain.generic;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events. Follows the Event Sourcing pattern: events are
 * immutable records of something that already happened.
 *
 * NOTE: unlike the Library Provider version this class is based on, the version
 * number is deliberately NOT tracked here. In that project the in-memory version
 * counter (ChangeEventSubscriber) was never re-synchronized when an aggregate was
 * reconstructed from history, so two independent aggregate instances could compute
 * the same "next version" and silently collide. Here, the persistence adapter
 * (infrastructure layer) is the single source of truth for stream position — it
 * assigns the version at save time based on how many events are actually stored,
 * which is also what makes optimistic concurrency checks reliable.
 */
public abstract class DomainEvent implements Serializable {

    private final String eventId;
    private LocalDateTime occurredOn;
    private String aggregateRootId;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(LocalDateTime occurredOn) {
        this.occurredOn = occurredOn;
    }

    public String getAggregateRootId() {
        return aggregateRootId;
    }

    public void setAggregateRootId(String aggregateRootId) {
        this.aggregateRootId = aggregateRootId;
    }
}
