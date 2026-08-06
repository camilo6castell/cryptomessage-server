package com.cryptomessage.server.domain.generic;

import java.util.Objects;

/**
 * Base type for aggregate identities. Ported from the Library Provider project's
 * generic ES framework, kept synchronous on purpose — this codebase is JPA/MVC
 * (blocking), not WebFlux, so there is no Mono/Flux wrapping here.
 */
public abstract class Identity implements IValueObject<String> {
    private final String uuid;

    protected Identity(String uuid) {
        this.uuid = Objects.requireNonNull(uuid, "Identity value cannot be null");
    }

    @Override
    public String value() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return Objects.equals(uuid, identity.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + uuid + ")";
    }
}
