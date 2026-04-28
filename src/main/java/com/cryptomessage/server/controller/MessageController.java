package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.dto.message.MessageResponse;
import com.cryptomessage.server.model.dto.message.SendMessageRequest;
import com.cryptomessage.server.services.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/messages")
@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /* ================= SEND MESSAGE ================= */

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestBody SendMessageRequest request
    ) throws Exception {

        MessageResponse response =
                messageService.sendMessage(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ================= GET MESSAGES ================= */

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<MessageResponse>> getMessagesByChat(
            @PathVariable Long chatId
    ) {

        return ResponseEntity.ok(
                messageService.getMessagesByChat(chatId)
        );
    }

    @PatchMapping("/chat/{chatId}/read")
    public ResponseEntity<Void> markChatAsRead(
            @PathVariable Long chatId
    ) {
        messageService.markChatAsRead(chatId);
        return ResponseEntity.noContent().build();
    }
}
