package com.tactics.engine.model;

/**
 * Represents a unit on the board.
 */
public class Unit {

    private final String id;
    private final PlayerId owner;
    private final int hp;
    private final int attack;
    private final Position position;
    private final boolean alive;

    public Unit(String id, PlayerId owner, int hp, int attack, Position position, boolean alive) {
        this.id = id;
        this.owner = owner;
        this.hp = hp;
        this.attack = attack;
        this.position = position;
        this.alive = alive;
    }

    public String getId() {
        return id;
    }

    public PlayerId getOwner() {
        return owner;
    }

    public int getHp() {
        return hp;
    }

    public int getAttack() {
        return attack;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isAlive() {
        return alive;
    }
}
