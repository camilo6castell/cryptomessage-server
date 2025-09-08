package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.persistance.contact.Contact;
import com.cryptomessage.server.model.persistance.contact.ContactId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, ContactId> {

//    List<Contact> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
}
