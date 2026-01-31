//package com.cryptomessage.server.services;
//
//import com.cryptomessage.server.config.exceptions.ConflictException;
//import com.cryptomessage.server.model.dto.security.authentication.AppUserResponseDTO;
//import com.cryptomessage.server.model.entity.user.AppUser;
//import com.cryptomessage.server.repositories.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.security.*;
//import java.util.NoSuchElementException;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AuthenticationServiceTest {
//    @Mock
//    private JwtService jwtService;
//
//    @Mock
//    private RSAEncryptionWithPassphraseService rsaEncryptionWithPassphraseService;
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private PasswordEncoder passwordEncoder;
//
//    @Mock
//    private AuthenticationManager authenticationManager;
//
//    @Mock
//    private UserDetailsServiceImp userDetailsServiceImp;
//
//    @InjectMocks
//    private AuthenticationService authenticationService;
//
//    // Claves dummy para pruebas
//    private KeyPair dummyKeyPair;
//
//    private AppUser mockAppUser;
//    private String username = "testUser";
//    private String passphrase = "testPassphrase";
//    private String hashedPassphrase = "hashedPassphrase";
//    private String token = "mockToken";
//    private String invalidToken = "mockInvalidToken";
//    private String encryptedPrivateKey= "encryptedPrivateKey";
//
//
//    @BeforeEach
//    void setup() throws Exception {
//        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
//        keyGen.initialize(2048);
//        dummyKeyPair = keyGen.generateKeyPair();
//
//        mockAppUser = AppUser.builder()
//                .username(username)
//                .passphraseHash(hashedPassphrase)
//                .publicKey(dummyKeyPair.getPublic())
//                .encryptedPrivateKey(encryptedPrivateKey)
//                .build();
//
//    }
//
//
//
//    @Test
//    void createUser_ShouldCreateUser_WhenUserDoesNotExist() throws Exception {
//        // Datos de entrada
//        String username = "testUser";
//        String passphrase = "securePassphrase";
//
//        // Mocks
//        when(userRepository.findUserByUsername(username)).thenReturn(Optional.empty());
//        when(passwordEncoder.encode(passphrase)).thenReturn("hashedPassphrase");
//        when(rsaEncryptionWithPassphraseService.generateKeyPair()).thenReturn(dummyKeyPair);
//        when(rsaEncryptionWithPassphraseService.savePrivateKeyWithPassphrase(any(), eq(passphrase)))
//                .thenReturn("encryptedPrivateKey");
//
//        // Ejecutar el método
//        authenticationService.createUser(username, passphrase);
//
//        // Verificar comportamiento
//        verify(userRepository, times(1)).save(any(AppUser.class));
//    }
//
//    @Test
//    void createUser_ShouldThrowConflictException_WhenUserAlreadyExists() throws Exception {
//        // Datos de entrada
//        String username = "existingUser";
//        String passphrase = "securePassphrase";
//
//        // Generar una clave pública dummy para el test
//        PublicKey dummyPublicKey = dummyKeyPair.getPublic();
//
//        // Mocks
//        when(userRepository.findUserByUsername(username))
//                .thenReturn(Optional.of(AppUser.builder()
//                        .username("existingUser")
//                        .passphraseHash("hashedPassphrase")
//                        .publicKey(dummyPublicKey) // Usar una clave pública válida
//                        .encryptedPrivateKey("encryptedPrivateKey")
//                        .build()));
//
//        // Ejecutar y verificar excepciones
//        assertThrows(ConflictException.class, () -> authenticationService.createUser(username, passphrase));
//    }
//
//    @Test
//    void authenticate_shouldReturnAppUserDTO_whenAuthenticationIsSuccessful() throws Exception {
//        // Mockear las dependencias
//        when(userRepository.findUserByUsername(username)).thenReturn(Optional.of(mockAppUser));
//
//        when(jwtService.generateToken(mockAppUser, passphrase)).thenReturn(token);
//        when(rsaEncryptionWithPassphraseService.decryptPrivateKey(anyString(), eq(passphrase))).thenReturn(dummyKeyPair.getPrivate());
//
//        // Ejecutar el método a probar
//        AppUserResponseDTO result = authenticationService.authenticate(username, passphrase);
//
//        // Verificar el comportamiento
//        assertEquals(username, result.getUsername());
//        assertEquals(token, result.getToken());
//        verify(userRepository, times(1)).findUserByUsername(username);
//        verify(jwtService, times(1)).generateToken(mockAppUser, passphrase);
//        verify(rsaEncryptionWithPassphraseService, times(1)).decryptPrivateKey(mockAppUser.getEncryptedPrivateKey(), passphrase);
//    }
//
//    @Test
//    void authenticate_shouldThrowException_whenUserNotFound() {
//        // Mockear usuario no encontrado
//        when(userRepository.findUserByUsername(username)).thenReturn(Optional.empty());
//
//        // Ejecutar el método y verificar excepción
//        Exception exception = assertThrows(NoSuchElementException.class, () ->
//                authenticationService.authenticate(username, passphrase)
//        );
//
//        assertEquals("User not found", exception.getMessage());
//        verify(userRepository, times(1)).findUserByUsername(username);
//        verifyNoInteractions(authenticationManager, jwtService, rsaEncryptionWithPassphraseService);
//    }
//
//    @Test
//    void authenticate_shouldThrowException_whenAuthenticationFails() {
//        // Mockear usuario encontrado
//        when(userRepository.findUserByUsername(username)).thenReturn(Optional.of(mockAppUser));
//
//        // Simular fallo de autenticación
//        doThrow(new RuntimeException("Authentication failed")).when(authenticationManager)
//                .authenticate(any(UsernamePasswordAuthenticationToken.class));
//
//        // Ejecutar el método y verificar excepción
//        assertThrows(RuntimeException.class, () ->
//                authenticationService.authenticate(username, "passphrase")
//        );
//
//        // Verificar que la autenticación fue llamada
//        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
//    }
//
//    @Test
//    void verifyToken_shouldReturnAppUserDTO_whenTokenIsValid() throws Exception {
//        // Mockear datos de prueba
//        UserDetails mockUserDetails = mock(UserDetails.class);
//
//        // Mockear dependencias del servicio
//        when(jwtService.extractUsername(token)).thenReturn(username);
//        when(jwtService.extractPassphrase(token)).thenReturn(passphrase);
//        when(userDetailsServiceImp.loadUserByAppUser(mockAppUser)).thenReturn(mockUserDetails);
//        when(jwtService.isTokenValid(eq(token), eq(mockUserDetails))).thenReturn(true);
//        when(userRepository.findUserByUsername(username)).thenReturn(Optional.of(mockAppUser));
//        when(rsaEncryptionWithPassphraseService.decryptPrivateKey(encryptedPrivateKey, passphrase))
//                .thenReturn(dummyKeyPair.getPrivate());
//
//        // Ejecutar el método
//        AppUserResponseDTO result = authenticationService.verifyToken(token);
//
//        // Verificar resultados
//        assertEquals(username, result.getUsername());
//        assertEquals(token, result.getToken());
//        assertEquals(
//                RSAEncryptionWithPassphraseService.privateKeyToString(dummyKeyPair.getPrivate()),
//                result.getPrivateKey()
//        );
//        verify(jwtService, times(1)).extractUsername(token);
//        verify(jwtService, times(1)).isTokenValid(eq(token), eq(mockUserDetails));
//        verify(userDetailsServiceImp, times(1)).loadUserByAppUser(mockAppUser);
//    }
//
//    @Test
//    void verifyToken_shouldThrowException_whenTokenIsInvalid() {
//        // Configurar mock para el token inválido
//        when(jwtService.extractUsername(invalidToken)).thenReturn(username);
//        when(userRepository.findUserByUsername(username)).thenReturn(Optional.of(mockAppUser));
//        when(userDetailsServiceImp.loadUserByAppUser(mockAppUser)).thenReturn(mock(UserDetails.class));
//        when(jwtService.isTokenValid(eq(invalidToken), any(UserDetails.class))).thenReturn(false);
//
//        // Ejecutar el método y verificar que se lanza una excepción
//        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
//            authenticationService.verifyToken(invalidToken);
//        });
//
//        // Validar el mensaje de la excepción
//        assertEquals("Invalid token", exception.getMessage());
//
//        // Verificar que los mocks se llamaron correctamente
//        verify(jwtService).extractUsername(invalidToken);
//        verify(userRepository).findUserByUsername(username);
//        verify(userDetailsServiceImp).loadUserByAppUser(mockAppUser);
//        verify(jwtService).isTokenValid(eq(invalidToken), any(UserDetails.class));
//    }
//
//}