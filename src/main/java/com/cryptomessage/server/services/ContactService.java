package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.config.exceptions.ForbiddenException;
import com.cryptomessage.server.model.dto.contact.ContactResponse;
import com.cryptomessage.server.model.entity.chat.ChatStatus;
import com.cryptomessage.server.model.entity.contact.Contact;
import com.cryptomessage.server.model.entity.contact.ContactId;
import com.cryptomessage.server.model.entity.user.AppUser;
import com.cryptomessage.server.model.mapper.ContactMapper;
import com.cryptomessage.server.repositories.ChatRepository;
import com.cryptomessage.server.repositories.ContactRepository;
import com.cryptomessage.server.repositories.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ContactService {

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final JwtService jwtService;
    private final RSAKeyConverterService RSAKeyConverterService;
    private final ContactMapper contactMapper;
    private final ChatRepository chatRepository;

    public ContactService(
            UserRepository userRepository,
            ContactRepository contactRepository,
            JwtService jwtService,
            ContactMapper contactMapper,
            RSAKeyConverterService RSAKeyConverterService,
            ChatRepository chatRepository
    ) {
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.jwtService = jwtService;
        this.contactMapper = contactMapper;
        this.RSAKeyConverterService = RSAKeyConverterService;
        this.chatRepository = chatRepository;
    }

    /* ================= SEARCH USER ================= */

    @Transactional(readOnly = true)
    public ContactResponse searchUserByUsername(String username) {

        AppUser user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return new ContactResponse(
                user.getUserId(),
                user.getUsername(),
                RSAKeyConverterService.publicKeyToString(user.getPublicKey())
        );
    }

    /* ================= LIST CONTACTS ================= */

    @Transactional(readOnly = true)
    public List<ContactResponse> getContacts(String bearerToken) {

        AppUser owner = resolveUserFromToken(bearerToken);

        return owner.getContacts().stream()
                .map(contactMapper::toResponse)
                .toList();
    }

    /* ================= ADD CONTACT ================= */

    public void addContact(String bearerToken, Long contactId) {

        AppUser owner = resolveUserFromToken(bearerToken);

        AppUser contactUser = userRepository.findById(contactId)
                .orElseThrow(() -> new NoSuchElementException("Contact user not found"));

        if (owner.getUserId().equals(contactUser.getUserId())) {
            throw new IllegalArgumentException("Cannot add yourself as contact");
        }

        boolean existsAcceptedChat =
                chatRepository.existsByAppUser1AndAppUser2AndStatus(owner, contactUser, ChatStatus.ACCEPTED)
                        || chatRepository.existsByAppUser2AndAppUser1AndStatus(owner, contactUser, ChatStatus.ACCEPTED);

        if (!existsAcceptedChat) {
            throw new ForbiddenException("Cannot add contact without accepted chat");
        }

        ContactId id = new ContactId(owner.getUserId(), contactUser.getUserId());

        if (contactRepository.existsById(id)) {
            throw new ConflictException("Contact already exists");
        }

        Contact contact = new Contact(owner, contactUser);
        contactRepository.save(contact);
    }

    /* ================= REMOVE CONTACT ================= */

    public void removeContact(String bearerToken, Long contactId) {

        AppUser owner = resolveUserFromToken(bearerToken);

        ContactId id = new ContactId(owner.getUserId(), contactId);

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact not found"));

        contactRepository.delete(contact);
    }

    /* ================= INTERNAL ================= */

    private AppUser resolveUserFromToken(String bearerToken) {

        String token = jwtService.stripBearer(bearerToken);
        String username = jwtService.extractUsername(token);

        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
