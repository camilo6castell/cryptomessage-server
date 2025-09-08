package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.persistance.chat.Chat;
import com.cryptomessage.server.services.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // Endpoint para crear un nuevo chat entre dos usuarios
    @PostMapping("/create-chat")
    public ResponseEntity<?> createChat(@RequestParam Long user1Id, @RequestParam Long user2Id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createChat(user1Id, user2Id));
    }
}
