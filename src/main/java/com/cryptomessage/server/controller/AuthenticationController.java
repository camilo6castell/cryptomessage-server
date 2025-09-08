package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.dto.security.authentication.AuthenticationRequest;
import com.cryptomessage.server.model.dto.security.authentication.VerifyTokenRequest;
import com.cryptomessage.server.model.dto.security.register.RegisterRequest;
import com.cryptomessage.server.services.AuthenticationService;
import org.bouncycastle.operator.OperatorCreationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(
            AuthenticationService authenticationService
    ) {
        this.authenticationService = authenticationService;
    }

    //    @CrossOrigin(origins = "http://localhost:5173/")
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestBody RegisterRequest registerRequest
    ) throws
            NoSuchAlgorithmException,
            IOException,
            NoSuchProviderException,
            OperatorCreationException {
        authenticationService.createUser(
                registerRequest.getUsername(),
                registerRequest.getPassphrase()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .build();
    }

    //    @CrossOrigin(origins = "http://localhost:5173/")
    @PostMapping("/authenticate")
    public ResponseEntity<?> login(
            @RequestBody AuthenticationRequest authenticationRequest
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(authenticationService.authenticate(
                        authenticationRequest.getUsername(),
                        authenticationRequest.getPassphrase()
                ));
    }

    //    @CrossOrigin(origins = "http://localhost:5173/")
    @PostMapping("/verify-token")
    public ResponseEntity<?> verifyToken(
            @RequestBody VerifyTokenRequest verifyTokenRequest
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(authenticationService.verifyToken(
                        verifyTokenRequest.getToken()
                ));

    }

}
