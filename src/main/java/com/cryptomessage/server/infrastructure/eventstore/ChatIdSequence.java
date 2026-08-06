package com.cryptomessage.server.infrastructure.eventstore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Deliberately empty besides its id. Its only job is to hand out unique,
 * DB-generated Long values so ChatId can keep matching the Long type the
 * frontend already expects — see the note on Chat#create. Not a real domain
 * concept, purely an infrastructure compatibility shim.
 */
@Entity
@Table(name = "chat_id_sequences")
public class ChatIdSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }
}
