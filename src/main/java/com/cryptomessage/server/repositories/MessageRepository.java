package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.persistance.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
//    List<Message> findByChatId(Long chatId);
}
