package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.dto.message.SendMessageRequest;
import com.cryptomessage.server.services.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.GeneralSecurityException;

@RestController
@RequestMapping("/api/v1/message")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // Endpoint para enviar un mensaje
    @PostMapping("/send-message")
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageRequest sendMessageRequest)
            throws
            GeneralSecurityException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(
                        sendMessageRequest.getChatId(),
                        sendMessageRequest.getSenderId(),
                        sendMessageRequest.getContent()
                ));
    }

    @GetMapping("/decrypt-message/{appUserId}/{messageId}")
    public ResponseEntity<?> decryptMessage(
            @PathVariable
            Long appUserId,
            @PathVariable
            Long messageId,
            HttpServletRequest request
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.OK)
                .body(messageService.decryptMessage(
                        appUserId,
                        messageId,
                        request.getHeader("Authorization")
                ));
    }
}
