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
 *
 * Unit type stats are based on UNIT_TYPES_V1.md:
 * - SWORDSMAN: hp=10, attack=3, moveRange=1, attackRange=1
 * - ARCHER:    hp=8,  attack=3, moveRange=1, attackRange=2
 * - TANK:      hp=16, attack=2, moveRange=1, attackRange=1 (not used in default lineup)
 */
public class GameStateFactory {

    // =========================================================================
    // Unit Type Base Stats (from UNIT_TYPES_V1.md)
    // =========================================================================

    // SWORDSMAN: Balanced melee fighter
    private static final int SWORDSMAN_HP = 10;
    private static final int SWORDSMAN_ATK = 3;
    private static final int SWORDSMAN_MOVE_RANGE = 1;
    private static final int SWORDSMAN_ATTACK_RANGE = 1;

    // ARCHER: Ranged attacker (fragile but high damage potential)
    private static final int ARCHER_HP = 8;
    private static final int ARCHER_ATK = 3;
    private static final int ARCHER_MOVE_RANGE = 1;
    private static final int ARCHER_ATTACK_RANGE = 2;

    // TANK: Durable frontline unit (not used in V1 default lineup)
    private static final int TANK_HP = 16;
    private static final int TANK_ATK = 2;
    private static final int TANK_MOVE_RANGE = 1;
    private static final int TANK_ATTACK_RANGE = 1;

    /**
     * Create a standard 5x5 game state with default unit placements.
     *
     * Based on UNIT_TYPES_V1.md default lineup:
     *
     * P1 units on bottom row (y=0):
     *   - u1_p1: SWORDSMAN at (1,0), hp=10, attack=3, moveRange=1, attackRange=1
     *   - u2_p1: ARCHER at (3,0), hp=8, attack=3, moveRange=1, attackRange=2
     *
     * P2 units on top row (y=4):
     *   - u1_p2: SWORDSMAN at (1,4), hp=10, attack=3, moveRange=1, attackRange=1
     *   - u2_p2: ARCHER at (3,4), hp=8, attack=3, moveRange=1, attackRange=2
     *
     * @return initial GameState with units placed
     */
    public static GameState createStandardGame() {
        Board board = new Board(5, 5);

        List<Unit> units = new ArrayList<>();

        // P1 units (bottom row)
        units.add(new Unit("u1_p1", new PlayerId("P1"), SWORDSMAN_HP, SWORDSMAN_ATK,
            SWORDSMAN_MOVE_RANGE, SWORDSMAN_ATTACK_RANGE, new Position(1, 0), true));
        units.add(new Unit("u2_p1", new PlayerId("P1"), ARCHER_HP, ARCHER_ATK,
            ARCHER_MOVE_RANGE, ARCHER_ATTACK_RANGE, new Position(3, 0), true));

        // P2 units (top row)
        units.add(new Unit("u1_p2", new PlayerId("P2"), SWORDSMAN_HP, SWORDSMAN_ATK,
            SWORDSMAN_MOVE_RANGE, SWORDSMAN_ATTACK_RANGE, new Position(1, 4), true));
        units.add(new Unit("u2_p2", new PlayerId("P2"), ARCHER_HP, ARCHER_ATK,
            ARCHER_MOVE_RANGE, ARCHER_ATTACK_RANGE, new Position(3, 4), true));

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
