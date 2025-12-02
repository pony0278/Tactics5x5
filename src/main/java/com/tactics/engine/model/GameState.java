package com.tactics.engine.model;

import com.tactics.engine.buff.BuffInstance;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the complete state of an ongoing match.
 * Immutable: all fields are final and collections are unmodifiable.
 */
public class GameState {

    private final Board board;
    private final List<Unit> units;
    private final PlayerId currentPlayer;
    private final boolean isGameOver;
    private final PlayerId winner;
    private final Map<String, List<BuffInstance>> unitBuffs;

    public GameState(Board board, List<Unit> units, PlayerId currentPlayer, boolean isGameOver, PlayerId winner) {
        this(board, units, currentPlayer, isGameOver, winner, Collections.emptyMap());
    }

    public GameState(Board board, List<Unit> units, PlayerId currentPlayer, boolean isGameOver, PlayerId winner,
                     Map<String, List<BuffInstance>> unitBuffs) {
        this.board = board;
        this.units = units;
        this.currentPlayer = currentPlayer;
        this.isGameOver = isGameOver;
        this.winner = winner;
        this.unitBuffs = unitBuffs != null ? Collections.unmodifiableMap(unitBuffs) : Collections.emptyMap();
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

    public Map<String, List<BuffInstance>> getUnitBuffs() {
        return unitBuffs;
    }
}
