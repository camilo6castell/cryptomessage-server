package com.cryptomessage.server.repositories;

import com.cryptomessage.server.model.entity.contact.Contact;
import com.cryptomessage.server.model.entity.contact.ContactId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, ContactId> {

//    List<Contact> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
}
