package com.cryptomessage.server.controller;

import com.cryptomessage.server.config.errors.ApiError;
import com.cryptomessage.server.config.exceptions.ConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Excepción de entrada/salida
    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiError> handleIOException(IOException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_SERVER_ERROR", ex.getMessage(), Instant.now()));
    }

    // Excepción para argumentos inválidos en las solicitudes
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String errorMessage =
                "Invalid argument type: " +
                ex.getName() +
                " should be of type " +
                Objects.requireNonNull(ex.getRequiredType()).getSimpleName();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(errorMessage, ex.getMessage(), Instant.now()));
    }

    // Excepción para parámetros faltantes en las solicitudes
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String errorMessage = "Missing request parameter: " + ex.getParameterName();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(errorMessage, ex.getMessage(), Instant.now()));
    }

    // Excepción de autenticación: manejar errores de credenciales incorrectas o acceso no autorizado
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("Authentication failed: ", ex.getMessage(), Instant.now()));
    }

    // //////// OWN

   // Excepción token inválido
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("Invalid token: ", ex.getMessage(), Instant.now()));
    }

    // Excepción de credenciales incorrectas
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ApiError> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("Bad credentials: ", ex.getMessage(), Instant.now()));
    }

    // Excepción de recurso no encontrado en la base de datos
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiError> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError("Resource not found: ", ex.getMessage(), Instant.now()));
    }

    // ////////  Excepciones personalizadas

    // ya existe el recurso
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("Conflict: ", ex.getMessage(), Instant.now()));
    }

    // Excepción genérica: manejar cualquier otra excepción no capturada anteriormente
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    public ResponseEntity<ApiError> handleGeneralException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.I_AM_A_TEAPOT)
                .body(
                        new ApiError("An unexpected & new error occurred: ",
                                ex.getMessage(),
                                Instant.now()
                        )
                );
    }
}

