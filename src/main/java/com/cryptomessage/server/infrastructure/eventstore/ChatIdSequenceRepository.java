package com.cryptomessage.server.infrastructure.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatIdSequenceRepository extends JpaRepository<ChatIdSequence, Long> {
}
