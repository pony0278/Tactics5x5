package com.tactics.engine.util;

import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.Unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for GameStateFactory.
 *
 * Tests follow UNIT_TYPES_TESTPLAN_V1.md specification.
 * Validates default game creation and unit type stats.
 */
@DisplayName("GameStateFactoryTest")
class GameStateFactoryTest {

    // =========================================================================
    // Expected values from UNIT_TYPES_V1.md
    // =========================================================================

    // SWORDSMAN stats
    private static final int SWORDSMAN_HP = 10;
    private static final int SWORDSMAN_ATK = 3;
    private static final int SWORDSMAN_MOVE_RANGE = 1;
    private static final int SWORDSMAN_ATTACK_RANGE = 1;

    // ARCHER stats
    private static final int ARCHER_HP = 8;
    private static final int ARCHER_ATK = 3;
    private static final int ARCHER_MOVE_RANGE = 1;
    private static final int ARCHER_ATTACK_RANGE = 2;

    // TANK stats (defined but not used in default lineup)
    private static final int TANK_HP = 16;
    private static final int TANK_ATK = 2;
    private static final int TANK_MOVE_RANGE = 1;
    private static final int TANK_ATTACK_RANGE = 1;

    // Expected positions from UNIT_TYPES_V1.md default lineup
    private static final int U1_P1_X = 1;
    private static final int U1_P1_Y = 0;
    private static final int U2_P1_X = 3;
    private static final int U2_P1_Y = 0;
    private static final int U1_P2_X = 1;
    private static final int U1_P2_Y = 4;
    private static final int U2_P2_X = 3;
    private static final int U2_P2_Y = 4;

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Find a unit by ID in the given list.
     *
     * @param units List of units
     * @param unitId ID to search for
     * @return Unit with matching ID, or null if not found
     */
    private Unit findUnitById(List<Unit> units, String unitId) {
        for (Unit unit : units) {
            if (unit.getId().equals(unitId)) {
                return unit;
            }
        }
        return null;
    }

    // =========================================================================
    // GSF-Series — GameStateFactory Tests
    // =========================================================================

    @Nested
    @DisplayName("GSF-Series - Default GameState creation")
    class DefaultGameStateTests {

        private GameState state;

        @BeforeEach
        void setUp() {
            state = GameStateFactory.createStandardGame();
        }

        @Test
        @DisplayName("GSF1 — createStandardGame returns non-null GameState")
        void gsf1_createStandardGameReturnsNonNullGameState() {
            // Then: result is not null
            assertNotNull(state, "createStandardGame() should return a non-null GameState");

            // Then: getBoard() is not null
            assertNotNull(state.getBoard(), "GameState.getBoard() should not be null");

            // Then: getUnits() is not null
            assertNotNull(state.getUnits(), "GameState.getUnits() should not be null");
        }

        @Test
        @DisplayName("GSF2 — Initial GameState uses a 5x5 board")
        void gsf2_initialGameStateUses5x5Board() {
            Board board = state.getBoard();

            // Then: width == 5
            assertEquals(5, board.getWidth(), "Board width should be 5");

            // Then: height == 5
            assertEquals(5, board.getHeight(), "Board height should be 5");
        }

        @Test
        @DisplayName("GSF3 — Default lineup has exactly 4 units")
        void gsf3_defaultLineupHasExactly4Units() {
            List<Unit> units = state.getUnits();

            // Then: units.size() == 4
            assertEquals(4, units.size(), "Default lineup should have exactly 4 units");
        }

        @Test
        @DisplayName("GSF4 — Default unit IDs and owners")
        void gsf4_defaultUnitIdsAndOwners() {
            List<Unit> units = state.getUnits();

            // Find each expected unit
            Unit u1_p1 = findUnitById(units, "u1_p1");
            Unit u2_p1 = findUnitById(units, "u2_p1");
            Unit u1_p2 = findUnitById(units, "u1_p2");
            Unit u2_p2 = findUnitById(units, "u2_p2");

            // Then: all units exist
            assertNotNull(u1_p1, "Unit u1_p1 should exist");
            assertNotNull(u2_p1, "Unit u2_p1 should exist");
            assertNotNull(u1_p2, "Unit u1_p2 should exist");
            assertNotNull(u2_p2, "Unit u2_p2 should exist");

            // Then: owners are correct
            assertEquals("P1", u1_p1.getOwner().getValue(), "u1_p1 should be owned by P1");
            assertEquals("P1", u2_p1.getOwner().getValue(), "u2_p1 should be owned by P1");
            assertEquals("P2", u1_p2.getOwner().getValue(), "u1_p2 should be owned by P2");
            assertEquals("P2", u2_p2.getOwner().getValue(), "u2_p2 should be owned by P2");

            // Then: no duplicate IDs
            Set<String> unitIds = new HashSet<>();
            for (Unit unit : units) {
                assertTrue(unitIds.add(unit.getId()),
                    "Duplicate unit ID found: " + unit.getId());
            }
        }

