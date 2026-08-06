package com.cryptomessage.server.domain.chat.port;

/**
 * Outbound port for pushing real-time updates to a connected client. The only
 * implementation today is a thin STOMP/WebSocket adapter
 * (infrastructure.notification.StompNotificationAdapter), but use cases don't
 * need to know that — this is what actually makes the transport swappable.
 */
public interface NotificationPort {
    void notifyUser(String username, String destination, Object payload);
}
