package com.cryptomessage.server.model.dto.message;

import java.util.Objects;

public class SendMessageRequest {

    private Long chatId;
    private Long senderId;
    private String content;

    public SendMessageRequest() {
    }

    private SendMessageRequest(Builder builder) {
        this.chatId = Objects.requireNonNull(builder.chatId, "chatId no puede ser nulo");
        this.senderId = Objects.requireNonNull(builder.senderId, "senderId no puede ser nulo");
        this.content = Objects.requireNonNull(builder.content, "content no puede ser nulo o vacío").trim();

        if (this.content.isEmpty()) {
            throw new IllegalArgumentException("content no puede estar vacío");
        }
    }

    public Long getChatId() {
        return chatId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "SendMessageRequest{" +
                "chatId=" + chatId +
                ", senderId=" + senderId +
                ", content='" + content + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SendMessageRequest that = (SendMessageRequest) o;
        return Objects.equals(chatId, that.chatId) &&
                Objects.equals(senderId, that.senderId) &&
                Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, senderId, content);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long chatId;
        private Long senderId;
        private String content;

        private Builder() {}

        public Builder withChatId(Long chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder withSenderId(Long senderId) {
            this.senderId = senderId;
            return this;
        }   

        public Builder withContent(String content) {
            this.content = content;
            return this;
        }

        public SendMessageRequest build() {
            return new SendMessageRequest(this);
        }
    }
}

