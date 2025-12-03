package com.tactics.engine.model;

import com.tactics.engine.buff.BuffType;

import java.util.Objects;

/**
 * Represents a BUFF tile on the board.
 * Created when a friendly minion dies and owner chooses SPAWN_BUFF_TILE.
 *
 * Properties:
 * - Duration: 2 rounds on the map
 * - Trigger: Once only - first unit to step on it
 * - Effect: Applies the contained BUFF to the triggering unit
 */
public class BuffTile {

    private final String id;
    private final Position position;
    private final BuffType buffType;  // May be null if random-on-trigger
    private final int duration;       // Rounds remaining on map (starts at 2)
    private final boolean triggered;  // Once true, tile should be removed

    public BuffTile(String id, Position position, BuffType buffType, int duration, boolean triggered) {
        this.id = id;
        this.position = position;
        this.buffType = buffType;
        this.duration = duration;
        this.triggered = triggered;
    }

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public BuffType getBuffType() {
        return buffType;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isTriggered() {
        return triggered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuffTile buffTile = (BuffTile) o;
        return duration == buffTile.duration &&
               triggered == buffTile.triggered &&
               Objects.equals(id, buffTile.id) &&
               Objects.equals(position, buffTile.position) &&
               buffType == buffTile.buffType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, position, buffType, duration, triggered);
    }

    @Override
    public String toString() {
        return "BuffTile{" +
               "id='" + id + '\'' +
               ", position=" + position +
               ", buffType=" + buffType +
               ", duration=" + duration +
               ", triggered=" + triggered +
               '}';
    }
}
