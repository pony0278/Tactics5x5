package com.tactics.engine.model;

import java.util.Objects;

/**
 * Represents a pending death choice awaiting player decision.
 * When a friendly minion dies, the owner must choose between:
 * - SPAWN_OBSTACLE: Creates impassable terrain at death location
 * - SPAWN_BUFF_TILE: Creates a BUFF tile (random buff) at death location
 */
public class DeathChoice {

    /**
     * The type of choice the player can make after minion death.
     */
    public enum ChoiceType {
        SPAWN_OBSTACLE,
        SPAWN_BUFF_TILE
    }

    private final String deadUnitId;      // ID of the minion that died
    private final PlayerId owner;         // Player who must make the choice
    private final Position deathPosition; // Where the minion died

    public DeathChoice(String deadUnitId, PlayerId owner, Position deathPosition) {
        this.deadUnitId = deadUnitId;
        this.owner = owner;
        this.deathPosition = deathPosition;
    }

    public String getDeadUnitId() {
        return deadUnitId;
    }

    public PlayerId getOwner() {
        return owner;
    }

    public Position getDeathPosition() {
        return deathPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeathChoice that = (DeathChoice) o;
        return Objects.equals(deadUnitId, that.deadUnitId) &&
               Objects.equals(owner, that.owner) &&
               Objects.equals(deathPosition, that.deathPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deadUnitId, owner, deathPosition);
    }

    @Override
    public String toString() {
        return "DeathChoice{" +
               "deadUnitId='" + deadUnitId + '\'' +
               ", owner=" + owner +
               ", deathPosition=" + deathPosition +
               '}';
    }
}
