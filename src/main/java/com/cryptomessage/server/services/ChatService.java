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
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
            ChatRepository chatRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ChatMapper chatMapper,
            ContactRepository contactRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.chatMapper = chatMapper;
        this.contactRepository = contactRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /* ================= CREATE CHAT ================= */

    public ChatResponse createChat(String username) {
        AppUser owner = currentUserService.get();
        AppUser otherUser = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (owner.getUserId().equals(otherUser.getUserId())) {
            throw new ConflictException("Cannot create chat with yourself");
        }

        try {
            Chat chat = chatRepository.save(new Chat(owner, otherUser, owner));

            messagingTemplate.convertAndSendToUser(
                    otherUser.getUsername(), "/queue/chats",
                    chatMapper.toResponse(chat, otherUser)
            );

            return chatMapper.toResponse(chat, owner);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Chat already exists");
        }
    }

    /* ================= ACCEPT CHAT ================= */

    public void acceptChat(Long chatId) {
        AppUser user = currentUserService.get();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        chat.assertUserIsParticipant(user.getUserId());

        if (chat.getStatus() != ChatStatus.PENDING) {
            throw new ConflictException("Chat is not pending");
        }

        if (chat.getInitiatedBy().getUserId().equals(user.getUserId())) {
            throw new ForbiddenException("Initiator cannot accept their own chat");
        }

        chat.accept();

        AppUser user1 = chat.getAppUser1();
        AppUser user2 = chat.getAppUser2();

        createContactIfNotExists(user1, user2);
        createContactIfNotExists(user2, user1);

        messagingTemplate.convertAndSendToUser(
                user1.getUsername(), "/queue/chats", chatMapper.toResponse(chat, user1)
        );
        messagingTemplate.convertAndSendToUser(
                user2.getUsername(), "/queue/chats", chatMapper.toResponse(chat, user2)
        );
    }

    /* ================= LIST CHATS ================= */

    @Transactional(readOnly = true)
    public List<ChatResponse> getMyChats(ChatStatus status) {
        AppUser owner = currentUserService.get();

        List<Chat> chats = (status == null)
                ? chatRepository.findByAppUser1OrAppUser2(owner, owner)
                : chatRepository.findByUserAndStatus(owner, status);

        return chats.stream()
                .map(chat -> chatMapper.toResponse(chat, owner))
                .toList();
    }

    /* ================= INTERNAL ================= */

    private void createContactIfNotExists(AppUser owner, AppUser contactUser) {
        ContactId id = new ContactId(owner.getUserId(), contactUser.getUserId());
        if (!contactRepository.existsById(id)) {
            contactRepository.save(new Contact(owner, contactUser));
        }
    }
}
