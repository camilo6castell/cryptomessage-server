package com.cryptomessage.server.domain.generic;

import java.util.Objects;

public abstract class Entity<I extends Identity> {
    private final I id;

    protected Entity(I id) {
        this.id = id;
    }

    public I identity() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity<?> other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
