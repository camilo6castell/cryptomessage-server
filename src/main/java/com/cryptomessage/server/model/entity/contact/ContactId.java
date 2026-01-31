package com.cryptomessage.server.model.entity.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContactId implements Serializable {

    @Column(name = "user_id")
    private Long appUser;

    @Column(name = "contact_id")
    private Long contact;

    // Constructor sin argumentos requerido por JPA
    protected ContactId() {}

    // Constructor parametrizado para crear instancias inmutables
    public ContactId(Long appUser, Long contact) {
        this.appUser = Objects.requireNonNull(appUser, "AppUser cannot be null");
        this.contact = Objects.requireNonNull(contact, "Contact cannot be null");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContactId)) return false;
        ContactId that = (ContactId) o;
        return Objects.equals(appUser, that.appUser)
                && Objects.equals(contact, that.contact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appUser, contact);
    }

    @Override
    public String toString() {
        return "ContactId{" +
                "appUser=" + appUser +
                ", contact=" + contact +
                '}';
    }
}
