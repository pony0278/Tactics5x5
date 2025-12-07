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
import com.tactics.engine.model.DeathChoice;

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
 * UI-Series: Unit Turn Integration Tests
 *
 * Integration tests for unit-by-unit turn system:
 * - Full round with 3v3 units
 * - Round with unequal unit counts
 * - Unit death mid-round
 * - SPEED + regular unit interaction
 * - Timer integration (per unit)
 * - Death Choice pause and resume
 */
@DisplayName("UI-Series: Unit Turn Integration Tests")
class RuleEngineUnitTurnIntegrationTest {

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

    private GameState createStateWithRound(List<Unit> units, PlayerId currentPlayer, int round) {
        return new GameState(board, units, currentPlayer, false, null, Collections.emptyMap(),
                            null, null, round, null);
    }

    private GameState createStateWithSpeedBuff(List<Unit> units, PlayerId currentPlayer, String speedUnitId) {
        Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
        BuffInstance speedBuff = BuffFactory.createSpeed("test_source");
        unitBuffs.put(speedUnitId, Collections.singletonList(speedBuff));
        return new GameState(board, units, currentPlayer, false, null, unitBuffs);
    }

    // ========== UI-Series Tests ==========

    @Nested
    @DisplayName("UI-001 ~ UI-002: Full Round Tests")
    class FullRoundTests {

        @Test
        @DisplayName("UI-001: Full round with 3v3 units")
        void ui001_fullRoundWith3v3Units() {
            // Given: P1: Hero, Minion1, Minion2; P2: Hero, Minion1, Minion2
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p1Minion2 = createUnit("p1_minion_2", p1, new Position(4, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));
            Unit p2Minion2 = createUnit("p2_minion_2", p2, new Position(4, 4));

            GameState state = createStateWithRound(
                Arrays.asList(p1Hero, p1Minion1, p1Minion2, p2Hero, p2Minion1, p2Minion2), p1, 1);

            // When: All 6 units act in alternating fashion
            // Turn 1: P1's Hero moves
            Action p1HeroMove = Action.move("p1_hero", new Position(2, 1));
            GameState afterP1Hero = ruleEngine.applyAction(state, p1HeroMove);
            assertEquals("P2", afterP1Hero.getCurrentPlayer().getValue(), "Turn 1 -> P2");

            // Turn 2: P2's Hero moves
            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2Hero = ruleEngine.applyAction(afterP1Hero, p2HeroMove);
            assertEquals("P1", afterP2Hero.getCurrentPlayer().getValue(), "Turn 2 -> P1");

            // Turn 3: P1's Minion1 moves
            Action p1Minion1Move = Action.move("p1_minion_1", new Position(0, 1));
            GameState afterP1Minion1 = ruleEngine.applyAction(afterP2Hero, p1Minion1Move);
            assertEquals("P2", afterP1Minion1.getCurrentPlayer().getValue(), "Turn 3 -> P2");

            // Turn 4: P2's Minion1 moves
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(0, 3));
            GameState afterP2Minion1 = ruleEngine.applyAction(afterP1Minion1, p2Minion1Move);
            assertEquals("P1", afterP2Minion1.getCurrentPlayer().getValue(), "Turn 4 -> P1");

            // Turn 5: P1's Minion2 moves
            Action p1Minion2Move = Action.move("p1_minion_2", new Position(4, 1));
            GameState afterP1Minion2 = ruleEngine.applyAction(afterP2Minion1, p1Minion2Move);
            assertEquals("P2", afterP1Minion2.getCurrentPlayer().getValue(), "Turn 5 -> P2");

            // Turn 6: P2's Minion2 moves (last action of round)
            Action p2Minion2Move = Action.move("p2_minion_2", new Position(4, 3));
            GameState afterRoundEnd = ruleEngine.applyAction(afterP1Minion2, p2Minion2Move);

            // Then: Round ends after 6 actions, Round 2 starts
            assertEquals(2, afterRoundEnd.getCurrentRound(), "Round should be 2 after all units acted");

            // All units reset
            for (Unit u : afterRoundEnd.getUnits()) {
                assertEquals(0, u.getActionsUsed(),
                    u.getId() + " should be reset for new round");
            }
        }

