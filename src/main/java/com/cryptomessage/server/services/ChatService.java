package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.model.dto.chat.ChatDTO;
import com.cryptomessage.server.model.persistance.chat.Chat;
import com.cryptomessage.server.model.persistance.user.AppUser;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public ChatService(ChatRepository chatRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    public ChatDTO createChat(Long user1Id, Long user2Id) {
        AppUser appUser1 = findUserByIdOrThrow(user1Id);
        AppUser appUser2 = findUserByIdOrThrow(user2Id);

        if (chatExists(appUser1, appUser2)) {
            throw new ConflictException("Chat already exists between the specified users.");
        }

        Chat newChat = Chat.builder()
                .withAppUser1(appUser1)
                .withAppUser2(appUser2)
                .build();

        return ChatDTO.from(chatRepository.save(newChat));
    }

    private AppUser findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User with ID " + userId + " not found"));
    }

    private boolean chatExists(AppUser user1, AppUser user2) {
        return chatRepository.existsByAppUser1AndAppUser2(user1, user2) ||
                chatRepository.existsByAppUser1AndAppUser2(user2, user1);
    }
}


//V0
//@Service
//public class ChatService {
//
//    private final ChatRepository chatRepository;
//    private final UserRepository userRepository;
//
//    public ChatService(ChatRepository chatRepository, UserRepository userRepository) {
//        this.chatRepository = chatRepository;
//        this.userRepository = userRepository;
//    }
//
//    public ResponseEntity<?> createChat(Long user1Id, Long user2Id) {
//        // Validamos que ambos usuarios existen
//        Optional<AppUser> appUser1 = userRepository.findById(user1Id);
//        Optional<AppUser> appUser2 = userRepository.findById(user2Id);
//
//        if (appUser1.isEmpty() || appUser2.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//
//        // Verificamos si ya existe un chat entre user1 y user2
//        boolean chatExists = chatRepository.existsByAppUser1AndAppUser2(appUser1.get(), appUser2.get()) ||
//                chatRepository.existsByAppUser1AndAppUser2(appUser2.get(), appUser1.get());
//
//        if (chatExists) {
//            return ResponseEntity.status(HttpStatus.CONFLICT).build();
//        }
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(ChatDTO.from(chatRepository.save(Chat.builder()
//                .withAppUser1(appUser1.get())
//                .withAppUser2(appUser2.get())
//                .withCreatedAt(null)
//                .build())));
//    }
//}

