package com.cryptomessage.server.services;

import com.cryptomessage.server.config.exceptions.ConflictException;
import com.cryptomessage.server.model.dto.contact.ContactDTO;
import com.cryptomessage.server.model.persistance.contact.Contact;
import com.cryptomessage.server.model.persistance.contact.ContactId;
import com.cryptomessage.server.model.persistance.user.AppUser;
import com.cryptomessage.server.repositories.ContactRepository;
import com.cryptomessage.server.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class ContactService {
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    public ContactService(UserRepository userRepository, ContactRepository contactRepository) {
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
    }

    public ContactDTO searchContact(String username) {
        return userRepository.findUserByUsername(username)
                .map(appUser -> ContactDTO.builder()
                        .withContactId(appUser.getUserId())
                        .withUsername(appUser.getUsername())
                        .withPublicKey(appUser.getPublicKey())
                        .build()
                )
                .orElseThrow(() -> new NoSuchElementException("User not found")                );
    }

    public void addContact(Long appUserId, Long contactId) {
        AppUser appUser = findUserOrThrow(appUserId, "User not authorized");
        AppUser contact = findUserOrThrow(contactId, "Contact not found");

        validateNotSelfContact(appUser, contact);

        ContactId newContactId = createContactId(appUser, contact);

        if (contactRepository.existsById(newContactId)) {
            throw new ConflictException("Contact already exists");
        }

        Contact newContact = Contact.builder()
                .withAppUser(appUser)
                .withContact(contact)
                .build();

        contactRepository.save(newContact);
    }

    @Transactional
    public void deleteContact(Long appUserId, Long contactId) {
        AppUser appUser = findUserOrThrow(appUserId, "User not found");
        AppUser contact = findUserOrThrow(contactId, "Contact not found");

        validateNotSelfContact(appUser, contact);

        ContactId toDeleteContactId = createContactId(appUser, contact);

        if (!contactRepository.existsById(toDeleteContactId)) {
            throw new NoSuchElementException("Contact does not exist");
        }

        contactRepository.deleteById(toDeleteContactId);
    }

    private AppUser findUserOrThrow(Long userId, String errorMessage) {
        return userRepository.findUserByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException(errorMessage));
    }

    private void validateNotSelfContact(AppUser appUser, AppUser contact) {
        if (appUser.equals(contact)) {
            throw new ConflictException("User cannot add/delete themselves as a contact");
        }
    }

    private ContactId createContactId(AppUser appUser, AppUser contact) {
        return new ContactId(appUser.getUserId(), contact.getUserId());
    }
}

//V0
//@Service
//public class ContactService {
//    private final UserRepository userRepository;
//    private final ContactRepository contactRepository;
//
//    public ContactService(UserRepository userRepository, ContactRepository contactRepository) {
//        this.userRepository = userRepository;
//        this.contactRepository = contactRepository;
//    }
//
//    public ResponseEntity<?> searchContact(String username) {
//        Optional<AppUser> isUser = userRepository.findUserByUsername(username);
//        if (isUser.isPresent()) {
//            AppUser appUser = isUser.get();
//            return ResponseEntity.status(HttpStatus.OK).body(
//                    SearchContactResponse.builder()
//                            .contactId(appUser.getUserId())
//                            .username(appUser.getUsername())
//                            .publicKey(appUser.getPublicKey())
//                            .build()
//            );
//        } else {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//    }
//
//    public ResponseEntity<Void> addContact(Long appUserId, Long contactId) {
//        Optional<AppUser> isUser = userRepository.findUserByUserId(appUserId);
//        if (isUser.isPresent()) {
//            Optional<AppUser> isContact = userRepository.findUserByUserId(contactId);
//            if (isContact.isPresent()) {
//                AppUser appUser = isUser.get();
//                AppUser contact = isContact.get();
//                if (appUser.equals(contact)) {
//                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//                }
//
//                ContactId newContactId = new ContactId(appUser.getUserId(), contact.getUserId());
//
//
//                if (contactRepository.existsById(newContactId)) {
//                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
//                }
//
//                Contact newContact = Contact.builder()
//                        .withAppUser(appUser)
//                        .withContact(contact)
//                        .build();
//
//                contactRepository.save(newContact);
//                return ResponseEntity.status(HttpStatus.CREATED).build();
//
//
//            } else {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//            }
//        } else {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        }
//    }
//
//    @Transactional
//    public ResponseEntity<Void> deleteContact(Long appUserId, Long contactId) {
//        Optional<AppUser> isUser = userRepository.findUserByUserId(appUserId);
//        if (isUser.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//
//        Optional<AppUser> isContact = userRepository.findUserByUserId(contactId);
//        if (isContact.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//
//        AppUser appUser = isUser.get();
//        AppUser contact = isContact.get();
//
//        // Verificar que no se está intentando eliminar a sí mismo como contacto
//        if (appUser.equals(contact)) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//
//        ContactId toDeleteContactId = new ContactId(appUser.getUserId(), contact.getUserId());
//
//        if (!contactRepository.existsById(toDeleteContactId)) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//
//        contactRepository.deleteById(toDeleteContactId);
//        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();  // 204 cuando se elimina correctamente sin cuerpo
//    }
//}
