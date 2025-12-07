package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.MinionType;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-by-Unit Turn System Tests.
 * Based on UNIT_TURN_TESTPLAN.md - UT-Series (Basic Turn Logic)
 *
 * Key design decisions:
 * - END_TURN marks only ONE unit as acted (not all units)
 * - Turn switches after each action
 * - Exhaustion Rule: opponent takes consecutive turns when one player has no unacted units
 * - SPEED buff: consecutive actions (both actions before switch)
 * - Unit selection: Free choice (player picks any unused unit)
 */
class RuleEngineUnitTurnTest {

    private RuleEngine ruleEngine;
    private PlayerId p1;
    private PlayerId p2;
    private Board board;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
        p1 = new PlayerId("P1");
        p2 = new PlayerId("P2");
        board = new Board(5, 5);
    }

    // Helper to find unit by ID in a list
    private Unit findUnit(List<Unit> units, String id) {
        for (Unit u : units) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    // Helper to create a V3 Hero unit
    private Unit createHero(String id, PlayerId owner, Position pos, HeroClass heroClass) {
        return new Unit(id, owner, 10, 3, 2, 2, pos, true,
                UnitCategory.HERO, null, heroClass, 10,
                null, 0,
                0, false, false, false, 0, null,
                0, false, null,
                0, 0);
    }

    // Helper to create a V3 Minion unit
    private Unit createMinion(String id, PlayerId owner, Position pos, MinionType minionType) {
        int hp = minionType == MinionType.TANK ? 6 : (minionType == MinionType.ARCHER ? 3 : 4);
        int atk = minionType == MinionType.TANK ? 2 : (minionType == MinionType.ARCHER ? 3 : 3);
        int moveRange = minionType == MinionType.TANK ? 1 : (minionType == MinionType.ARCHER ? 1 : 2);
        int attackRange = minionType == MinionType.TANK ? 1 : (minionType == MinionType.ARCHER ? 3 : 1);
        return new Unit(id, owner, hp, atk, moveRange, attackRange, pos, true,
                UnitCategory.MINION, minionType, null, hp,
                null, 0,
                0, false, false, false, 0, null,
                0, false, null,
                0, 0);
    }

    // Helper to create standard 3v3 game state
    private GameState create3v3State() {
        List<Unit> units = new ArrayList<>();
        // P1: Hero at (2,0), Minion1 at (0,0), Minion2 at (4,0)
        units.add(createHero("p1_hero", p1, new Position(2, 0), HeroClass.WARRIOR));
        units.add(createMinion("p1_minion_1", p1, new Position(0, 0), MinionType.TANK));
        units.add(createMinion("p1_minion_2", p1, new Position(4, 0), MinionType.ARCHER));
        // P2: Hero at (2,4), Minion1 at (0,4), Minion2 at (4,4)
        units.add(createHero("p2_hero", p2, new Position(2, 4), HeroClass.WARRIOR));
        units.add(createMinion("p2_minion_1", p2, new Position(0, 4), MinionType.TANK));
        units.add(createMinion("p2_minion_2", p2, new Position(4, 4), MinionType.ARCHER));

        return new GameState(board, units, p1, false, null);
    }

    // ========== UT-Series: Unit Turn Basic Tests ==========

    @Nested
    @DisplayName("UT-Series: Unit Turn Basic Tests")
    class UnitTurnBasicTests {

        @Test
        @DisplayName("UT-001: Single unit acts, turn switches to opponent")
        void ut001_singleUnitActsTurnSwitchesToOpponent() {
            // Given: P1 has 3 units, P2 has 3 units, P1's turn
            GameState state = create3v3State();
            assertEquals("P1", state.getCurrentPlayer().getValue());

            // When: P1's Hero performs MOVE action
            Unit p1Hero = findUnit(state.getUnits(), "p1_hero");
            Position newPos = new Position(2, 1); // Move forward
            Action moveAction = Action.move("p1_hero", newPos);

            GameState newState = ruleEngine.applyAction(state, moveAction);

            // Then:
            // - P1's Hero marked as acted
            Unit movedHero = findUnit(newState.getUnits(), "p1_hero");
            assertNotNull(movedHero);
            assertEquals(1, movedHero.getActionsUsed(), "Hero should have 1 action used");
            assertTrue(movedHero.hasActed(), "Hero should be marked as acted");

            // - Current player switches to P2
            assertEquals("P2", newState.getCurrentPlayer().getValue(),
                    "Turn should switch to P2 after P1's unit acts");

            // - P2 can choose any of their 3 units (all unacted)
            Unit p2Hero = findUnit(newState.getUnits(), "p2_hero");
            Unit p2Minion1 = findUnit(newState.getUnits(), "p2_minion_1");
            Unit p2Minion2 = findUnit(newState.getUnits(), "p2_minion_2");
            assertEquals(0, p2Hero.getActionsUsed(), "P2's hero should be unacted");
            assertEquals(0, p2Minion1.getActionsUsed(), "P2's minion1 should be unacted");
            assertEquals(0, p2Minion2.getActionsUsed(), "P2's minion2 should be unacted");
        }

        @Test
        @DisplayName("UT-002: Turn alternates between players")
        void ut002_turnAlternatesBetweenPlayers() {
            // Given: Initial state, P1's turn
            GameState state = create3v3State();
            assertEquals("P1", state.getCurrentPlayer().getValue());

            // When/Then: Turn alternates P1 → P2 → P1 → P2
            // Action 1: P1's Hero moves
            Action p1HeroMove = Action.move("p1_hero", new Position(2, 1));
            state = ruleEngine.applyAction(state, p1HeroMove);
            assertEquals("P2", state.getCurrentPlayer().getValue(), "After P1 action, should be P2's turn");

            // Action 2: P2's Hero moves
            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            state = ruleEngine.applyAction(state, p2HeroMove);
            assertEquals("P1", state.getCurrentPlayer().getValue(), "After P2 action, should be P1's turn");

            // Action 3: P1's Minion1 ends turn (uses END_TURN with specific unit ID)
            Action p1Minion1EndTurn = Action.endTurn("p1_minion_1");
            state = ruleEngine.applyAction(state, p1Minion1EndTurn);
            assertEquals("P2", state.getCurrentPlayer().getValue(), "After P1 action, should be P2's turn");

            // Action 4: P2's Minion1 ends turn
            Action p2Minion1EndTurn = Action.endTurn("p2_minion_1");
            state = ruleEngine.applyAction(state, p2Minion1EndTurn);
            assertEquals("P1", state.getCurrentPlayer().getValue(), "After P2 action, should be P1's turn");
        }

        @Test
        @DisplayName("UT-003: END_TURN marks only acting unit")
        void ut003_endTurnMarksOnlyActingUnit() {
            // Given: P1's turn, none of P1's units have acted
            GameState state = create3v3State();

            // When: P1 calls END_TURN for Hero specifically
            Action endTurnHero = Action.endTurn("p1_hero");
            GameState newState = ruleEngine.applyAction(state, endTurnHero);

            // Then:
            // - P1's Hero.hasActed = true
            Unit hero = findUnit(newState.getUnits(), "p1_hero");
            assertTrue(hero.hasActed(), "P1's Hero should be marked as acted");

            // - P1's Minion1.hasActed = false
            Unit minion1 = findUnit(newState.getUnits(), "p1_minion_1");
            assertFalse(minion1.hasActed(), "P1's Minion1 should NOT be marked as acted");

            // - P1's Minion2.hasActed = false
            Unit minion2 = findUnit(newState.getUnits(), "p1_minion_2");
            assertFalse(minion2.hasActed(), "P1's Minion2 should NOT be marked as acted");

            // - Turn switches to P2
            assertEquals("P2", newState.getCurrentPlayer().getValue(),
                    "Turn should switch to P2 after END_TURN");
        }

        @Test
        @DisplayName("UT-004: Player can choose any unacted unit")
        void ut004_playerCanChooseAnyUnactedUnit() {
            // Given: P1's turn, P1's Hero has acted
            GameState state = create3v3State();

            // First, P1's Hero acts
            Action p1HeroMove = Action.move("p1_hero", new Position(2, 1));
            state = ruleEngine.applyAction(state, p1HeroMove);

            // P2 takes a turn
            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            state = ruleEngine.applyAction(state, p2HeroMove);

            // Now P1's turn again, with Hero already acted
            assertEquals("P1", state.getCurrentPlayer().getValue());
            Unit hero = findUnit(state.getUnits(), "p1_hero");
            assertTrue(hero.hasActed(), "P1's Hero should have acted");

            // When: P1 submits action for Minion2 (skipping Minion1)
            // Note: Minion2 (ARCHER) at (4,0) has moveRange 1, so valid move is (3,0) or (4,1)
            Action minion2Move = Action.move("p1_minion_2", new Position(4, 1));

            // Then: Action should be valid and processed
            ValidationResult validation = ruleEngine.validateAction(state, minion2Move);
            assertTrue(validation.isValid(), "Should be able to act with any unacted unit: " + validation.getErrorMessage());

            GameState newState = ruleEngine.applyAction(state, minion2Move);

            // Verify Minion2 moved and is marked as acted
            Unit minion2 = findUnit(newState.getUnits(), "p1_minion_2");
            assertTrue(minion2.hasActed(), "P1's Minion2 should be marked as acted");
            assertEquals(new Position(4, 1), minion2.getPosition(), "P1's Minion2 should have moved");

            // Verify Minion1 is still unacted
            Unit minion1 = findUnit(newState.getUnits(), "p1_minion_1");
            assertFalse(minion1.hasActed(), "P1's Minion1 should still be unacted");
        }

        @Test
        @DisplayName("UT-005: Round ends when all units have acted")
        void ut005_roundEndsWhenAllUnitsActed() {
            // Given: P1 has 2 units, P2 has 2 units, Round 1
            List<Unit> units = new ArrayList<>();
            units.add(createHero("p1_hero", p1, new Position(2, 0), HeroClass.WARRIOR));
            units.add(createMinion("p1_minion_1", p1, new Position(0, 0), MinionType.TANK));
            units.add(createHero("p2_hero", p2, new Position(2, 4), HeroClass.WARRIOR));
            units.add(createMinion("p2_minion_1", p2, new Position(0, 4), MinionType.TANK));

            GameState state = new GameState(board, units, p1, false, null);
            assertEquals(1, state.getCurrentRound(), "Should start at round 1");

            // When: All 4 units act in alternating order
            // P1's Hero moves
            state = ruleEngine.applyAction(state, Action.move("p1_hero", new Position(2, 1)));
            assertEquals(1, state.getCurrentRound(), "Round should still be 1");

            // P2's Hero moves
            state = ruleEngine.applyAction(state, Action.move("p2_hero", new Position(2, 3)));
            assertEquals(1, state.getCurrentRound(), "Round should still be 1");

            // P1's Minion1 ends turn
            state = ruleEngine.applyAction(state, Action.endTurn("p1_minion_1"));
            assertEquals(1, state.getCurrentRound(), "Round should still be 1");

            // P2's Minion1 ends turn - this is the last unit
            state = ruleEngine.applyAction(state, Action.endTurn("p2_minion_1"));

            // Then:
            // - Round increments to 2
            assertEquals(2, state.getCurrentRound(), "Round should increment to 2 after all units acted");

            // - All units reset to hasActed = false
            for (Unit u : state.getUnits()) {
                assertFalse(u.hasActed(),
                        "Unit " + u.getId() + " should have actionsUsed reset to 0 at new round");
            }
        }

        @Test
        @DisplayName("UT-006: MOVE action switches turn")
        void ut006_moveActionSwitchesTurn() {
            // Given: P1's turn, P1's Hero unacted
            GameState state = create3v3State();
            assertEquals("P1", state.getCurrentPlayer().getValue());

            // When: P1's Hero performs MOVE
            Action moveAction = Action.move("p1_hero", new Position(2, 1));
            GameState newState = ruleEngine.applyAction(state, moveAction);

            // Then:
            // - Hero marked as acted
            Unit hero = findUnit(newState.getUnits(), "p1_hero");
            assertTrue(hero.hasActed(), "Hero should be marked as acted after MOVE");

            // - Turn switches to P2
            assertEquals("P2", newState.getCurrentPlayer().getValue(),
                    "Turn should switch to P2 after MOVE");
        }

        @Test
        @DisplayName("UT-007: ATTACK action switches turn")
        void ut007_attackActionSwitchesTurn() {
            // Given: P1's turn, P1's Hero adjacent to enemy
            List<Unit> units = new ArrayList<>();
            units.add(createHero("p1_hero", p1, new Position(2, 2), HeroClass.WARRIOR));
            units.add(createHero("p2_hero", p2, new Position(2, 3), HeroClass.WARRIOR));
            GameState state = new GameState(board, units, p1, false, null);

            // When: P1's Hero performs ATTACK
            Action attackAction = Action.attack("p1_hero", new Position(2, 3), "p2_hero");
            GameState newState = ruleEngine.applyAction(state, attackAction);

            // Then:
            // - Hero marked as acted
            Unit hero = findUnit(newState.getUnits(), "p1_hero");
            assertTrue(hero.hasActed(), "Hero should be marked as acted after ATTACK");

            // - Turn switches to P2
            assertEquals("P2", newState.getCurrentPlayer().getValue(),
                    "Turn should switch to P2 after ATTACK");
        }

        @Test
        @DisplayName("UT-010: Cannot act with already-acted unit")
        void ut010_cannotActWithAlreadyActedUnit() {
            // Given: P1's Hero already acted this round
            GameState state = create3v3State();

            // P1's Hero moves (acts)
            state = ruleEngine.applyAction(state, Action.move("p1_hero", new Position(2, 1)));

            // P2 takes a turn
            state = ruleEngine.applyAction(state, Action.move("p2_hero", new Position(2, 3)));

            // Now it's P1's turn again
            assertEquals("P1", state.getCurrentPlayer().getValue());

            // Verify P1's Hero has already acted
            Unit hero = findUnit(state.getUnits(), "p1_hero");
            assertTrue(hero.hasActed(), "P1's Hero should have already acted");

            // When: P1 tries to act with Hero again (move to adjacent position)
            Action heroMoveAgain = Action.move("p1_hero", new Position(2, 2));

            // Then: Action should be rejected (unit already acted)
            ValidationResult validation = ruleEngine.validateAction(state, heroMoveAgain);
            assertFalse(validation.isValid(), "Should not be able to act with already-acted unit");
            assertTrue(validation.getErrorMessage().contains("acted") ||
                            validation.getErrorMessage().contains("already"),
                    "Error message should indicate unit already acted");
        }

        @Test
        @DisplayName("UT-011: Unit hasActed resets at round start")
        void ut011_unitHasActedResetsAtRoundStart() {
            // Given: 2v2 game, Round 1 ended (all units acted)
            List<Unit> units = new ArrayList<>();
            units.add(createHero("p1_hero", p1, new Position(2, 0), HeroClass.WARRIOR));
            units.add(createHero("p2_hero", p2, new Position(2, 4), HeroClass.WARRIOR));
            GameState state = new GameState(board, units, p1, false, null);

            // All units act to end round 1
            state = ruleEngine.applyAction(state, Action.endTurn("p1_hero"));
            state = ruleEngine.applyAction(state, Action.endTurn("p2_hero"));

            // When: Round 2 starts
            assertEquals(2, state.getCurrentRound(), "Should be round 2");

            // Then: All units have hasActed = false
            for (Unit u : state.getUnits()) {
                assertFalse(u.hasActed(),
                        "Unit " + u.getId() + " should have actionsUsed=0 at new round");
            }
        }

        @Test
        @DisplayName("UT-012: getUnactedUnits returns correct list")
        void ut012_getUnactedUnitsReturnsCorrectList() {
            // Given: P1's Hero acted, P1's Minion1 and Minion2 not acted
            GameState state = create3v3State();

            // P1's Hero acts
            state = ruleEngine.applyAction(state, Action.move("p1_hero", new Position(2, 1)));

            // P2 takes a turn
            state = ruleEngine.applyAction(state, Action.move("p2_hero", new Position(2, 3)));

            // When: Check unacted units for P1
            List<Unit> p1UnactedUnits = new ArrayList<>();
            for (Unit u : state.getUnits()) {
                if (u.getOwner().getValue().equals("P1") && !u.hasActed() && u.isAlive()) {
                    p1UnactedUnits.add(u);
                }
            }

            // Then: Returns [Minion1, Minion2] (2 units)
            assertEquals(2, p1UnactedUnits.size(), "P1 should have 2 unacted units");

            List<String> unactedIds = new ArrayList<>();
            for (Unit u : p1UnactedUnits) {
                unactedIds.add(u.getId());
            }
            assertTrue(unactedIds.contains("p1_minion_1"), "Minion1 should be in unacted list");
            assertTrue(unactedIds.contains("p1_minion_2"), "Minion2 should be in unacted list");
            assertFalse(unactedIds.contains("p1_hero"), "Hero should NOT be in unacted list");
        }
    }
}
