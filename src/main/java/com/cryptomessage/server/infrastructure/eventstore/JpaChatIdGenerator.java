package com.cryptomessage.server.infrastructure.eventstore;

import com.cryptomessage.server.domain.chat.ChatId;
import com.cryptomessage.server.domain.chat.port.ChatIdGenerator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaChatIdGenerator implements ChatIdGenerator {

    private final ChatIdSequenceRepository sequenceRepository;

    public JpaChatIdGenerator(ChatIdSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Override
    // REQUIRES_NEW: id minting must commit (and the auto-increment value burn)
    // independently of the outer use-case transaction, otherwise a rollback
    // after minting an id would make that id unreachable to reuse — harmless,
    // just like a normal auto-increment gap, but only if this commits on its own.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatId nextId() {
        ChatIdSequence saved = sequenceRepository.save(new ChatIdSequence());
        return ChatId.of(String.valueOf(saved.getId()));
    }
}
