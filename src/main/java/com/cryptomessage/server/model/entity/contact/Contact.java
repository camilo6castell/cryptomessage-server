package com.cryptomessage.server.model.entity.contact;

import com.cryptomessage.server.model.entity.user.AppUser;
import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(
        name = "contacts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_contacts_user_contact",
                        columnNames = {"user_id", "contact_id"}
                )
        }
)
public class Contact implements Serializable {

    @EmbeddedId
    private ContactId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("appUser")
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("contact")
    @JoinColumn(name = "contact_id", nullable = false)
    private AppUser contact;

    // Constructor sin argumentos necesario para JPA
    protected Contact() {}

    public Contact(AppUser appUser, AppUser contact) {
        this.appUser = appUser;
        this.contact = contact;
    }


    // Getters
    public AppUser getAppUser() {
        return appUser;
    }

    public AppUser getContact() {
        return contact;
    }
}