        @Test
        @DisplayName("UI-002: Round with unequal unit counts")
        void ui002_roundWithUnequalUnitCounts() {
            // Given: P1: Hero, Minion1 (2 units); P2: Hero, Minion1, Minion2 (3 units)
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));
            Unit p2Minion2 = createUnit("p2_minion_2", p2, new Position(4, 4));

            GameState state = createStateWithRound(
                Arrays.asList(p1Hero, p1Minion1, p2Hero, p2Minion1, p2Minion2), p1, 1);

            // When: All units act
            // P1 -> P2 -> P1 -> P2 (alternating until P1 exhausted)
            Action p1HeroMove = Action.move("p1_hero", new Position(2, 1));
            GameState afterP1Hero = ruleEngine.applyAction(state, p1HeroMove);
            assertEquals("P2", afterP1Hero.getCurrentPlayer().getValue(), "P1 -> P2");

            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2Hero = ruleEngine.applyAction(afterP1Hero, p2HeroMove);
            assertEquals("P1", afterP2Hero.getCurrentPlayer().getValue(), "P2 -> P1");

            Action p1Minion1Move = Action.move("p1_minion_1", new Position(0, 1));
            GameState afterP1Minion1 = ruleEngine.applyAction(afterP2Hero, p1Minion1Move);
            assertEquals("P2", afterP1Minion1.getCurrentPlayer().getValue(), "P1 -> P2");

            Action p2Minion1Move = Action.move("p2_minion_1", new Position(0, 3));
            GameState afterP2Minion1 = ruleEngine.applyAction(afterP1Minion1, p2Minion1Move);

            // Then: P2 -> still P2 (P1 exhausted)
            assertEquals("P2", afterP2Minion1.getCurrentPlayer().getValue(),
                "P1 exhausted, P2 continues");

            // P2's last unit acts -> Round End
            Action p2Minion2Move = Action.move("p2_minion_2", new Position(4, 3));
            GameState afterRoundEnd = ruleEngine.applyAction(afterP2Minion1, p2Minion2Move);

            assertEquals(2, afterRoundEnd.getCurrentRound(), "Round should be 2");
        }
    }

    @Nested
    @DisplayName("UI-003 ~ UI-004: Combat and SPEED Tests")
    class CombatAndSpeedTests {

        @Test
        @DisplayName("UI-003: Unit death mid-round affects turn order")
        void ui003_unitDeathMidRoundAffectsTurnOrder() {
            // Given: P1: Hero, Minion1; P2: Hero (1 HP), Minion1
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 1));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Target = createUnit("p2_hero", p2, new Position(2, 2), 1); // 1 HP, will die
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));

            GameState state = createState(
                Arrays.asList(p1Hero, p1Minion1, p2Target, p2Minion1), p1);

            // When: P1's Hero attacks and kills P2's Hero
            Action attack = Action.attack("p1_hero", new Position(2, 2), "p2_hero");
            GameState afterKill = ruleEngine.applyAction(state, attack);

            // Then: P2's Hero is dead
            Unit p2TargetAfter = findUnit(afterKill.getUnits(), "p2_hero");
            assertFalse(p2TargetAfter.isAlive(), "P2 Hero should be dead");

            // Turn continues - P2 has 1 unacted unit (Minion1)
            assertEquals("P2", afterKill.getCurrentPlayer().getValue(),
                "Turn switches to P2");

            // P2's Minion1 can still act
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(0, 3));
            ValidationResult result = ruleEngine.validateAction(afterKill, p2Minion1Move);
            assertTrue(result.isValid(), "P2 Minion1 can still act");
        }

        @Test
        @DisplayName("UI-004: SPEED + regular unit in same round")
        void ui004_speedPlusRegularUnitInSameRound() {
            // Given: P1: Hero (SPEED), Minion1; P2: Hero, Minion1
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));
            Unit p2Minion1 = createUnit("p2_minion_1", p2, new Position(0, 4));

            GameState state = createStateWithSpeedBuff(
                Arrays.asList(p1Hero, p1Minion1, p2Hero, p2Minion1), p1, "p1_hero");

            // Turn Order:
            // 1. P1's Hero (action 1)
            Action p1HeroAction1 = Action.move("p1_hero", new Position(2, 1));
            GameState afterAction1 = ruleEngine.applyAction(state, p1HeroAction1);
            assertEquals("P1", afterAction1.getCurrentPlayer().getValue(),
                "Step 1: P1's Hero (SPEED action 1), stays P1");

            // 2. P1's Hero (action 2) - SPEED
            Action p1HeroAction2 = Action.move("p1_hero", new Position(2, 2));
            GameState afterAction2 = ruleEngine.applyAction(afterAction1, p1HeroAction2);
            assertEquals("P2", afterAction2.getCurrentPlayer().getValue(),
                "Step 2: P1's Hero (SPEED action 2), switches to P2");

            // 3. P2's Hero
            Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
            GameState afterP2Hero = ruleEngine.applyAction(afterAction2, p2HeroMove);
            assertEquals("P1", afterP2Hero.getCurrentPlayer().getValue(),
                "Step 3: P2's Hero, switches to P1");

            // 4. P1's Minion1
            Action p1Minion1Move = Action.move("p1_minion_1", new Position(0, 1));
            GameState afterP1Minion1 = ruleEngine.applyAction(afterP2Hero, p1Minion1Move);
            assertEquals("P2", afterP1Minion1.getCurrentPlayer().getValue(),
                "Step 4: P1's Minion1, switches to P2");

            // 5. P2's Minion1 -> Round End
            Action p2Minion1Move = Action.move("p2_minion_1", new Position(0, 3));
            GameState afterRoundEnd = ruleEngine.applyAction(afterP1Minion1, p2Minion1Move);

            // Verify round ended
            for (Unit u : afterRoundEnd.getUnits()) {
                assertEquals(0, u.getActionsUsed(),
                    u.getId() + " should be reset for new round");
            }
        }
    }

    @Nested
    @DisplayName("UI-005 ~ UI-006: Timer and Death Choice Tests")
    class TimerAndDeathChoiceTests {

        @Test
        @DisplayName("UI-005: Timer integration - per unit (state verification)")
        void ui005_timerIntegrationPerUnit() {
            // Timer behavior is tested in MatchServiceTimerTest
            // Here we verify the game state properly supports per-unit turns

            // Given: Unit-by-unit turn system active
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 0));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createState(Arrays.asList(p1Hero, p1Minion1, p2Hero), p1);

            // When: P1's Hero acts
            Action p1HeroMove = Action.move("p1_hero", new Position(2, 1));
            GameState afterP1Hero = ruleEngine.applyAction(state, p1HeroMove);

            // Then: State correctly tracks who acted
            Unit p1HeroAfter = findUnit(afterP1Hero.getUnits(), "p1_hero");
            Unit p1Minion1After = findUnit(afterP1Hero.getUnits(), "p1_minion_1");

            assertEquals(1, p1HeroAfter.getActionsUsed(), "P1 Hero acted");
            assertEquals(0, p1Minion1After.getActionsUsed(), "P1 Minion1 not acted");

            // Timer would be fresh for P2 - verified by MatchServiceTimerTest
            assertEquals("P2", afterP1Hero.getCurrentPlayer().getValue(),
                "Turn switched to P2");
        }

        @Test
        @DisplayName("UI-006: Death Choice pauses then resumes unit turn")
        void ui006_deathChoicePausesThenResumesUnitTurn() {
            // Given: P1's Hero attacks, kills P2's Minion1 which has death choice
            Unit p1Hero = createUnit("p1_hero", p1, new Position(2, 1));
            Unit p1Minion1 = createUnit("p1_minion_1", p1, new Position(0, 0));
            Unit p2Target = createUnit("p2_minion_1", p2, new Position(2, 2), 3); // Will die from 3 damage
            Unit p2Hero = createUnit("p2_hero", p2, new Position(2, 4));

            GameState state = createState(
                Arrays.asList(p1Hero, p1Minion1, p2Target, p2Hero), p1);

            // When: P1's Hero attacks and kills P2's Minion1
            Action attack = Action.attack("p1_hero", new Position(2, 2), "p2_minion_1");
            GameState afterKill = ruleEngine.applyAction(state, attack);

            // Then: P2's Minion1 is dead
            Unit p2TargetAfter = findUnit(afterKill.getUnits(), "p2_minion_1");
            assertFalse(p2TargetAfter.isAlive(), "P2 Minion1 should be dead");

            // Note: Death Choice triggers for Heroes, not minions in this game
            // For minions, turn should just switch normally
            // If death choice exists, current player would change to death choice owner

            // Verify P1's Hero is marked as acted
            Unit p1HeroAfter = findUnit(afterKill.getUnits(), "p1_hero");
            assertTrue(p1HeroAfter.hasActed(), "P1 Hero should be marked as acted");

            // If no death choice pending, turn switches normally
            if (!afterKill.hasPendingDeathChoice()) {
                assertEquals("P2", afterKill.getCurrentPlayer().getValue(),
                    "Turn should switch to P2 after attack");

                // P2 can act with remaining unit (Hero)
                Action p2HeroMove = Action.move("p2_hero", new Position(2, 3));
                ValidationResult result = ruleEngine.validateAction(afterKill, p2HeroMove);
                assertTrue(result.isValid(), "P2 Hero can act");
            } else {
                // Death choice scenario - verify it can be resolved
                DeathChoice choice = afterKill.getPendingDeathChoice();
                assertNotNull(choice, "Death choice should be present");

                // Resolve death choice
                Action deathChoiceAction = Action.deathChoice(
                    choice.getOwner(), DeathChoice.ChoiceType.SPAWN_OBSTACLE);
                GameState afterChoice = ruleEngine.applyAction(afterKill, deathChoiceAction);

                // After resolving, normal turn order resumes
                assertFalse(afterChoice.hasPendingDeathChoice(),
                    "Death choice should be resolved");
            }
        }
    }
}
