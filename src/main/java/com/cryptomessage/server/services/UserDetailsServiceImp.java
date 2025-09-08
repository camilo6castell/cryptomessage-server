package com.cryptomessage.server.services;

import com.cryptomessage.server.model.persistance.user.AppUser;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImp implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImp(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username){
        AppUser appUser = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        /* this should be implemented in a better way granting authorities saved in the database (and implementing
        the UserDetails interface in [or related] to the entity class) */
        return new User(
                appUser.getUsername(),
                appUser.getPassphraseHash(),
                true,
                true,
                true,
                true,
                List.of(
                        (GrantedAuthority) () -> "USER"
                )
        );
    }

    public UserDetails loadUserByAppUser(AppUser appUser){
        /* this should be implemented in a better way granting authorities saved in the database (and implementing
        the UserDetails interface in [or related] to the entity class) */
        return new User(
                appUser.getUsername(),
                appUser.getPassphraseHash(),
                true,
                true,
                true,
                true,
                List.of(
                        (GrantedAuthority) () -> "USER"
                )
        );
    }

//    This is a simple implementation of the UserDetails interface
//    private final User user;
//
//    public UserDetailsServiceImp(User user) {
//        this.user = user;
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        //return List.of();
//        return null;
//    }
//
//    @Override
//    public String getPassword() {
//        return user.getPasswordHash();
//    }
//
//    @Override
//    public String getUsername() {
//        return user.getUsername();
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return UserDetails.super.isAccountNonExpired();
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return UserDetails.super.isAccountNonLocked();
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return UserDetails.super.isCredentialsNonExpired();
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return UserDetails.super.isEnabled();
//    }
}


