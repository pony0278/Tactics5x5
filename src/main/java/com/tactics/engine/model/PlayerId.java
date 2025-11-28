package com.tactics.engine.model;

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
}
