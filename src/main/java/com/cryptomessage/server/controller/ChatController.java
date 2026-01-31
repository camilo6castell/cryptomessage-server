package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.dto.chat.CreateChatRequest;
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
            @RequestBody CreateChatRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        chatService.createChat(
                                httpRequest.getHeader("Authorization"),
                                request.getContactId()
                        )
                );
    }

    /* ================= LIST CHATS ================= */

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getMyChats(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(
                chatService.getMyChats(
                        request.getHeader("Authorization")
                )
        );
    }
}

