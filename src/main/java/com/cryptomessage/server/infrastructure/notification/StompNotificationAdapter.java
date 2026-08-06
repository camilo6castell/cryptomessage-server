package com.cryptomessage.server.infrastructure.notification;

import com.cryptomessage.server.domain.chat.port.NotificationPort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter for NotificationPort. Wraps the exact same SimpMessagingTemplate call
 * ChatService/MessageService made directly before — only its location changed,
 * not its behavior or the destinations the frontend already subscribes to
 * (/user/queue/messages, /user/queue/chats).
 */
@Component
public class StompNotificationAdapter implements NotificationPort {

    private final SimpMessagingTemplate messagingTemplate;

    public StompNotificationAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notifyUser(String username, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(username, destination, payload);
    }
}
