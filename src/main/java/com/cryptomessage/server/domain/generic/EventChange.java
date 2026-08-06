package com.cryptomessage.server.domain.generic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Registers per-event-type subscribers (behaviors) that mutate an aggregate's state
 * when a domain event is applied to it — either freshly appended or replayed.
 */
public abstract class EventChange {

    private final Set<Consumer<? super DomainEvent>> subscribers = new HashSet<>();

    protected <T extends DomainEvent> void addSubscriber(Class<T> eventType, Consumer<T> handler) {
        subscribers.add(event -> {
            if (eventType.isInstance(event)) {
                handler.accept(eventType.cast(event));
            }
        });
    }

    public Set<Consumer<? super DomainEvent>> getSubscribers() {
        return Set.copyOf(subscribers);
    }
}
