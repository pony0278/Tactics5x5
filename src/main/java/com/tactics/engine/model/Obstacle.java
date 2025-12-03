package com.tactics.engine.model;

import java.util.Objects;

/**
 * Represents an obstacle on the board.
 * Created when a friendly minion dies and owner chooses SPAWN_OBSTACLE.
 *
 * Properties:
 * - Blocks movement (units cannot pass through)
 * - Duration: Permanent until destroyed
 * - Destruction: Only units with POWER BUFF can destroy obstacles
 */
public class Obstacle {

    private final String id;
    private final Position position;

    public Obstacle(String id, Position position) {
        this.id = id;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Obstacle obstacle = (Obstacle) o;
        return Objects.equals(id, obstacle.id) &&
               Objects.equals(position, obstacle.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, position);
    }

    @Override
    public String toString() {
        return "Obstacle{" +
               "id='" + id + '\'' +
               ", position=" + position +
               '}';
    }
}