        @Test
        @DisplayName("GSF5 — Default starting positions match UNIT_TYPES_V1")
        void gsf5_defaultStartingPositionsMatchUnitTypesV1() {
            List<Unit> units = state.getUnits();

            Unit u1_p1 = findUnitById(units, "u1_p1");
            Unit u2_p1 = findUnitById(units, "u2_p1");
            Unit u1_p2 = findUnitById(units, "u1_p2");
            Unit u2_p2 = findUnitById(units, "u2_p2");

            // Then: u1_p1.position == (1, 0)
            assertEquals(U1_P1_X, u1_p1.getPosition().getX(), "u1_p1 X position should be 1");
            assertEquals(U1_P1_Y, u1_p1.getPosition().getY(), "u1_p1 Y position should be 0");

            // Then: u2_p1.position == (3, 0)
            assertEquals(U2_P1_X, u2_p1.getPosition().getX(), "u2_p1 X position should be 3");
            assertEquals(U2_P1_Y, u2_p1.getPosition().getY(), "u2_p1 Y position should be 0");

            // Then: u1_p2.position == (1, 4)
            assertEquals(U1_P2_X, u1_p2.getPosition().getX(), "u1_p2 X position should be 1");
            assertEquals(U1_P2_Y, u1_p2.getPosition().getY(), "u1_p2 Y position should be 4");

            // Then: u2_p2.position == (3, 4)
            assertEquals(U2_P2_X, u2_p2.getPosition().getX(), "u2_p2 X position should be 3");
            assertEquals(U2_P2_Y, u2_p2.getPosition().getY(), "u2_p2 Y position should be 4");
        }

        @Test
        @DisplayName("GSF6 — Default stats for SWORDSMAN units")
        void gsf6_defaultStatsForSwordsmanUnits() {
            List<Unit> units = state.getUnits();

            // u1_p1 and u1_p2 are SWORDSMAN units
            Unit u1_p1 = findUnitById(units, "u1_p1");
            Unit u1_p2 = findUnitById(units, "u1_p2");

            // Then: u1_p1 has SWORDSMAN stats
            assertEquals(SWORDSMAN_HP, u1_p1.getHp(), "u1_p1 (SWORDSMAN) HP should be 10");
            assertEquals(SWORDSMAN_ATK, u1_p1.getAttack(), "u1_p1 (SWORDSMAN) attack should be 3");
            assertEquals(SWORDSMAN_MOVE_RANGE, u1_p1.getMoveRange(), "u1_p1 (SWORDSMAN) moveRange should be 1");
            assertEquals(SWORDSMAN_ATTACK_RANGE, u1_p1.getAttackRange(), "u1_p1 (SWORDSMAN) attackRange should be 1");
            assertTrue(u1_p1.isAlive(), "u1_p1 should be alive");

            // Then: u1_p2 has SWORDSMAN stats
            assertEquals(SWORDSMAN_HP, u1_p2.getHp(), "u1_p2 (SWORDSMAN) HP should be 10");
            assertEquals(SWORDSMAN_ATK, u1_p2.getAttack(), "u1_p2 (SWORDSMAN) attack should be 3");
            assertEquals(SWORDSMAN_MOVE_RANGE, u1_p2.getMoveRange(), "u1_p2 (SWORDSMAN) moveRange should be 1");
            assertEquals(SWORDSMAN_ATTACK_RANGE, u1_p2.getAttackRange(), "u1_p2 (SWORDSMAN) attackRange should be 1");
            assertTrue(u1_p2.isAlive(), "u1_p2 should be alive");
        }

        @Test
        @DisplayName("GSF7 — Default stats for ARCHER units")
        void gsf7_defaultStatsForArcherUnits() {
            List<Unit> units = state.getUnits();

            // u2_p1 and u2_p2 are ARCHER units
            Unit u2_p1 = findUnitById(units, "u2_p1");
            Unit u2_p2 = findUnitById(units, "u2_p2");

            // Then: u2_p1 has ARCHER stats
            assertEquals(ARCHER_HP, u2_p1.getHp(), "u2_p1 (ARCHER) HP should be 8");
            assertEquals(ARCHER_ATK, u2_p1.getAttack(), "u2_p1 (ARCHER) attack should be 3");
            assertEquals(ARCHER_MOVE_RANGE, u2_p1.getMoveRange(), "u2_p1 (ARCHER) moveRange should be 1");
            assertEquals(ARCHER_ATTACK_RANGE, u2_p1.getAttackRange(), "u2_p1 (ARCHER) attackRange should be 2");
            assertTrue(u2_p1.isAlive(), "u2_p1 should be alive");

            // Then: u2_p2 has ARCHER stats
            assertEquals(ARCHER_HP, u2_p2.getHp(), "u2_p2 (ARCHER) HP should be 8");
            assertEquals(ARCHER_ATK, u2_p2.getAttack(), "u2_p2 (ARCHER) attack should be 3");
            assertEquals(ARCHER_MOVE_RANGE, u2_p2.getMoveRange(), "u2_p2 (ARCHER) moveRange should be 1");
            assertEquals(ARCHER_ATTACK_RANGE, u2_p2.getAttackRange(), "u2_p2 (ARCHER) attackRange should be 2");
            assertTrue(u2_p2.isAlive(), "u2_p2 should be alive");
        }

        @Test
        @DisplayName("GSF8 — Initial flags: currentPlayer, isGameOver, winner")
        void gsf8_initialFlagsCurrentPlayerIsGameOverWinner() {
            // Then: currentPlayer.value == "P1" (Player 1 starts)
            assertNotNull(state.getCurrentPlayer(), "currentPlayer should not be null");
            assertEquals("P1", state.getCurrentPlayer().getValue(),
                "currentPlayer should be P1 (Player 1 starts)");

            // Then: isGameOver == false
            assertFalse(state.isGameOver(), "isGameOver should be false at game start");

            // Then: winner == null
            assertNull(state.getWinner(), "winner should be null at game start");
        }

        @Test
        @DisplayName("GSF9 — No TANK units in default lineup")
        void gsf9_noTankUnitsInDefaultLineup() {
            List<Unit> units = state.getUnits();

            // Then: no unit has TANK stats (hp == 16 && attack == 2)
            for (Unit unit : units) {
                boolean hasTankStats = (unit.getHp() == TANK_HP && unit.getAttack() == TANK_ATK);
                assertFalse(hasTankStats,
                    "No unit should have TANK stats (HP=16, ATK=2), but found: " + unit.getId());
            }
        }
    }
}
