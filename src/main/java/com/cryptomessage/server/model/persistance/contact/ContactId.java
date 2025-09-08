package com.cryptomessage.server.model.persistance.contact;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;



@Embeddable
public class ContactId implements Serializable {
    private final Long appUser;
    private final Long contact;

    // Constructor sin argumentos requerido por JPA
    protected ContactId() {
        this.appUser = null;
        this.contact = null;
    }

    // Constructor parametrizado para crear instancias inmutables
    public ContactId(Long appUser, Long contact) {
        this.appUser = Objects.requireNonNull(appUser, "AppUser cannot be null");
        this.contact = Objects.requireNonNull(contact, "Contact cannot be null");
    }

    public Long getAppUser() {
        return appUser;
    }

    public Long getContact() {
        return contact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactId contactId = (ContactId) o;
        return Objects.equals(appUser, contactId.appUser) && Objects.equals(contact, contactId.contact);
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

//V0
//public class ContactId implements Serializable {
//    private Long appUser;
//    private Long contact;
//
//    public Long getAppUser() {
//        return appUser;
//    }
//
//    public void setAppUser(Long appUser) {
//        this.appUser = appUser;
//    }
//
//    public Long getContact() {
//        return contact;
//    }
//
//    public void setContact(Long contact) {
//        this.contact = contact;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        ContactId contactId = (ContactId) o;
//        return Objects.equals(appUser, contactId.appUser) && Objects.equals(contact, contactId.contact);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(appUser, contact);
//    }
//
//    @Override
//    public String toString() {
//        return "ContactId{" +
//                "userId=" + appUser +
//                ", contactUserId=" + contact +
//                '}';
//    }
//}
