package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.ChatMapper;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.UserRepository;
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
    public ChatResponse createChat(String bearerToken, Long contactId) {

        AppUser owner = resolveUserFromToken(bearerToken);

        AppUser contact = userRepository.findById(contactId)
                .orElseThrow(() -> new NoSuchElementException("Contact not found"));

        validateNotSelfChat(owner, contact);

        if (chatExists(owner, contact)) {
            throw new ConflictException("Chat already exists");
        }

        Chat chat = chatRepository.save(new Chat(owner, contact));

        return chatMapper.toResponse(chat, owner);
    }

    /* ================= LIST CHATS ================= */

    @Transactional(readOnly = true)
    public List<ChatResponse> getMyChats(String bearerToken) {

        AppUser owner = resolveUserFromToken(bearerToken);

        return chatRepository
                .findByAppUser1OrAppUser2(owner, owner)
                .stream()
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
