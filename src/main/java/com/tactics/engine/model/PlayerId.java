package com.tactics.engine.model;

import java.util.Objects;

/**
 * Strong typing wrapper for player identity.
 */
public class PlayerId {

    // Common player ID constants
    public static final String PLAYER_1_VALUE = "P1";
    public static final String PLAYER_2_VALUE = "P2";
    public static final PlayerId PLAYER_1 = new PlayerId(PLAYER_1_VALUE);
    public static final PlayerId PLAYER_2 = new PlayerId(PLAYER_2_VALUE);

    private final String value;

    public PlayerId(String value) {
        this.value = value;
    }

    /**
     * Check if this is Player 1.
     */
    public boolean isPlayer1() {
        return PLAYER_1_VALUE.equals(value);
    }

    /**
     * Check if this is Player 2.
     */
    public boolean isPlayer2() {
        return PLAYER_2_VALUE.equals(value);
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
