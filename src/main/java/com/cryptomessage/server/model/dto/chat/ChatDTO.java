package com.cryptomessage.server.model.dto.chat;

import com.cryptomessage.server.model.dto.contact.ContactDTO;
import com.cryptomessage.server.model.dto.message.MessageDTO;
import com.cryptomessage.server.model.persistance.chat.Chat;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ChatDTO {
    private final Long chatId;
    private final List<ContactDTO> participants;
    private final List<MessageDTO> messages;

    public static ChatDTO from(Chat chat) {
        return new ChatDTO.Builder()
                .withChatId(chat.getChatId())
                .withParticipants(List.of(
                        new ContactDTO.Builder()
                                .withContactId(chat.getAppUser1().getUserId())
                                .withUsername(chat.getAppUser1().getUsername())
                                .withPublicKey(chat.getAppUser1().getPublicKey())
                                .build(),
                        new ContactDTO.Builder()
                                .withContactId(chat.getAppUser2().getUserId())
                                .withUsername(chat.getAppUser2().getUsername())
                                .withPublicKey(chat.getAppUser2().getPublicKey())
                                .build()
                ))
                .withMessages(chat.getMessages().stream()
                        .map(message -> new MessageDTO.Builder()
                                .withChatId(message.getChat().getChatId())
                                .withMessageId(message.getMessageId())
                                .withSenderId(message.getSender().getUserId())
                                .withSentAt(message.getSentAt().toString())
                                .withIsRead(message.getIsRead())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    // Constructor privado para forzar el uso del Builder
    private ChatDTO(Builder builder) {
        this.chatId = builder.chatId;
        this.participants = builder.participants != null ? Collections.unmodifiableList(builder.participants) : Collections.emptyList();
        this.messages = builder.messages != null ? Collections.unmodifiableList(builder.messages) : Collections.emptyList();
    }

    // Getters
    public Long getChatId() {
        return chatId;
    }

    public List<ContactDTO> getParticipants() {
        return participants;
    }

    public List<MessageDTO> getMessages() {
        return messages;
    }

    // Builder interno
    public static class Builder {
        private Long chatId;
        private List<ContactDTO> participants;
        private List<MessageDTO> messages;

        public Builder withChatId(Long chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder withParticipants(List<ContactDTO> participants) {
            this.participants = participants;
            return this;
        }

        public Builder withMessages(List<MessageDTO> messages) {
            this.messages = messages;
            return this;
        }

        public ChatDTO build() {
            return new ChatDTO(this);
        }
    }

    // Implementación de equals, hashCode y toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatDTO that = (ChatDTO) o;
        return Objects.equals(chatId, that.chatId) &&
                Objects.equals(participants, that.participants) &&
                Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, participants, messages);
    }

    @Override
    public String toString() {
        return "ChatResponse{" +
                "chatId=" + chatId +
                ", participants=" + participants +
                ", messages=" + messages +
                '}';
    }
}
