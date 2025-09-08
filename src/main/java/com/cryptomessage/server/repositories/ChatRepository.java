package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.persistance.chat.Chat;
import com.cryptomessage.server.model.persistance.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    boolean existsByAppUser1AndAppUser2(AppUser appUser1, AppUser appUser2);
//    List<Chat> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
}
