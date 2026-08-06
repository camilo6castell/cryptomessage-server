package com.cryptomessage.server.domain.generic;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages uncommitted domain events for a single aggregate instance and dispatches
 * them to registered behaviors, both for new events and for events replayed during
 * reconstruction.
 */
public class ChangeEventSubscriber {

    private final List<DomainEvent> domainEvents = Collections.synchronizedList(new LinkedList<>());
    private final Set<Consumer<? super DomainEvent>> subscribers = ConcurrentHashMap.newKeySet();

    public List<DomainEvent> events() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void markCommitted() {
        domainEvents.clear();
    }

    public void subscribe(EventChange eventChange) {
        subscribers.addAll(eventChange.getSubscribers());
    }

    /** Appends a new event to the uncommitted list and stamps it with the current time. */
    public DomainEvent appendEvent(DomainEvent domainEvent) {
        domainEvent.setOccurredOn(LocalDateTime.now());
        domainEvents.add(domainEvent);
        return domainEvent;
    }

    /** Dispatches an event (new or replayed) to every registered behavior. */
    public void applyEvent(DomainEvent domainEvent) {
        subscribers.forEach(consumer -> consumer.accept(domainEvent));
    }
}
