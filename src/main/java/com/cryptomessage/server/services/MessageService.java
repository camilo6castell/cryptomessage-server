package com.cryptomessage.server.services;

import com.cryptomessage.server.model.dto.message.MessageDTO;
import com.cryptomessage.server.model.persistance.chat.Chat;
import com.cryptomessage.server.model.persistance.message.Message;
import com.cryptomessage.server.model.persistance.user.AppUser;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.MessageRepository;
import com.cryptomessage.server.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class MessageService {

    private static final Long SENDER_POSITION = 1L;
    private static final Long RECEIVER_POSITION = 2L;

    private final RSAEncryptionWithPassphraseService rsaEncryptionService;
    private final JwtService jwtService;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;

    public MessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            UserRepository userRepository,
            RSAEncryptionWithPassphraseService rsaEncryptionService,
            JwtService jwtService
    ) {
        this.rsaEncryptionService = rsaEncryptionService;
        this.jwtService = jwtService;
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
    }

    public MessageDTO sendMessage(Long chatId, Long senderId, String content) throws GeneralSecurityException {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        // Identificar remitente y receptor
        var sender = identifyUser(chat, senderId);
        var receiver = chat.getAppUser1().equals(sender) ? chat.getAppUser2() : chat.getAppUser1();

        // Cifrar el mensaje para ambos usuarios
        Map<Long, String> contentByUser = new HashMap<>();
        contentByUser.put(SENDER_POSITION, rsaEncryptionService.encrypt(content, sender.getPublicKey()));
        contentByUser.put(RECEIVER_POSITION, rsaEncryptionService.encrypt(content, receiver.getPublicKey()));

        // Guardar el mensaje
        Message savedMessage = messageRepository.save(
                Message.builder()
                        .withChat(chat)
                        .withSender(sender)
                        .withContentByUser(contentByUser)
                        .withSentAt(LocalDateTime.now())
                        .build()
        );

        return MessageDTO.from(savedMessage, contentByUser.get(SENDER_POSITION));
    }

    @Transactional
    public MessageDTO decryptMessage(Long userId, Long messageId, String token) throws Exception {
        // Buscar el mensaje
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NoSuchElementException("Message not found"));

        // Validar el chat
        Chat chat = chatRepository.findById(message.getChat().getChatId())
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        // Identificar al usuario
        var user = identifyUser(chat, userId);
        boolean isSender = message.getSender().equals(user);

        // Desencriptar la clave privada del usuario
        PrivateKey decryptedPrivateKey = rsaEncryptionService.decryptPrivateKey(
                user.getEncryptedPrivateKey(),
                jwtService.extractPassphrase(token.substring(7))
        );

        // Seleccionar contenido basado en el rol del usuario
        Long position = isSender ? SENDER_POSITION : RECEIVER_POSITION;
        String encryptedContent = message.getContentByUser().get(position);

        if (encryptedContent == null) {
            throw new IllegalArgumentException("No encrypted content found for user");
        }

        // Actualizar estado de lectura si es el destinatario
        if (!isSender && !message.getIsRead()) {
            message.setIsRead(true); // Marcar como leído
            messageRepository.save(message); // Persistir cambios
        }

        // Desencriptar el contenido
        try {
            String decryptedContent = rsaEncryptionService.decrypt(encryptedContent, decryptedPrivateKey);
            return MessageDTO.from(message, decryptedContent); // Devolver DTO
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting message", e);
        }
    }


    private AppUser identifyUser(Chat chat, Long userId) {
        if (chat.getAppUser1().getUserId().equals(userId)) {
            return chat.getAppUser1();
        } else if (chat.getAppUser2().getUserId().equals(userId)) {
            return chat.getAppUser2();
        } else {
            throw new IllegalArgumentException("User is not part of this chat");
        }
    }
}


