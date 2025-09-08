package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.dto.contact.TransactionContactRequest;
import com.cryptomessage.server.model.dto.contact.SearchContactRequest;
import com.cryptomessage.server.services.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactController {
    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping("/search-contact")
    public ResponseEntity<?> searchContact(@RequestBody SearchContactRequest searchContactRequest) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(contactService.searchContact(searchContactRequest.getUsername()));
    }

    @PostMapping("/add-contact")
    public ResponseEntity<Void> addContact(@RequestBody TransactionContactRequest transactionContactRequest) {
        contactService.addContact(
                transactionContactRequest.getAppUserId(),
                transactionContactRequest.getContactId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/delete-contact/{userId}/contacts/{contactId}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long userId, @PathVariable Long contactId) {
        contactService.deleteContact(userId, contactId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

//V0
//@RestController
//@RequestMapping("/api/v1/contact")
//public class ContactController {
//    private final ContactService contactService;
//
//    public ContactController(ContactService contactService) {
//        this.contactService = contactService;
//    }
//
//    @PostMapping("/search-contact")
//    public ResponseEntity<?> searchContact(@RequestBody SearchContactRequest searchContactRequest) {
//        return contactService.searchContact(searchContactRequest.getUsername());
//    }
//
//    @PostMapping("/add-contact")
//    public ResponseEntity<Void> addContact(@RequestBody TransactionContactRequest transactionContactRequest) {
//        return contactService.addContact(
//                transactionContactRequest.getAppUserId(),
//                transactionContactRequest.getContactId()
//        );
//    }
//
//    @DeleteMapping("/delete-contact/{userId}/contacts/{contactId}")
//    public ResponseEntity<Void> deleteContact(@PathVariable Long userId, @PathVariable Long contactId) {
//        return contactService.deleteContact(userId, contactId);
//    }
//}
