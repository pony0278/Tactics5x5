package com.tactics.engine.model;

import java.util.List;

/**
 * Represents the complete state of an ongoing match.
 */
public class GameState {

    private final Board board;
    private final List<Unit> units;
    private final PlayerId currentPlayer;
    private final boolean isGameOver;
    private final PlayerId winner;

    public GameState(Board board, List<Unit> units, PlayerId currentPlayer, boolean isGameOver, PlayerId winner) {
        this.board = board;
        this.units = units;
        this.currentPlayer = currentPlayer;
        this.isGameOver = isGameOver;
        this.winner = winner;
    }

    public Board getBoard() {
        return board;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public PlayerId getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public PlayerId getWinner() {
        return winner;
    }
}
