package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    boolean existsByAppUser1AndAppUser2(
            AppUser appUser1,
            AppUser appUser2
    );
    List<Chat> findByAppUser1OrAppUser2(
            AppUser appUser1,
            AppUser appUser2
    );
    List<Chat> findByAppUser1OrAppUser2AndStatus(
            AppUser appUser1,
            AppUser appUser2,
            ChatStatus status
    );
    boolean existsByAppUser1AndAppUser2AndStatus(
            AppUser appUser1,
            AppUser appUser2,
            ChatStatus status
    );

    boolean existsByAppUser2AndAppUser1AndStatus(
            AppUser appUser1,
            AppUser appUser2,
            ChatStatus status
    );

    @Modifying
    @Query("""
    DELETE FROM Chat c
    WHERE c.createdAt < :limit
    AND c.messages IS EMPTY
    """)
    void deleteEmptyChatsOlderThan(LocalDateTime limit);

}
