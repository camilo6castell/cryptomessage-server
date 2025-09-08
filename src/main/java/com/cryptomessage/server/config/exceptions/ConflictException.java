package com.cryptomessage.server.config.exceptions;

// Excepción lanzada cuando ocurre un conflicto (por ejemplo, al agregar un contacto ya existente)
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}