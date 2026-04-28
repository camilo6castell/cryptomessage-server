package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.dto.chat.CreateChatRequest;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.services.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /* ================= CREATE CHAT ================= */

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(
            @RequestBody CreateChatRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createChat(request.getUsername()));
    }

    /* ================= LIST CHATS ================= */

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getMyChats(
            @RequestParam(required = false) ChatStatus status
    ) {
        return ResponseEntity.ok(
                chatService.getMyChats(status)
        );
    }

    /* ================= ACCEPT CHAT ================= */

    @PostMapping("/{chatId}/accept")
    public ResponseEntity<Void> acceptChat(
            @PathVariable Long chatId
    ) {
        chatService.acceptChat(chatId);
        return ResponseEntity.ok().build();
    }
}


