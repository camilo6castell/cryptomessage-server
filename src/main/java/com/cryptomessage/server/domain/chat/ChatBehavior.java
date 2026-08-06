package com.cryptomessage.server.domain.chat;

import com.cryptomessage.server.domain.chat.events.ChatAccepted;
import com.cryptomessage.server.domain.chat.events.ChatCreated;
import com.cryptomessage.server.domain.chat.events.MessageSent;
import com.cryptomessage.server.domain.generic.EventChange;
import com.cryptomessage.server.domain.shared.UserId;
import com.cryptomessage.server.model.entity.chat.ChatStatus;

/**
 * Applies Chat's own domain events onto its in-memory state — both for freshly
 * appended events and for historical ones replayed during reconstruction.
 *
 * ChatRead is intentionally NOT subscribed here: no field on the write-side
 * aggregate depends on it. It only exists to drive the read-model projection.
 */
public class ChatBehavior extends EventChange {

    public ChatBehavior(Chat chat) {

        addSubscriber(ChatCreated.class, event -> {
            chat.setUser1Id(UserId.of(event.getUser1Id()));
            chat.setUser2Id(UserId.of(event.getUser2Id()));
            chat.setInitiatedBy(UserId.of(event.getInitiatedByUserId()));
            chat.setStatus(ChatStatus.PENDING);
        });

        // Only meaningful while PENDING — see the write-model note on Chat itself.
        addSubscriber(MessageSent.class, event -> {
            if (chat.getStatus() == ChatStatus.PENDING) {
                chat.markPendingMessageSent();
            }
        });

        addSubscriber(ChatAccepted.class, event -> chat.setStatus(ChatStatus.ACCEPTED));
    }
}
