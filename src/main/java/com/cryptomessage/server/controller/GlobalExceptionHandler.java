package com.cryptomessage.server.controller;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.config.exceptions.ForbiddenException;
import com.cryptomessage.server.domain.chat.exceptions.ChatDomainException;
import com.cryptomessage.server.domain.chat.exceptions.ChatNotAcceptedException;
import com.cryptomessage.server.domain.chat.exceptions.ChatNotPendingException;
import com.cryptomessage.server.domain.chat.exceptions.MessageLimitExceededException;
import com.cryptomessage.server.domain.chat.exceptions.SelfAcceptanceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 400 Bad Request ──────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter '" + ex.getName()
                + "': expected " + Objects.requireNonNull(ex.getRequiredType()).getSimpleName();
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, "Missing parameter: " + ex.getParameterName());
    }

    // ─── 401 Unauthorized ─────────────────────────────────────────────────────

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    // ─── 403 Forbidden ────────────────────────────────────────────────────────

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // Same status the original service-layer ForbiddenException used for these
    // two cases — see ChatNotAcceptedException / MessageLimitExceededException /
    // SelfAcceptanceException javadoc for the original message they replace.
    @ExceptionHandler({
            ChatNotAcceptedException.class,
            MessageLimitExceededException.class,
            SelfAcceptanceException.class
    })
    public ResponseEntity<ApiError> handleChatForbidden(ChatDomainException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // ─── 404 Not Found ────────────────────────────────────────────────────────

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNotFound(NoSuchElementException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ─── 409 Conflict ─────────────────────────────────────────────────────────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Same status the original ConflictException("Chat is not pending") used.
    @ExceptionHandler(ChatNotPendingException.class)
    public ResponseEntity<ApiError> handleChatNotPending(ChatNotPendingException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Catch-all for the remaining Chat invariant violations (not participant,
    // same user, malformed encrypted content) — same status the original
    // IllegalArgumentException-based checks used. Kept last among the domain
    // handlers, and below the more specific ones above, so it never shadows them.
    @ExceptionHandler(ChatDomainException.class)
    public ResponseEntity<ApiError> handleChatDomainException(ChatDomainException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ─── 500 Internal Server Error ────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        log.error("Unhandled exception in request", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(status.name(), message, Instant.now()));
    }

    public record ApiError(String error, String message, Instant timestamp) {}
}