//V0
//@Service
//public class MessageService {
//
//    private final RSAEncryptionWithPassphraseService rsaEncryptionWithPassphraseService;
//    private final JwtService jwtService;
//
//    private final MessageRepository messageRepository;
//    private final ChatRepository chatRepository;
//    private final UserRepository userRepository;
//
//    public MessageService(
//            MessageRepository messageRepository,
//            ChatRepository chatRepository,
//            UserRepository userRepository,
//            RSAEncryptionWithPassphraseService rsaEncryptionWithPassphraseService,
//            JwtService jwtService
//    ) {
//        this.rsaEncryptionWithPassphraseService = rsaEncryptionWithPassphraseService;
//        this.jwtService = jwtService;
//        this.messageRepository = messageRepository;
//        this.chatRepository = chatRepository;
//        this.userRepository = userRepository;
//    }
//
//    // estoy construyendo este método, necesito identificar al emisor y receptor y ponerlos en 0 y 1 respectivamente
//
//    public MessageDTO sendMessage(Long chatId, Long senderId, String content) throws GeneralSecurityException {
//        // Validar que el chat existe
//        Optional<Chat> chat = chatRepository.findById(chatId);
//        if (chat.isEmpty()) {
//            throw new NoSuchElementException("Chat not found");
//        }
//
//        Chat currentChat = chat.get();
//
////        Map<Long, String> contentByUser = new HashMap<>();
////        contentByUser.put(chat.get().getAppUser1().getUserId(), rsaEncryptionWithPassphraseService.encrypt(content, chat.get().getAppUser1().getPublicKey()));
////        contentByUser.put(chat.get().getAppUser2().getUserId(), rsaEncryptionWithPassphraseService.encrypt(content, chat.get().getAppUser2().getPublicKey()));
//
//        Map<Long, String> contentByUser = new HashMap<>();
//        contentByUser.put(
//                1L,
//                rsaEncryptionWithPassphraseService.encrypt(
//                        content,
//                        currentChat.getAppUser1().getUserId().equals(senderId) ?
//                                currentChat.getAppUser1().getPublicKey() : currentChat.getAppUser2().getPublicKey()
//                )
//        );
//        contentByUser.put(
//                2L,
//                rsaEncryptionWithPassphraseService.encrypt(
//                        content,
//                        currentChat.getAppUser2().getUserId().equals(senderId) ?
//                                currentChat.getAppUser2().getPublicKey() : currentChat.getAppUser1().getPublicKey()
//                )
//        );
//
//        return MessageDTO.from(
//                messageRepository.save(new Message.Builder()
//                        .withChat(chat.get())
//                        .withSender(
//                                currentChat
//                                        .getAppUser1()
//                                        .getUserId()
//                                        .equals(senderId) ? currentChat.getAppUser1() : currentChat.getAppUser2())
//                        .withContentByUser(contentByUser)
//                        .withSentAt(LocalDateTime.now())
//                        .build()),
//                contentByUser.get(
//                        currentChat.getAppUser1().getUserId().equals(senderId) ? 1L : 2L
//                )
//        );
//    }
//
//    public MessageDTO decryptMessage(Long appUserId, Long messageId, String token) {
//        return messageRepository.findById(messageId)
//                .map(message -> {
//                    Optional<Chat> currentChat = chatRepository.findById(message.getChat().getChatId());
//                    if (currentChat.isEmpty()) {
//                        throw new NoSuchElementException("Chat not found");
//                    }
//                    try {
//                        return MessageDTO.from(
//                                Message.builder()
//                                        .withChat(message.getChat())
//                                        .withMessageId(message.getMessageId())
//                                        .withSender(message.getSender())
//                                        .withSentAt(message.getSentAt())
//                                        .build(),
//                                rsaEncryptionWithPassphraseService.decrypt(
//                                        message.getContentByUser().get(
//                                                message.getSender().getUserId().equals(appUserId) ?
//                                                        1L : 2L),
//                                        rsaEncryptionWithPassphraseService.decryptPrivateKey(
//                                                message.getSender().getUserId().equals(appUserId) ?
//                                                        message.getSender().getEncryptedPrivateKey() :
//                                                        currentChat.get().getAppUser1().getUserId().equals(appUserId) ?
//                                                                currentChat.get().getAppUser2().getEncryptedPrivateKey() :
//                                                                currentChat.get().getAppUser1().getEncryptedPrivateKey(),
//                                                jwtService.extractPassphrase(token)
//                                        )
//                                )
//                        );
//
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                })
//                .orElseThrow(() -> new NoSuchElementException("Message not found"));
//    }
//}

