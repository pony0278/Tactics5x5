package com.tactics.server.core;

/**
 * Strongly typed wrapper for a match identifier.
 */
public class MatchId {

    private final String value;

    public MatchId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
