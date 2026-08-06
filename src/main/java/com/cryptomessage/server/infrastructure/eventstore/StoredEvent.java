package com.cryptomessage.server.infrastructure.eventstore;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * JPA envelope for a persisted domain event ("events" table = the actual event
 * store). The unique constraint on (aggregate_id, version) is what makes
 * optimistic concurrency control work: two concurrent writers racing to append
 * the same version both attempt the insert, and the database — not application
 * code — guarantees only one wins.
 */
@Entity
@Table(
        name = "events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_events_aggregate_version", columnNames = {"aggregate_id", "version"})
        },
        indexes = {
                @Index(name = "idx_events_aggregate_id", columnList = "aggregate_id"),
                @Index(name = "idx_events_occurred_on", columnList = "occurred_on")
        }
)
public class StoredEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private String aggregateId;

    @Column(name = "aggregate_type", nullable = false, updatable = false)
    private String aggregateType;

    // Fully qualified event class name — used to deserialize `payload` back into
    // the right DomainEvent subtype. Kept separate from `aggregateType` on
    // purpose: the Library Provider version conflated "which aggregate" with
    // "which event" in a single overloaded `type` field, which made debugging
    // harder than it needed to be. Here they're two distinct columns.
    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;

    @Column(name = "version", nullable = false, updatable = false)
    private long version;

    @Column(name = "occurred_on", nullable = false, updatable = false)
    private LocalDateTime occurredOn;

    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    protected StoredEvent() {
        // JPA
    }

    public StoredEvent(
            String aggregateId,
            String aggregateType,
            String eventType,
            long version,
            LocalDateTime occurredOn,
            String payload
    ) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.version = version;
        this.occurredOn = occurredOn;
        this.payload = payload;
    }

    public Long getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public String getEventType() { return eventType; }
    public long getVersion() { return version; }
    public LocalDateTime getOccurredOn() { return occurredOn; }
    public String getPayload() { return payload; }
}
