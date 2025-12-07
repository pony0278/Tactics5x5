package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-Series: SPEED Buff Turn Tests
 *
 * Tests for SPEED buff mechanics in unit-by-unit turn system:
 * - SPEED unit gets 2 consecutive actions before turn switches
 * - Cannot switch to different unit mid-SPEED
 * - END_TURN forfeits remaining SPEED action
 * - Timer resets for each SPEED action
 */
@DisplayName("US-Series: SPEED Buff Turn Tests")
class RuleEngineSpeedTurnTest {

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

    // ========== Helper Methods ==========

    private Unit createUnit(String id, PlayerId owner, Position pos) {
        return new Unit(id, owner, 10, 3, 1, 1, pos, true);
    }

    private Unit createUnit(String id, PlayerId owner, Position pos, int hp) {
        return new Unit(id, owner, hp, 3, 1, 1, pos, true);
    }

    private Unit findUnit(List<Unit> units, String id) {
        return units.stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    private GameState createState(List<Unit> units, PlayerId currentPlayer) {
        return new GameState(board, units, currentPlayer, false, null, Collections.emptyMap());
    }

    private GameState createStateWithSpeedBuff(List<Unit> units, PlayerId currentPlayer, String speedUnitId) {
        Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
        BuffInstance speedBuff = BuffFactory.createSpeed("test_source");
        unitBuffs.put(speedUnitId, Collections.singletonList(speedBuff));
        return new GameState(board, units, currentPlayer, false, null, unitBuffs);
    }

    // ========== US-Series Tests ==========

    @Nested
    @DisplayName("US-001 ~ US-002: SPEED Consecutive Actions")
    class SpeedConsecutiveActions {

        @Test
        @DisplayName("US-001: SPEED unit acts twice consecutively")
        void us001_speedUnitActsTwiceConsecutively() {
            // Given: P1's Hero has SPEED buff, P2 has units available
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createStateWithSpeedBuff(
                Arrays.asList(p1Hero, p1Minion1, p2Hero), p1, "p1_hero");

            // When: P1's Hero performs first action
            Action move1 = Action.move("p1_hero", new Position(2, 1));
            GameState afterAction1 = ruleEngine.applyAction(state, move1);

            // Then: Hero NOT marked as fully acted
            Unit p1HeroAfter = findUnit(afterAction1.getUnits(), "p1_hero");
            assertEquals(1, p1HeroAfter.getActionsUsed(),
                "Hero should have 1 action used, not fully acted");

            // Turn stays with P1
            assertEquals("P1", afterAction1.getCurrentPlayer().getValue(),
                "Turn stays with P1 for second SPEED action");

            // P1 must act with same Hero again (verify second action is valid)
            Action move2 = Action.move("p1_hero", new Position(2, 2));
            ValidationResult result = ruleEngine.validateAction(afterAction1, move2);
            assertTrue(result.isValid(), "Second SPEED action should be valid");
        }

        @Test
        @DisplayName("US-002: SPEED - second action completes turn")
        void us002_speedSecondActionCompletesTurn() {
            // Given: P1's Hero has SPEED buff and performed first action
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createStateWithSpeedBuff(
                Arrays.asList(p1Hero, p2Hero), p1, "p1_hero");

            // First action
            Action move1 = Action.move("p1_hero", new Position(2, 1));
            GameState afterAction1 = ruleEngine.applyAction(state, move1);

            // When: P1's Hero performs second action
            Action move2 = Action.move("p1_hero", new Position(2, 2));
            GameState afterAction2 = ruleEngine.applyAction(afterAction1, move2);

            // Then: Hero marked as acted
            Unit p1HeroAfter = findUnit(afterAction2.getUnits(), "p1_hero");
            assertEquals(2, p1HeroAfter.getActionsUsed(),
                "Hero should have 2 actions used (fully acted)");

            // Turn switches to P2
            assertEquals("P2", afterAction2.getCurrentPlayer().getValue(),
                "Turn switches to P2 after SPEED actions complete");
        }
    }

    @Nested
    @DisplayName("US-003 ~ US-004: SPEED Unit Constraints")
    class SpeedUnitConstraints {

        @Test
        @DisplayName("US-003: SPEED - cannot switch to different unit mid-SPEED")
        void us003_cannotSwitchUnitMidSpeed() {
            // Given: P1's Hero has SPEED buff and performed first action
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 1))
                .withActionsUsed(1); // Simulating after first action
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createStateWithSpeedBuff(
                Arrays.asList(p1Hero, p1Minion1, p2Hero), p1, "p1_hero");

            // When: P1 tries to act with Minion1 (not the SPEED unit)
            Action minionMove = Action.move("p1_minion_1", new Position(1, 0));
            ValidationResult result = ruleEngine.validateAction(state, minionMove);

            // Then: Action rejected (must complete SPEED actions with Hero)
            assertFalse(result.isValid(),
                "Should not be able to switch units mid-SPEED");
            assertTrue(result.getErrorMessage().toLowerCase().contains("speed") ||
                       result.getErrorMessage().toLowerCase().contains("must complete"),
                "Error should mention SPEED or must complete: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("US-004: SPEED - END_TURN forfeits remaining action")
        void us004_speedEndTurnForfeitsRemainingAction() {
            // Given: P1's Hero has SPEED buff and performed first action
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createStateWithSpeedBuff(
                Arrays.asList(p1Hero, p2Hero), p1, "p1_hero");

            // First action
            Action move1 = Action.move("p1_hero", new Position(2, 1));
            GameState afterAction1 = ruleEngine.applyAction(state, move1);

            // Verify still P1's turn
            assertEquals("P1", afterAction1.getCurrentPlayer().getValue());

            // When: P1 calls END_TURN for Hero (forfeiting second action)
            Action endTurn = Action.endTurn("p1_hero");
            GameState afterEndTurn = ruleEngine.applyAction(afterAction1, endTurn);

            // Then: Hero marked as acted (forfeits second action)
            Unit p1HeroAfter = findUnit(afterEndTurn.getUnits(), "p1_hero");
            assertTrue(p1HeroAfter.hasActed(),
                "Hero should be marked as acted after END_TURN");

            // Turn switches to P2
            assertEquals("P2", afterEndTurn.getCurrentPlayer().getValue(),
                "Turn switches to P2 after END_TURN");
        }
    }

    @Nested
    @DisplayName("US-005 ~ US-006: SPEED Timer and Integration")
    class SpeedTimerAndIntegration {

        @Test
        @DisplayName("US-005: SPEED - timer resets for each action")
        void us005_speedTimerResetsForEachAction() {
            // Timer behavior is tested in MatchServiceTimerTest
            // Here we verify that the game state correctly tracks SPEED actions

            // Given: P1's Hero has SPEED buff
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createStateWithSpeedBuff(
                Arrays.asList(p1Hero, p2Hero), p1, "p1_hero");

            // When: First action
            Action move1 = Action.move("p1_hero", new Position(2, 1));
            GameState afterAction1 = ruleEngine.applyAction(state, move1);

            // Then: State correctly reflects first action used
            Unit p1HeroAfter1 = findUnit(afterAction1.getUnits(), "p1_hero");
            assertEquals(1, p1HeroAfter1.getActionsUsed(),
                "First action: actionsUsed = 1");
            assertEquals("P1", afterAction1.getCurrentPlayer().getValue(),
                "P1 still has second action");

            // Second action
            Action move2 = Action.move("p1_hero", new Position(2, 2));
            GameState afterAction2 = ruleEngine.applyAction(afterAction1, move2);

            // State correctly reflects second action
            Unit p1HeroAfter2 = findUnit(afterAction2.getUnits(), "p1_hero");
            assertEquals(2, p1HeroAfter2.getActionsUsed(),
                "Second action: actionsUsed = 2");
            assertEquals("P2", afterAction2.getCurrentPlayer().getValue(),
                "Turn switches after both actions");
        }

        @Test
        @DisplayName("US-006: SPEED + Exhaustion interaction")
        void us006_speedPlusExhaustionInteraction() {
            // Given: P1's Hero has SPEED buff (only unit), P2 has 2 units
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));

            GameState state = createStateWithSpeedBuff(
                Arrays.asList(p1Hero, p2Hero, p2Minion1), p1, "p1_hero");

            // Turn 1: P1's Hero (action 1)
            Action p1Action1 = Action.move("p1_hero", new Position(2, 1));
            GameState afterP1Action1 = ruleEngine.applyAction(state, p1Action1);
            assertEquals("P1", afterP1Action1.getCurrentPlayer().getValue(),
                "Step 1: P1's Hero (SPEED action 1)");

            // Turn 2: P1's Hero (action 2) - SPEED
            Action p1Action2 = Action.move("p1_hero", new Position(2, 2));
            GameState afterP1Action2 = ruleEngine.applyAction(afterP1Action1, p1Action2);
            assertEquals("P2", afterP1Action2.getCurrentPlayer().getValue(),
                "Step 2: After SPEED actions, turn switches to P2");

            // Turn 3: P2's Hero - P1 is now exhausted
            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2Hero = ruleEngine.applyAction(afterP1Action2, p2HeroMove);
            assertEquals("P2", afterP2Hero.getCurrentPlayer().getValue(),
                "Step 3: P2's Hero acts, P1 exhausted, P2 continues");

            // Turn 4: P2's Minion1 - P1 still exhausted
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(1, 4));
            GameState afterP2Minion1 = ruleEngine.applyAction(afterP2Hero, p2Minion1Move);

            // Round ends
            Unit p1HeroAfter = findUnit(afterP2Minion1.getUnits(), "p1_hero");
            assertEquals(0, p1HeroAfter.getActionsUsed(),
                "Round ended, P1 Hero reset");
        }
    }
}
