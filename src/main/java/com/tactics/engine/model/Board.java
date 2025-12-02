package com.tactics.engine.model;

import java.util.Objects;

/**
 * Defines the game grid dimensions.
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Board board = (Board) o;
        return width == board.width && height == board.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }

    @Override
    public String toString() {
        return "Board{" + width + "x" + height + "}";
    }
}
