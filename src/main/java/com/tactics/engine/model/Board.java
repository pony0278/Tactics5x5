package com.tactics.engine.model;

/**
 * Defines the 5x5 grid.
 */
public class Board {

    private final int width;
    private final int height;

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
