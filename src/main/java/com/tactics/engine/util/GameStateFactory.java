package com.tactics.engine.util;

import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating initial game states.
 */
public class GameStateFactory {

    /**
     * Create a standard 5x5 game state with default unit placements.
     *
     * P1 units on bottom row (y=0):
     *   - u1_p1: position (1,0), hp=10, attack=3
     *   - u2_p1: position (3,0), hp=10, attack=3
     *
     * P2 units on top row (y=4):
     *   - u1_p2: position (1,4), hp=10, attack=3
     *   - u2_p2: position (3,4), hp=10, attack=3
     *
     * @return initial GameState with units placed
     */
    public static GameState createStandardGame() {
        Board board = new Board(5, 5);

        List<Unit> units = new ArrayList<>();

        // P1 units (bottom row)
        units.add(new Unit("u1_p1", new PlayerId("P1"), 10, 3, new Position(1, 0), true));
        units.add(new Unit("u2_p1", new PlayerId("P1"), 10, 3, new Position(3, 0), true));

        // P2 units (top row)
        units.add(new Unit("u1_p2", new PlayerId("P2"), 10, 3, new Position(1, 4), true));
        units.add(new Unit("u2_p2", new PlayerId("P2"), 10, 3, new Position(3, 4), true));

        return new GameState(
            board,
            units,
            new PlayerId("P1"),  // P1 starts
            false,
            null
        );
    }

    /**
     * Create an empty 5x5 game state with no units.
     * Useful for testing.
     *
     * @return initial GameState with no units
     */
    public static GameState createEmptyGame() {
        return new GameState(
            new Board(5, 5),
            new ArrayList<>(),
            new PlayerId("P1"),
            false,
            null
        );
    }

    /**
     * Create a custom game state with specified parameters.
     *
     * @param width board width
     * @param height board height
     * @param units list of units
     * @param startingPlayer player who starts (P1 or P2)
     * @return custom GameState
     */
    public static GameState createCustomGame(int width, int height, List<Unit> units, String startingPlayer) {
        return new GameState(
            new Board(width, height),
            new ArrayList<>(units),  // Copy to prevent external mutation
            new PlayerId(startingPlayer),
            false,
            null
        );
    }
}
