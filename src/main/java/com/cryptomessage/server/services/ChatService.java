package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.config.exceptions.ForbiddenException;
import com.cryptomessage.server.model.dto.chat.ChatResponse;
import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.model.entity.contact.Contact;
import com.cryptomessage.server.model.entity.contact.ContactId;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.ChatMapper;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.ContactRepository;
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
    private final CurrentUserService currentUserService;
    private final ChatMapper chatMapper;
    private final ContactRepository contactRepository;

    public ChatService(
            ChatRepository chatRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ChatMapper chatMapper,
            ContactRepository contactRepository
    ) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.chatMapper = chatMapper;
        this.contactRepository = contactRepository;
    }

    /* ================= CREATE CHAT ================= */

    @Transactional
    public ChatResponse createChat(String username) {
        AppUser owner = currentUserService.get();
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
    public void acceptChat(Long chatId) {

        AppUser user = currentUserService.get();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.assertUserIsParticipant(user.getUserId());

        if (chat.getStatus() != ChatStatus.PENDING) {
            throw new ConflictException("Chat is not pending");
        }

        if (chat.getInitiatedBy().equals(user)) {
            throw new ForbiddenException("Initiator cannot accept their own chat");
        }

        // 🔥 aceptar chat
        chat.accept();

        // 🔥 obtener ambos usuarios
        AppUser user1 = chat.getAppUser1();
        AppUser user2 = chat.getAppUser2();

        // 🔥 crear contactos bidireccionales
        createContactIfNotExists(user1, user2);
        createContactIfNotExists(user2, user1);
    }

    /* ================= LIST CHATS ================= */

    @Transactional(readOnly = true)
    public List<ChatResponse> getMyChats(ChatStatus status) {
        AppUser owner = currentUserService.get();

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

    private void createContactIfNotExists(AppUser owner, AppUser contactUser) {

        ContactId id = new ContactId(owner.getUserId(), contactUser.getUserId());

        if (!contactRepository.existsById(id)) {
            Contact contact = new Contact(owner, contactUser);
            contactRepository.save(contact);
        }
    }
}
