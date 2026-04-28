package com.cryptomessage.server.model.mapper;

import com.cryptomessage.server.model.dto.contact.ContactResponse;
import com.cryptomessage.server.model.entity.contact.Contact;
import com.cryptomessage.server.model.entity.user.AppUser;
import org.springframework.stereotype.Service;

@Service
public class ContactMapper {

    public ContactResponse toResponse(Contact contact) {
        if (contact == null) return null;

        AppUser user = contact.getContact();

        return new ContactResponse(
                user.getUserId(),
                user.getUsername(),
                user.getPublicKey()
        );
    }
}

