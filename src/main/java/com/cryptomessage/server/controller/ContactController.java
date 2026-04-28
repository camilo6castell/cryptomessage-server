package com.cryptomessage.server.controller;

import com.cryptomessage.server.model.dto.contact.AddContactRequest;
import com.cryptomessage.server.model.dto.contact.ContactResponse;
import com.cryptomessage.server.model.dto.contact.SearchContactRequest;
import com.cryptomessage.server.services.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /* ================= SEARCH USER ================= */

    @PostMapping("/search")
    public ResponseEntity<ContactResponse> searchUser(
            @RequestBody SearchContactRequest request
    ) {
        return ResponseEntity.ok(
                contactService.searchUserByUsername(request.username())
        );
    }

    /* ================= LIST CONTACTS ================= */

    @GetMapping
    public ResponseEntity<List<ContactResponse>> getMyContacts() {
        return ResponseEntity.ok(
                contactService.getContacts()
        );
    }

    /* ================= ADD CONTACT ================= */

    @PostMapping
    public ResponseEntity<Void> addContact(
            @RequestBody AddContactRequest request
    ) {
        contactService.addContact(request.contactId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    /* ================= REMOVE CONTACT ================= */

    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> removeContact(
            @PathVariable Long contactId
    ) {
        contactService.removeContact(contactId);
        return ResponseEntity.noContent().build();
    }
}
