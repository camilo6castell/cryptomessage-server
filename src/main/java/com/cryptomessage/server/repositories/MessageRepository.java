package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatOrderBySentAtAsc(Chat chat);
}
