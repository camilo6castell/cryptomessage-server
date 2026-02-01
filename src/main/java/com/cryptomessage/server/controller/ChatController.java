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
            @RequestHeader("Authorization") String bearerToken,
            @RequestBody CreateChatRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createChat(bearerToken, request.getUsername()));
    }

    /* ================= LIST CHATS ================= */

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getMyChats(
            @RequestHeader("Authorization") String bearerToken,
            @RequestParam(required = false) ChatStatus status
    ) {
        return ResponseEntity.ok(
                chatService.getMyChats(bearerToken, status)
        );
    }

    /* ================= ACCEPT CHAT ================= */

    @PostMapping("/{chatId}/accept")
    public ResponseEntity<Void> acceptChat(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable Long chatId
    ) {
        chatService.acceptChat(bearerToken, chatId);
        return ResponseEntity.ok().build();
    }

    /* ================= BLOCK CHAT ================= */

    @PostMapping("/{chatId}/block")
    public ResponseEntity<Void> blockChat(
            @RequestHeader("Authorization") String bearerToken,
            @PathVariable Long chatId
    ) {
        chatService.blockChat(bearerToken, chatId);
        return ResponseEntity.ok().build();
    }
}


