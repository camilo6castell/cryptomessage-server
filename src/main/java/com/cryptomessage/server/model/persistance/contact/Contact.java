package com.cryptomessage.server.model.persistance.contact;

import com.cryptomessage.server.model.persistance.user.AppUser;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "contacts")
@IdClass(ContactId.class)
public class Contact implements Serializable {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser appUser;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private AppUser contact;

    // Constructor privado para forzar el uso del Builder
    private Contact(Builder builder) {
        this.appUser = Objects.requireNonNull(builder.appUser, "appUser cannot be null");
        this.contact = Objects.requireNonNull(builder.contact, "contact cannot be null");
    }

    // Constructor sin argumentos necesario para JPA
    protected Contact() {
    }

    // Getters
    public AppUser getAppUser() {
        return appUser;
    }

    public AppUser getContact() {
        return contact;
    }

    // Builder interno
    public static class Builder {
        private AppUser appUser;
        private AppUser contact;

        public Builder withAppUser(AppUser appUser) {
            this.appUser = appUser;
            return this;
        }

        public Builder withContact(AppUser contact) {
            this.contact = contact;
            return this;
        }

        public Contact build() {
            return new Contact(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Implementación de equals, hashCode y toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return Objects.equals(appUser, contact.appUser) &&
                Objects.equals(this.contact, contact.contact);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appUser, contact);
    }

    @Override
    public String toString() {
        return "Contact{" +
                "appUser=" + appUser +
                ", contact=" + contact +
                '}';
    }
}

// V0
//@Entity
//@Table(name = "contacts")
//@IdClass(ContactId.class)
//public class Contact implements Serializable {
//
//    @Id
//    @ManyToOne
//    @JoinColumn(name = "user_id", nullable = false)
//    private AppUser appUser;
//
//    @Id
//    @ManyToOne
//    @JoinColumn(name = "contact_id", nullable = false)
//    private AppUser contact;
//
//    // Constructor privado para forzar el uso del Builder
//    private Contact(Builder builder) {
//        this.appUser = builder.appUser;
//        this.contact = builder.contact;
//    }
//
//    public Contact() {
//    }
//
//    // Getters
//    public AppUser getUser() {
//        return appUser;
//    }
//
//    public AppUser getContact() {
//        return contact;
//    }
//
//    // Builder interno
//    public static class Builder {
//        private AppUser appUser;
//        private AppUser contact;
//
//        public Builder withUser(AppUser appUser) {
//            this.appUser = appUser;
//            return this;
//        }
//
//        public Builder withContact(AppUser contact) {
//            this.contact = contact;
//            return this;
//        }
//
//        public Contact build() {
//            // Validación de campos obligatorios
//            if (appUser == null || contact == null) {
//                throw new IllegalStateException("appUser y contact no pueden ser nulos");
//            }
//            return new Contact(this);
//        }
//    }
//
//    // Implementación de equals, hashCode y toString
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Contact contact = (Contact) o;
//        return Objects.equals(appUser, contact.appUser) &&
//                Objects.equals(this.contact, contact.contact);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(appUser, contact);
//    }
//
//    @Override
//    public String toString() {
//        return "Contact{" +
//                "appUser=" + appUser +
//                ", contact=" + contact +
//                '}';
//    }
//}
