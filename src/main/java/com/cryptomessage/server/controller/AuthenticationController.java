package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.dto.security.authentication.AuthenticationRequest;
import com.cryptomessage.server.model.dto.security.authentication.AuthenticationResponse;
import com.cryptomessage.server.model.dto.security.authentication.LoginResponse;
import com.cryptomessage.server.model.dto.security.register.RegisterRequest;
import com.cryptomessage.server.services.AuthenticationService;
import com.cryptomessage.server.services.UserRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/auth")
@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserRegistrationService userRegistrationService;

    public AuthenticationController(
            AuthenticationService authenticationService,
            UserRegistrationService userRegistrationService
    ) {
        this.authenticationService = authenticationService;
        this.userRegistrationService = userRegistrationService;
    }

    /* ================= REGISTER ================= */

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestBody RegisterRequest request
    ) throws Exception {

        userRegistrationService.createUser(
                request.getUsername(),
                request.getPassphrase()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /* ================= LOGIN ================= */

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody AuthenticationRequest request
    ) throws Exception {

        return ResponseEntity.ok(
                authenticationService.authenticate(
                        request.getUsername(),
                        request.getPassphrase()
                )
        );
    }

    /* ================= TOKEN VERIFY ================= */

    @GetMapping("/verify")
    public ResponseEntity<AuthenticationResponse> verifyToken(
            @RequestHeader("Authorization") String bearerToken
    ) {

        return ResponseEntity.ok(
                authenticationService.verifyToken(bearerToken)
        );
    }
}
