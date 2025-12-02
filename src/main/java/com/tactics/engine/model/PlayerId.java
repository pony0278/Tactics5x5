package com.tactics.engine.model;

import java.util.Objects;

/**
 * Strong typing wrapper for player identity.
 */
public class PlayerId {

    private final String value;

    public PlayerId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerId playerId = (PlayerId) o;
        return Objects.equals(value, playerId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "PlayerId{" + value + "}";
    }
}
