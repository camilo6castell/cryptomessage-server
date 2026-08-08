package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.entity.chat.Chat;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // FIX: la query derivada "findByAppUser1OrAppUser2AndStatus" se parsea como
    // "user1=X OR (user2=Y AND status=Z)", no como "(user1=X OR user2=Y) AND status=Z".
    // Se reemplaza con @Query explícita para garantizar la precedencia correcta.
    @Query("""
    SELECT c FROM Chat c
    WHERE (c.appUser1 = :user OR c.appUser2 = :user)
    AND c.status = :status
    """)
    List<Chat> findByUserAndStatus(
            @Param("user") AppUser user,
            @Param("status") ChatStatus status
    );

    @Query("""
    SELECT COUNT(c) > 0 FROM Chat c
    WHERE (c.appUser1 = :a AND c.appUser2 = :b)
       OR (c.appUser1 = :b AND c.appUser2 = :a)
    AND c.status = :status
    """)
    boolean existsByUsersAndStatus(
            @Param("a") AppUser a,
            @Param("b") AppUser b,
            @Param("status") ChatStatus status
    );

    @Modifying
    @Query("""
    DELETE FROM Chat c
    WHERE c.createdAt < :limit
    AND c.messages IS EMPTY
    """)
    void deleteEmptyChatsOlderThan(LocalDateTime limit);

    // Read-only counterpart to the query above, used by Scheduler to collect
    // aggregate ids before the rows are deleted, so their event streams can be
    // purged too. Same WHERE clause, kept in sync deliberately.
    @Query("""
    SELECT c FROM Chat c
    WHERE c.createdAt < :limit
    AND c.messages IS EMPTY
    """)
    List<Chat> findEmptyChatsOlderThan(LocalDateTime limit);

    // A chat can have at most one message while PENDING (see Chat#sendMessage's
    // invariant). If that single message is about to be purged by
    // deleteOldMessages, the chat is left "stuck": still PENDING, but with no
    // trace that a message was ever sent — the initiator could then send a
    // second "first" message, silently bypassing that invariant on replay.
    // Scheduler treats these exactly like abandoned chats: deletes the whole
    // thing now instead of leaving it in that inconsistent state until
    // deleteEmptyChats eventually catches it, days later. See
    // apuntes/05-decisiones-de-diseno/03-chats-pendientes-abandonados.md.
    @Query("""
    SELECT DISTINCT c FROM Chat c
    JOIN c.messages m
    WHERE c.status = :status
    AND m.sentAt < :limit
    """)
    List<Chat> findChatsByStatusWithMessagesOlderThan(
            @Param("status") ChatStatus status,
            @Param("limit") LocalDateTime limit
    );
}