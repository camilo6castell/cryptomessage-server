package com.cryptomessage.server.domain.generic;

import java.util.List;

/**
 * Base class for event-sourced aggregate roots.
 *
 * @param <I> identity type of the aggregate root
 */
public abstract class AggregateRoot<I extends Identity> extends Entity<I> {

    private final ChangeEventSubscriber changeEventSubscriber = new ChangeEventSubscriber();

    protected AggregateRoot(I id) {
        super(id);
    }

    public List<DomainEvent> getUncommittedChanges() {
        return List.copyOf(changeEventSubscriber.events());
    }

    public void markChangesAsCommitted() {
        changeEventSubscriber.markCommitted();
    }

    protected final void subscribe(EventChange eventChange) {
        changeEventSubscriber.subscribe(eventChange);
    }

    /** Replays a historical event onto the aggregate — no new uncommitted change is created. */
    protected void applyEvent(DomainEvent domainEvent) {
        changeEventSubscriber.applyEvent(domainEvent);
    }

    /** Registers a brand-new domain event and immediately applies it to update in-memory state. */
    protected DomainEvent appendEvent(DomainEvent domainEvent) {
        domainEvent.setAggregateRootId(identity().value());
        DomainEvent appended = changeEventSubscriber.appendEvent(domainEvent);
        changeEventSubscriber.applyEvent(appended);
        return appended;
    }
}
