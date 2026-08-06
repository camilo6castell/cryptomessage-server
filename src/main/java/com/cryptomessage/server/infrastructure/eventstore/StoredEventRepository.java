package com.cryptomessage.server.infrastructure.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StoredEventRepository extends JpaRepository<StoredEvent, Long> {

    List<StoredEvent> findByAggregateIdOrderByVersionAsc(String aggregateId);

    long countByAggregateId(String aggregateId);

    // Used by the retention scheduler to purge old MessageSent events while
    // keeping ChatCreated/ChatAccepted for the chat's lifetime — mirrors the
    // original MessageRepository#deleteOlderThan behavior. See Scheduler.
    void deleteByEventTypeAndOccurredOnBefore(String eventType, LocalDateTime limit);

    void deleteByAggregateId(String aggregateId);
}
