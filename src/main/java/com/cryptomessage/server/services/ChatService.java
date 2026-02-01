package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.config.exceptions.ForbiddenException;
import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.ChatMapper;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ChatMapper chatMapper;

    public ChatService(
            ChatRepository chatRepository,
            UserRepository userRepository,
            JwtService jwtService,
            ChatMapper chatMapper
    ) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.chatMapper = chatMapper;
    }

    /* ================= CREATE CHAT ================= */

    @Transactional
    public ChatResponse createChat(String bearerToken, String username) {
        AppUser owner = resolveUserFromToken(bearerToken);
        AppUser otherUser = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        validateNotSelfChat(owner, otherUser);

        try {
            Chat chat = chatRepository.save(new Chat(owner, otherUser, owner));
            return chatMapper.toResponse(chat, owner);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Chat already exists");
        }
    }

    @Transactional
    public void acceptChat(String bearerToken, Long chatId) {

        AppUser user = resolveUserFromToken(bearerToken);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.assertUserIsParticipant(user.getUserId());

        if (chat.getStatus() != ChatStatus.PENDING) {
            throw new ConflictException("Chat is not pending");
        }

        if (chat.getInitiatedBy().equals(user)) {
            throw new ForbiddenException("Initiator cannot accept their own chat");
        }

        chat.accept();
    }

    @Transactional
    public void blockChat(String bearerToken, Long chatId) {

        AppUser user = resolveUserFromToken(bearerToken);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.assertUserIsParticipant(user.getUserId());

        chat.block();
    }

    /* ================= LIST CHATS ================= */

    @Transactional(readOnly = true)
    public List<ChatResponse> getMyChats(String bearerToken, ChatStatus status) {
        AppUser owner = resolveUserFromToken(bearerToken);

        List<Chat> chats = (status == null)
                ? chatRepository.findByAppUser1OrAppUser2(owner, owner)
                : chatRepository.findByAppUser1OrAppUser2AndStatus(owner, owner, status);

        return chats.stream()
                .map(chat -> chatMapper.toResponse(chat, owner))
                .toList();
    }

    /* ================= INTERNAL ================= */

    private void validateNotSelfChat(AppUser user1, AppUser user2) {
        if (user1.equals(user2)) {
            throw new ConflictException("Cannot create chat with yourself");
        }
    }

    private boolean chatExists(AppUser user1, AppUser user2) {
        return chatRepository.existsByAppUser1AndAppUser2(user1, user2)
                || chatRepository.existsByAppUser1AndAppUser2(user2, user1);
    }

    private AppUser resolveUserFromToken(String bearerToken) {

        String token = jwtService.stripBearer(bearerToken);
        String username = jwtService.extractUsername(token);

        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
