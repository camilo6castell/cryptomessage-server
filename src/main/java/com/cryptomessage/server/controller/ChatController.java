package com.cryptomessage.server.controller;

import com.cryptomessage.server.application.chat.AcceptChatUseCase;
import com.cryptomessage.server.application.chat.CreateChatUseCase;
import com.cryptomessage.server.application.chat.GetMyChatsUseCase;
import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.dto.chat.CreateChatRequest;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {

    private final CreateChatUseCase createChatUseCase;
    private final GetMyChatsUseCase getMyChatsUseCase;
    private final AcceptChatUseCase acceptChatUseCase;

    public ChatController(
            CreateChatUseCase createChatUseCase,
            GetMyChatsUseCase getMyChatsUseCase,
            AcceptChatUseCase acceptChatUseCase
    ) {
        this.createChatUseCase = createChatUseCase;
        this.getMyChatsUseCase = getMyChatsUseCase;
        this.acceptChatUseCase = acceptChatUseCase;
    }

    /* ================= CREATE CHAT ================= */

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(
            @RequestBody CreateChatRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createChatUseCase.execute(request.username()));
    }

    /* ================= LIST CHATS ================= */

    @GetMapping
    public ResponseEntity<List<ChatResponse>> getMyChats(
            @RequestParam(required = false) ChatStatus status
    ) {
        return ResponseEntity.ok(
                getMyChatsUseCase.execute(status)
        );
    }

    /* ================= ACCEPT CHAT ================= */

    @PostMapping("/{chatId}/accept")
    public ResponseEntity<Void> acceptChat(
            @PathVariable Long chatId
    ) {
        acceptChatUseCase.execute(chatId);
        return ResponseEntity.ok().build();
    }
}
