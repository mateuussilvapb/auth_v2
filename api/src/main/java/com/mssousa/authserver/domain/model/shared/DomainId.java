package com.mssousa.authserver.domain.model.shared;

import java.util.Objects;

public abstract class DomainId {
    protected final Long value;

    protected DomainId(Long value) {
        this.value = Objects.requireNonNull(value);
    }

    public Long value() {
        return value;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainId domainId = (DomainId) o;
        return value.equals(domainId.value);
    }

    @Override
    public final int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
