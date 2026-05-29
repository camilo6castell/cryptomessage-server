package com.cryptomessage.server.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthConfig {

    // UserDetailsServiceImpl se registra como bean vía @Service en la propia clase.
    // No se redeclara aquí: hacerlo crea dos beans del mismo tipo
    // (userDetailsServiceImpl + userDetailsService), lo que confunde al
    // auto-config de Spring Boot y genera el warning
    // "Found 2 UserDetailsService beans".
    //
    // Con un único UserDetailsService bean + PasswordEncoder bean en el contexto,
    // Spring Boot auto-configura el DaoAuthenticationProvider internamente,
    // sin necesidad de declararlo como @Bean — que era la causa del warning anterior.

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
