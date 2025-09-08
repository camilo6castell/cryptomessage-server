package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.persistance.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findUserByUsername(String username);
    Optional<AppUser> findUserByUserId (Long id);

    // INTERESTING METHOD:
//    @Query("SELECT u FROM User u WHERE u.username = ?")
//    Optional<User> findByUSer(String username);
}
