package com.cryptomessage.server.model.dto.message;

import com.cryptomessage.server.model.persistance.message.Message;

import java.util.Objects;

public class MessageDTO {
    private final Long chatId;
    private final Long messageId;
    private final Long senderId;
    private String content;
    private final String sentAt;
    private boolean isRead;

    // Constructor privado para el Builder
    private MessageDTO(Builder builder) {
        this.messageId = builder.messageId;
        this.senderId = builder.senderUsername;
        this.content = builder.content;
        this.sentAt = builder.sentAt;
        this.chatId = builder.chatId;
        this.isRead = builder.isRead;
    }

    // Método estático para obtener una instancia del builder FROM a message
    public static MessageDTO from(Message message, String content) {
        return new MessageDTO.Builder()
                .withChatId(message.getChat().getChatId())
                .withMessageId(message.getMessageId())
                .withSenderId(message.getSender().getUserId())
                .withContent(content)
                .withSentAt(message.getSentAt().toString())
                .withIsRead(message.getIsRead())
                .build();
    }

    // Getters

    public Long getChatId() {
        return chatId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public String getSentAt() {
        return sentAt;
    }

    public boolean getIsRead() {
        return isRead;
    }

    // Set


    public void setContent(String content) {
        this.content = content;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    // Builder interno
    public static class Builder {
        private Long chatId;
        private Long messageId;
        private Long senderUsername;
        private String content;
        private String sentAt;
        private boolean isRead;

        public Builder withMessageId(Long messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder withChatId(Long chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder withSenderId(Long senderUsername) {
            this.senderUsername = senderUsername;
            return this;
        }

        public Builder withContent(String content) {
            this.content = content;
            return this;
        }

        public Builder withSentAt(String sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public Builder withIsRead(boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public MessageDTO build() {
            // Validación opcional
            if (chatId == null || messageId == null || senderUsername == null || content == null || sentAt == null) {
                throw new IllegalStateException("All fields must be set");
            }
            return new MessageDTO(this);
        }
    }

    // Implementación de equals, hashCode y toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageDTO that = (MessageDTO) o;
        return Objects.equals(messageId, that.messageId) &&
                Objects.equals(chatId, that.chatId) &&
                Objects.equals(senderId, that.senderId) &&
                Objects.equals(content, that.content) &&
                Objects.equals(sentAt, that.sentAt) &&
                Objects.equals(isRead, that.isRead);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, messageId, senderId, content, sentAt, isRead);
    }

    @Override
    public String toString() {
        return "MessageResponse{" +
                "chatId=" + chatId +
                "messageId=" + messageId +
                ", senderUsername='" + senderId + '\'' +
                ", content='" + content + '\'' +
                ", sentAt='" + sentAt + '\'' +
                ", isRead='" + isRead + '\'' +
                '}';
    }
}

