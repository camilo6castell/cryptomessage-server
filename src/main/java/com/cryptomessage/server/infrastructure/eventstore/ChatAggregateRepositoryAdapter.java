package com.cryptomessage.server.infrastructure.eventstore;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.domain.chat.Chat;
import com.cryptomessage.server.domain.chat.ChatId;
import com.cryptomessage.server.domain.chat.port.ChatAggregateRepository;
import com.cryptomessage.server.domain.generic.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed implementation of the Chat event store port.
 *
 * Version assignment happens HERE, not inside the aggregate — see the note on
 * DomainEvent about the version-tracking bug found in the ported Library
 * Provider framework. The adapter always asks the database how many events
 * already exist for this stream and assigns the next ones from that count, so
 * a stale in-memory aggregate can never silently overwrite history.
 */
@Component
public class ChatAggregateRepositoryAdapter implements ChatAggregateRepository {

    private static final Logger log = LoggerFactory.getLogger(ChatAggregateRepositoryAdapter.class);
    private static final String AGGREGATE_TYPE = "chat";

    private final StoredEventRepository storedEventRepository;
    private final EventSerializer eventSerializer;

    public ChatAggregateRepositoryAdapter(StoredEventRepository storedEventRepository, EventSerializer eventSerializer) {
        this.storedEventRepository = storedEventRepository;
        this.eventSerializer = eventSerializer;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findById(ChatId chatId) {
        List<StoredEvent> storedEvents = storedEventRepository.findByAggregateIdOrderByVersionAsc(chatId.value());
        if (storedEvents.isEmpty()) {
            return Optional.empty();
        }

        List<DomainEvent> events = storedEvents.stream()
                .map(stored -> {
                    DomainEvent event = eventSerializer.fromJson(stored.getPayload(), stored.getEventType());
                    event.setOccurredOn(stored.getOccurredOn());
                    event.setAggregateRootId(stored.getAggregateId());
                    return event;
                })
                .toList();

        return Optional.of(Chat.from(chatId.value(), events));
    }

    @Override
    @Transactional
    public void save(Chat chat) {
        List<DomainEvent> uncommitted = chat.getUncommittedChanges();
        if (uncommitted.isEmpty()) {
            return;
        }

        String aggregateId = chat.identity().value();
        long nextVersion = storedEventRepository.countByAggregateId(aggregateId) + 1;

        try {
            for (DomainEvent event : uncommitted) {
                StoredEvent storedEvent = new StoredEvent(
                        aggregateId,
                        AGGREGATE_TYPE,
                        event.getClass().getName(),
                        nextVersion,
                        event.getOccurredOn(),
                        eventSerializer.toJson(event)
                );
                storedEventRepository.save(storedEvent);
                nextVersion++;
            }
        } catch (DataIntegrityViolationException e) {
            // The unique (aggregate_id, version) constraint was violated: another
            // request appended to this same chat between our read and our write.
            log.warn("Concurrent write conflict on chat '{}'", aggregateId);
            throw new ConflictException("This chat was modified concurrently — please retry");
        }

        chat.markChangesAsCommitted();
    }
}
