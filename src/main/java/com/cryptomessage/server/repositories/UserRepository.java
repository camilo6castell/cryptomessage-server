package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findUserByUsername(String username);
    Optional<AppUser> findUserByUserId (Long id);

    @Modifying
    @Query("DELETE FROM AppUser u WHERE u.lastSeen < :limit")
    void deleteInactiveUsers(LocalDateTime limit);
}
