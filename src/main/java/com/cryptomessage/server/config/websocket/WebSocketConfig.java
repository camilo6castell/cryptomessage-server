package com.cryptomessage.server.config.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * Real-time channel used to push new messages and chat status changes to connected
 * clients, replacing the previous "reload to see anything new" flow.
 *
 * The server remains message-blind here too: it only ever relays the same opaque
 * ciphertext blobs it already stores — nothing new is decrypted or inspected to make
 * this work.
 *
 * Endpoint: /ws (raw WebSocket, no SockJS — modern browsers and @stomp/stompjs
 * support native WebSocket directly, so we avoid the extra long-polling fallback
 * complexity SockJS brings for a private app with no legacy-browser requirement).
 *
 * Destinations:
 *   /user/queue/messages  -> new message events (per-user)
 *   /user/queue/chats     -> chat created/accepted/status events (per-user)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(resolveAllowedOrigins());
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    private String[] resolveAllowedOrigins() {
        String allowedOrigin = System.getenv("CORS_ALLOWED_ORIGINS");

        List<String> patterns = (allowedOrigin != null && !allowedOrigin.isBlank())
                ? Arrays.stream(allowedOrigin.split(",")).map(String::trim).toList()
                : List.of("*");

        return patterns.toArray(new String[0]);
    }
}
