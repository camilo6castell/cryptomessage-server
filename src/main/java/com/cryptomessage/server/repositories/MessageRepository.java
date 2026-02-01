package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.message.Message;
import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatOrderBySentAtAsc(Chat chat);

    @Modifying
    @Query("""
            UPDATE Message m
            SET m.isRead = true
            WHERE m.chat = :chat
            AND m.sender <> :user
            AND m.isRead = false
            """)
    void markAsReadByChatAndNotSender(Chat chat, AppUser user);

}
