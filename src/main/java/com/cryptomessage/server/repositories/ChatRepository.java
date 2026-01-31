package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    boolean existsByAppUser1AndAppUser2(AppUser appUser1, AppUser appUser2);
    List<Chat> findByAppUser1OrAppUser2(AppUser appUser1, AppUser appUser2);
}
