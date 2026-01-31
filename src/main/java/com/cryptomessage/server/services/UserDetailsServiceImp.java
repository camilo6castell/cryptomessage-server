package com.cryptomessage.server.services;

import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserDetailsServiceImp implements UserDetailsService {

    private static final List<GrantedAuthority> DEFAULT_AUTHORITIES =
            List.of((GrantedAuthority) () -> "USER");

    private final UserRepository userRepository;

    public UserDetailsServiceImp(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = userRepository.findUserByUsername(username)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found: " + username)
                );
        return toUserDetails(appUser);
    }

    public UserDetails loadUserByAppUser(AppUser appUser) {
        return toUserDetails(appUser);
    }

    private UserDetails toUserDetails(AppUser appUser) {
        return new User(
                appUser.getUsername(),
                appUser.getPassphraseHash(),
                true,
                true,
                true,
                true,
                DEFAULT_AUTHORITIES
        );
    }
}
