package com.cryptomessage.server.model.dto.contact;

/**
 * DTO de salida para representar un contacto.
 * Representa al "otro usuario" dentro de una relación Contact.
 */
public record ContactResponse(Long contactId, String username, String publicKey) {

}

