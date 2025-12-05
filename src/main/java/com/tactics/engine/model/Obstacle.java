package com.tactics.engine.model;

import java.util.Objects;

/**
 * Represents an obstacle on the board.
 * Created when a friendly minion dies and owner chooses SPAWN_OBSTACLE.
 *
 * Properties:
 * - Blocks movement (units cannot pass through)
 * - HP: 3 (default) - can be attacked and destroyed
 * - Destruction: Any unit can attack, POWER buff destroys instantly
 */
public class Obstacle {

    public static final int DEFAULT_HP = 3;
    public static final String ID_PREFIX = "obstacle_";

    private final String id;
    private final Position position;
    private final int hp;

    /**
     * Create obstacle with default HP (3).
     */
    public Obstacle(String id, Position position) {
        this(id, position, DEFAULT_HP);
    }

    /**
     * Create obstacle with specified HP.
     */
    public Obstacle(String id, Position position, int hp) {
        this.id = id;
        this.position = position;
        this.hp = hp;
    }

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public int getHp() {
        return hp;
    }

    /**
     * Check if obstacle is destroyed (HP <= 0).
     */
    public boolean isDestroyed() {
        return hp <= 0;
    }

    /**
     * Create a new Obstacle with reduced HP after taking damage.
     * Returns a new immutable instance.
     */
    public Obstacle withDamage(int damage) {
        return new Obstacle(id, position, hp - damage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Obstacle obstacle = (Obstacle) o;
        return hp == obstacle.hp &&
               Objects.equals(id, obstacle.id) &&
               Objects.equals(position, obstacle.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, position, hp);
    }

    @Override
    public String toString() {
        return "Obstacle{" +
               "id='" + id + '\'' +
               ", position=" + position +
               ", hp=" + hp +
               '}';
    }
}
