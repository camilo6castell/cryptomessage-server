package com.cryptomessage.server.config.websocket;

import com.cryptomessage.server.services.JwtService;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * The WebSocket handshake itself is anonymous (permitAll — see SecurityConfig),
 * because SockJS/raw-WS handshakes cannot reliably carry custom headers from the browser.
 * Real authentication happens here, on the STOMP CONNECT frame, where the client
 * sends the JWT as a native "Authorization" header. This mirrors JwtAuthenticationFilter's
 * validation logic so there is a single source of truth for what makes a token valid.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public StompAuthChannelInterceptor(
            JwtService jwtService,
            UserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new MessagingAuthenticationException("Missing Authorization header on CONNECT");
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new MessagingAuthenticationException("Invalid or expired token");
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

            accessor.setUser(authToken);
        } catch (MessagingAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new MessagingAuthenticationException("WebSocket authentication failed", e);
        }

        return message;
    }

    /**
     * Thrown when a STOMP CONNECT frame fails authentication.
     * Kept internal to this package — callers should not need to catch it individually;
     * Spring's STOMP error handling converts it into an ERROR frame and closes the session.
     */
    static class MessagingAuthenticationException extends RuntimeException {
        MessagingAuthenticationException(String message) {
            super(message);
        }

        MessagingAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
