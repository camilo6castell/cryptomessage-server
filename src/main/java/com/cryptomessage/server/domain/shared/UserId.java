package com.cryptomessage.server.domain.shared;

import com.cryptomessage.server.domain.generic.IValueObject;

import java.util.Objects;

/**
 * Reference to an AppUser aggregate. AppUser deliberately stays a plain JPA entity
 * (see project notes) — user registration/profile data has no real audit-history
 * value that would justify Event Sourcing. This value object exists only so the
 * Chat aggregate can refer to participants by id without depending on AppUser
 * (and, transitively, on JPA) from inside the domain layer.
 */
public final class UserId implements IValueObject<Long> {

    private final Long id;

    private UserId(Long id) {
        this.id = Objects.requireNonNull(id, "UserId cannot be null");
    }

    public static UserId of(Long id) {
        return new UserId(id);
    }

    @Override
    public Long value() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserId other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserId(" + id + ")";
    }
}
