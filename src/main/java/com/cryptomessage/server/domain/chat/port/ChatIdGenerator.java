package com.cryptomessage.server.domain.chat.port;

import com.cryptomessage.server.domain.chat.ChatId;

/**
 * Mints a fresh, unique ChatId. A separate port from ChatAggregateRepository
 * because id generation needs a DB round-trip today (see the JPA adapter) but
 * is conceptually a different responsibility — a future adapter (e.g. a proper
 * distributed id generator) could implement this without touching how events
 * are stored.
 */
public interface ChatIdGenerator {
    ChatId nextId();
}
