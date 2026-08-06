package com.cryptomessage.server.controller;

import com.cryptomessage.server.application.message.GetMessagesByChatUseCase;
import com.cryptomessage.server.application.message.MarkChatAsReadUseCase;
import com.cryptomessage.server.application.message.SendMessageUseCase;
import com.cryptomessage.server.model.dto.message.MessageResponse;
import com.cryptomessage.server.model.dto.message.SendMessageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/messages")
@RestController
public class MessageController {

    private final SendMessageUseCase sendMessageUseCase;
    private final GetMessagesByChatUseCase getMessagesByChatUseCase;
    private final MarkChatAsReadUseCase markChatAsReadUseCase;

    public MessageController(
            SendMessageUseCase sendMessageUseCase,
            GetMessagesByChatUseCase getMessagesByChatUseCase,
            MarkChatAsReadUseCase markChatAsReadUseCase
    ) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.getMessagesByChatUseCase = getMessagesByChatUseCase;
        this.markChatAsReadUseCase = markChatAsReadUseCase;
    }

    /* ================= SEND MESSAGE ================= */

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestBody SendMessageRequest request
    ) {
        MessageResponse response = sendMessageUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /* ================= GET MESSAGES ================= */

    @GetMapping("/chat/{chatId}")
    public ResponseEntity<List<MessageResponse>> getMessagesByChat(
            @PathVariable Long chatId
    ) {
        return ResponseEntity.ok(
                getMessagesByChatUseCase.execute(chatId)
        );
    }

    @PatchMapping("/chat/{chatId}/read")
    public ResponseEntity<Void> markChatAsRead(
            @PathVariable Long chatId
    ) {
        markChatAsReadUseCase.execute(chatId);
        return ResponseEntity.noContent().build();
    }
}
