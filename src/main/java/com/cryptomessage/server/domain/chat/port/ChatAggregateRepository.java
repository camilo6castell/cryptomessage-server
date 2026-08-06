package com.cryptomessage.server.domain.chat.port;

import com.cryptomessage.server.domain.chat.Chat;
import com.cryptomessage.server.domain.chat.ChatId;

import java.util.Optional;

/**
 * Outbound port for the Chat aggregate's write side (event store). Deliberately
 * separate from the existing `repositories.ChatRepository` (Spring Data JPA),
 * which keeps serving reads against the projection table — see
 * infrastructure.projection.ChatProjector. Two repositories, two
 * responsibilities: this one is CQRS's "C", the JPA one is the "Q".
 */
public interface ChatAggregateRepository {

    Optional<Chat> findById(ChatId chatId);

    /**
     * Persists the aggregate's uncommitted events and marks them committed.
     *
     * @throws com.cryptomessage.server.config.exceptions.ConflictException if another
     *         process appended events to the same stream concurrently (optimistic
     *         concurrency conflict) — the caller should reload and retry.
     */
    void save(Chat chat);
}
