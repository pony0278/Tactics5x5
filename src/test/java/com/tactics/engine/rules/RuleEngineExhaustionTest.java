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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UE-Series: Exhaustion Rule Tests
 *
 * Tests for the Exhaustion Rule in unit-by-unit turn system:
 * - When one player has no unacted units, opponent takes consecutive turns
 * - Exhaustion ends when round ends
 * - Timer resets for each consecutive action
 */
@DisplayName("UE-Series: Exhaustion Rule Tests")
class RuleEngineExhaustionTest {

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

    private GameState createStateWithBuffs(List<Unit> units, PlayerId currentPlayer,
                                           Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, currentPlayer, false, null, unitBuffs);
    }

    // ========== UE-Series Tests ==========

    @Nested
    @DisplayName("UE-001 ~ UE-004: Basic Exhaustion")
    class BasicExhaustion {

        @Test
        @DisplayName("UE-001: Exhaustion - opponent takes consecutive turns")
        void ue001_exhaustionOpponentTakesConsecutiveTurns() {
            // Given: P1 has 1 unit (Hero), P2 has 3 units
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));
            Unit p2Minion2 = createUnit("p2_minion_2", p2, new Position(4, 4));

            GameState state = createState(
                Arrays.asList(p1Hero, p2Hero, p2Minion1, p2Minion2), p1);

            // When: P1's Hero acts
            Action p1Move = Action.move("p1_hero", new Position(2, 1));
            GameState afterP1 = ruleEngine.applyAction(state, p1Move);

            // Then: P2's turn
            assertEquals("P2", afterP1.getCurrentPlayer().getValue(),
                "After P1 acts, turn switches to P2");

            // P2's Hero acts
            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2Hero = ruleEngine.applyAction(afterP1, p2HeroMove);

            // Then: Still P2's turn (P1 is exhausted)
            assertEquals("P2", afterP2Hero.getCurrentPlayer().getValue(),
                "P1 exhausted, P2 takes consecutive turn");

            // P2's Minion1 acts
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(1, 4));
            GameState afterP2Minion1 = ruleEngine.applyAction(afterP2Hero, p2Minion1Move);

            // Then: Still P2's turn
            assertEquals("P2", afterP2Minion1.getCurrentPlayer().getValue(),
                "P1 still exhausted, P2 continues");

            // P2's Minion2 acts (last unit) - should trigger round end
            Action p2Minion2Move = Action.move("p2_minion_2", new Position(3, 4));
            GameState afterRoundEnd = ruleEngine.applyAction(afterP2Minion1, p2Minion2Move);

            // Then: Round ends, new round starts
            // All units should be reset
            Unit p1HeroAfter = findUnit(afterRoundEnd.getUnits(), "p1_hero");
            assertEquals(0, p1HeroAfter.getActionsUsed(),
                "P1 Hero actionsUsed reset after round end");
        }

        @Test
        @DisplayName("UE-002: Exhaustion - each consecutive action gets fresh timer")
        void ue002_exhaustionEachActionGetsFreshTimer() {
            // This test verifies that each action in exhaustion scenario is independent
            // Timer tests are handled in MatchServiceTimerTest, here we verify turn stays

            // Given: P1 has 0 unacted units, P2 has 2 unacted units
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0))
                .withActionsUsed(1); // Already acted
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));

            GameState state = createState(Arrays.asList(p1Hero, p2Hero, p2Minion1), p2);

            // When: P2's Hero acts
            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2Hero = ruleEngine.applyAction(state, p2HeroMove);

            // Then: P2's turn continues (P1 exhausted)
            assertEquals("P2", afterP2Hero.getCurrentPlayer().getValue(),
                "P2 takes consecutive turn due to P1 exhaustion");

            // Verify P2's Hero is now acted
            Unit p2HeroAfter = findUnit(afterP2Hero.getUnits(), "p2_hero");
            assertTrue(p2HeroAfter.hasActed(), "P2 Hero should be marked as acted");

            // P2 can still act with Minion1
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(1, 4));
            ValidationResult result = ruleEngine.validateAction(afterP2Hero, p2Minion1Move);
            assertTrue(result.isValid(), "P2 Minion1 can act in exhaustion scenario");
        }

        @Test
        @DisplayName("UE-003: Exhaustion ends when round ends")
        void ue003_exhaustionEndsWhenRoundEnds() {
            // Given: P2 took consecutive turns due to P1 exhaustion
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 1))
                .withActionsUsed(1);
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 3))
                .withActionsUsed(1);
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(1, 4));

            // P2's turn, P2 has 1 unacted unit (last one)
            GameState state = createState(Arrays.asList(p1Hero, p2Hero, p2Minion1), p2);

            // When: P2's Minion1 acts (last unit of round)
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(0, 4));
            GameState afterRoundEnd = ruleEngine.applyAction(state, p2Minion1Move);

            // Then: New round starts
            Unit p1HeroAfter = findUnit(afterRoundEnd.getUnits(), "p1_hero");
            Unit p2HeroAfter = findUnit(afterRoundEnd.getUnits(), "p2_hero");
            Unit p2Minion1After = findUnit(afterRoundEnd.getUnits(), "p2_minion_1");

            // All units reset
            assertEquals(0, p1HeroAfter.getActionsUsed(), "P1 Hero reset for new round");
            assertEquals(0, p2HeroAfter.getActionsUsed(), "P2 Hero reset for new round");
            assertEquals(0, p2Minion1After.getActionsUsed(), "P2 Minion1 reset for new round");

            // P1 can now act again (normal alternation resumes)
            Action p1Action = Action.move("p1_hero", new Position(2, 2));
            ValidationResult result = ruleEngine.validateAction(afterRoundEnd, p1Action);
            assertTrue(result.isValid(), "P1 can act in new round");
        }

        @Test
        @DisplayName("UE-004: Both players have units - no exhaustion")
        void ue004_bothPlayersHaveUnitsNoExhaustion() {
            // Given: Both players have 2 unacted units
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));

            GameState state = createState(
                Arrays.asList(p1Hero, p1Minion1, p2Hero, p2Minion1), p1);

            // When: P1's Hero acts
            Action p1Move = Action.move("p1_hero", new Position(2, 1));
            GameState afterP1 = ruleEngine.applyAction(state, p1Move);

            // Then: Turn switches to P2 (no consecutive turns)
            assertEquals("P2", afterP1.getCurrentPlayer().getValue(),
                "Turn switches to P2, no exhaustion");

            // P2's Hero acts
            Action p2Move = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2 = ruleEngine.applyAction(afterP1, p2Move);

            // Then: Turn switches back to P1 (normal alternation)
            assertEquals("P1", afterP2.getCurrentPlayer().getValue(),
                "Turn switches back to P1, normal alternation");
        }
    }

    @Nested
    @DisplayName("UE-005 ~ UE-008: Advanced Exhaustion Scenarios")
    class AdvancedExhaustion {

        @Test
        @DisplayName("UE-005: Exhaustion with SPEED buff")
        void ue005_exhaustionWithSpeedBuff() {
            // Given: P1 has 1 unit (Hero with SPEED), P2 has 2 units
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));

            // Add SPEED buff to P1's Hero
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance speedBuff = BuffFactory.createSpeed("test_source");
            unitBuffs.put("p1_hero", Collections.singletonList(speedBuff));

            GameState state = createStateWithBuffs(
                Arrays.asList(p1Hero, p2Hero, p2Minion1), p1, unitBuffs);

            // When: P1's Hero acts (action 1)
            Action p1Action1 = Action.move("p1_hero", new Position(2, 1));
            GameState afterAction1 = ruleEngine.applyAction(state, p1Action1);

            // Then: Still P1's turn (SPEED consecutive)
            assertEquals("P1", afterAction1.getCurrentPlayer().getValue(),
                "SPEED buff: P1 keeps turn for second action");

            // P1's Hero acts (action 2)
            Action p1Action2 = Action.move("p1_hero", new Position(2, 2));
            GameState afterAction2 = ruleEngine.applyAction(afterAction1, p1Action2);

            // Then: P1 now exhausted, P2's turn
            assertEquals("P2", afterAction2.getCurrentPlayer().getValue(),
                "After SPEED actions, turn switches to P2");

            // P2's Hero acts
            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2Hero = ruleEngine.applyAction(afterAction2, p2HeroMove);

            // Then: Still P2's turn (P1 exhausted)
            assertEquals("P2", afterP2Hero.getCurrentPlayer().getValue(),
                "P1 exhausted, P2 takes consecutive turn");

            // P2's Minion1 acts - last unit, round ends
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(1, 4));
            GameState afterRoundEnd = ruleEngine.applyAction(afterP2Hero, p2Minion1Move);

            // Verify round ended (all units reset)
            Unit p1HeroAfter = findUnit(afterRoundEnd.getUnits(), "p1_hero");
            assertEquals(0, p1HeroAfter.getActionsUsed(), "P1 Hero reset after round end");
        }

        @Test
        @DisplayName("UE-006: Exhaustion after unit death")
        void ue006_exhaustionAfterUnitDeath() {
            // Given: P1 has 2 units (Hero, Minion1), P2 has 2 units
            // P1's Hero already acted
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 1))
                .withActionsUsed(1);
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(3, 1), 1); // 1 HP
            Unit p2Hero = createUnit("p2_hero", p2, new Position(3, 2));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));

            GameState state = createState(
                Arrays.asList(p1Hero, p1Minion1, p2Hero, p2Minion1), p2);

            // When: P2 kills P1's Minion1 (the only unacted P1 unit)
            Action p2Attack = Action.attack("p2_hero", new Position(3, 1), "p1_minion_1");
            GameState afterKill = ruleEngine.applyAction(state, p2Attack);

            // Then: P1's Minion1 is dead
            Unit p1Minion1After = findUnit(afterKill.getUnits(), "p1_minion_1");
            assertFalse(p1Minion1After.isAlive(), "P1 Minion1 should be dead");

            // P1 is now exhausted (Hero acted, Minion1 dead)
            // So P2 continues
            assertEquals("P2", afterKill.getCurrentPlayer().getValue(),
                "P1 exhausted after unit death, P2 continues");

            // P2's Minion1 can act
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(1, 4));
            ValidationResult result = ruleEngine.validateAction(afterKill, p2Minion1Move);
            assertTrue(result.isValid(), "P2 Minion1 can act, P1 exhausted");
        }

        @Test
        @DisplayName("UE-007: Exhaustion alternates if both have 1 unit each")
        void ue007_bothHaveOneUnitEachNoExhaustion() {
            // Given: P1 has 1 unit, P2 has 1 unit
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createState(Arrays.asList(p1Hero, p2Hero), p1);

            // When: P1's Hero acts
            Action p1Move = Action.move("p1_hero", new Position(2, 1));
            GameState afterP1 = ruleEngine.applyAction(state, p1Move);

            // Then: Turn switches to P2 (no exhaustion - P2 also has only 1)
            assertEquals("P2", afterP1.getCurrentPlayer().getValue(),
                "Turn switches to P2, no exhaustion when counts equal");

            // When: P2's Hero acts
            Action p2Move = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2 = ruleEngine.applyAction(afterP1, p2Move);

            // Then: Round ends (both have acted)
            Unit p1HeroAfter = findUnit(afterP2.getUnits(), "p1_hero");
            assertEquals(0, p1HeroAfter.getActionsUsed(), "Round ended, P1 Hero reset");
        }

        @Test
        @DisplayName("UE-008: Exhaustion with 0 units = round ends")
        void ue008_noUnactedUnitsRoundEnds() {
            // Given: All units have acted (P1: 1, P2: 1)
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 1))
                .withActionsUsed(1);
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 3));

            // P2's turn, P2's hero is the last unacted unit
            GameState state = createState(Arrays.asList(p1Hero, p2Hero), p2);

            // When: Last unit acts
            Action p2Move = Action.move("p2_hero", new Position(2, 2));
            GameState afterLastAction = ruleEngine.applyAction(state, p2Move);

            // Then: Round ends, no exhaustion
            Unit p1HeroAfter = findUnit(afterLastAction.getUnits(), "p1_hero");
            Unit p2HeroAfter = findUnit(afterLastAction.getUnits(), "p2_hero");

            assertEquals(0, p1HeroAfter.getActionsUsed(), "Round ended, P1 reset");
            assertEquals(0, p2HeroAfter.getActionsUsed(), "Round ended, P2 reset");
        }
    }
}
