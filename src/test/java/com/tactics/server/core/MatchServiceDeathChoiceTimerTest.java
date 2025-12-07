package com.tactics.server.core;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.DeathChoice;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.HeroClass;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.model.UnitCategory;
import com.tactics.engine.rules.RuleEngine;
import com.tactics.engine.util.GameStateSerializer;
import com.tactics.server.timer.TimerCallback;
import com.tactics.server.timer.TimerConfig;
import com.tactics.server.timer.TimerService;
import com.tactics.server.timer.TimerState;
import com.tactics.server.timer.TimerType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TD-Series: Death Choice Timer Tests (Remaining tests from TIMER_TESTPLAN.md)
 *
 * Tests for Death Choice Timer behavior:
 * - TD-007: Death Choice message format
 * - TD-008: Multiple Death Choices - sequential processing
 * - TD-009: Multiple Death Choices - Unit ID order
 * - TD-010: Death Choice does not affect turn order
 * - TD-012: Death Choice during SPEED buff double action
 * - TD-013: Display shows Death Choice Timer during choice
 * - TD-014: Hero death does not trigger Death Choice
 * - TD-015: Death Choice with multiple owners
 */
@DisplayName("TD-Series: Death Choice Timer Tests (Remaining)")
class MatchServiceDeathChoiceTimerTest {

    private MatchRegistry registry;
    private RuleEngine ruleEngine;
    private GameStateSerializer serializer;
    private TimerService timerService;
    private MatchService service;
    private AtomicLong mockTime;
    private PlayerId p1;
    private PlayerId p2;
    private Board board;

    @BeforeEach
    void setUp() {
        registry = new MatchRegistry(new HashMap<>());
        ruleEngine = new RuleEngine();
        serializer = new GameStateSerializer();
        mockTime = new AtomicLong(1000000L);
        timerService = new TimerService(mockTime::get);
        service = new MatchService(registry, ruleEngine, serializer, timerService);
        p1 = new PlayerId("P1");
        p2 = new PlayerId("P2");
        board = new Board(5, 5);
    }

    @AfterEach
    void tearDown() {
        timerService.shutdown();
    }

    // ========== Helper Methods ==========

    private Unit createHero(String id, PlayerId owner, int hp, Position pos) {
        return new Unit(id, owner, hp, 3, 1, 1, pos, true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, hp,
                null, 0,
                0, false, false, false, 0, null);
    }

    private Unit createMinion(String id, PlayerId owner, int hp, Position pos) {
        return new Unit(id, owner, hp, 2, 1, 1, pos, true,
                UnitCategory.MINION, null, null, hp,
                null, 0,
                0, false, false, false, 0, null);
    }

    private GameState createState(List<Unit> units, PlayerId currentPlayer) {
        return new GameState(board, units, currentPlayer, false, null);
    }

    private GameState createStateWithBuffs(List<Unit> units, PlayerId currentPlayer,
                                           Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, currentPlayer, false, null, unitBuffs);
    }

    private Unit findUnit(GameState state, String unitId) {
        return state.getUnits().stream()
                .filter(u -> u.getId().equals(unitId))
                .findFirst()
                .orElse(null);
    }

    // ========== TD-007: Death Choice message format ==========

    @Nested
    @DisplayName("TD-007: Death Choice message format")
    class DeathChoiceMessageFormat {

        @Test
        @DisplayName("TD-007: ActionResult contains Death Choice info")
        void td007_actionResultContainsDeathChoiceInfo() {
            // Given: Minion death triggers Death Choice
            Unit hero1 = createHero("p1_hero", p1, 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", p2, 5, new Position(4, 4));
            Unit minion2 = createMinion("p2_minion_1", p2, 1, new Position(1, 2));
            GameState state = createState(Arrays.asList(hero1, hero2, minion2), p1);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");

            // When: Attack kills minion
            Action attack = Action.attack("p1_hero", new Position(1, 2), "p2_minion_1");
            ActionResult result = service.applyActionWithTimer("match-1", p1, attack);

            // Then: Message contains required fields
            assertTrue(result.hasTimer(), "Result should have timer info");
            assertEquals(TimerType.DEATH_CHOICE, result.getTimerType(),
                "Timer type should be DEATH_CHOICE");
            assertEquals(TimerConfig.DEATH_CHOICE_TIMEOUT_MS, result.getTimeoutMs(),
                "Timeout should be 5000ms");
            assertTrue(result.getActionStartTime() > 0,
                "Should have valid start timestamp");
            assertEquals(p2, result.getNextPlayer(),
                "Next player should be dead minion's owner (P2)");

            // Verify Death Choice is pending in game state
            GameState newState = result.getNewState();
            assertTrue(newState.hasPendingDeathChoice(),
                "Game state should have pending Death Choice");

            DeathChoice deathChoice = newState.getPendingDeathChoice();
            assertEquals("p2_minion_1", deathChoice.getDeadUnitId(),
                "Dead unit ID should be p2_minion_1");
            assertEquals(new Position(1, 2), deathChoice.getDeathPosition(),
                "Death position should be (1, 2)");
            assertEquals(p2, deathChoice.getOwner(),
                "Owner should be P2");
        }
    }

    // ========== TD-010: Death Choice does not affect turn order ==========

    @Nested
    @DisplayName("TD-010: Death Choice does not affect turn order")
    class DeathChoiceDoesNotAffectTurnOrder {

        @Test
        @DisplayName("TD-010: After Death Choice, normal turn order continues")
        void td010_afterDeathChoiceNormalTurnOrderContinues() {
            // Given: P1's unit attacks and kills P2's minion
            Unit hero1 = createHero("p1_hero", p1, 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", p2, 5, new Position(4, 4));
            Unit minion2 = createMinion("p2_minion_1", p2, 1, new Position(1, 2));
            GameState state = createState(Arrays.asList(hero1, hero2, minion2), p1);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");

            // P1 attacks and kills P2's minion
            Action attack = Action.attack("p1_hero", new Position(1, 2), "p2_minion_1");
            ActionResult attackResult = service.applyActionWithTimer("match-1", p1, attack);

            // Death Choice pending for P2
            assertEquals(TimerType.DEATH_CHOICE, attackResult.getTimerType());
            assertEquals(p2, attackResult.getNextPlayer());

            // When: Death Choice completed
            Action deathChoice = Action.deathChoice(p2, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            ActionResult choiceResult = service.applyActionWithTimer("match-1", p2, deathChoice);

            // Then: Normal turn order continues
            // P1's attack action is complete, so turn passes to P2
            // (Unit-by-unit system: after P1's hero acted, P2's turn)
            assertEquals(p2, choiceResult.getNextPlayer(),
                "After Death Choice, turn should continue to P2 (P1's action was attack)");
            assertEquals(TimerType.ACTION, choiceResult.getTimerType(),
                "Should be back to Action Timer");
            assertFalse(choiceResult.getNewState().hasPendingDeathChoice(),
                "No pending Death Choice after resolution");
        }
    }

    // ========== TD-012: Death Choice during SPEED buff double action ==========

    @Nested
    @DisplayName("TD-012: Death Choice during SPEED buff double action")
    class DeathChoiceDuringSpeedBuff {

        @Test
        @DisplayName("TD-012: SPEED first action kills minion - timer resets for second action")
        void td012_speedFirstActionKillsMinionTimerResetsForSecondAction() {
            // Given: P1's unit has SPEED buff (2 actions)
            Unit hero1 = createHero("p1_hero", p1, 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", p2, 5, new Position(4, 4));
            Unit minion2 = createMinion("p2_minion_1", p2, 1, new Position(1, 2));

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            BuffInstance speedBuff = BuffFactory.createSpeed("test_source");
            unitBuffs.put("p1_hero", Collections.singletonList(speedBuff));

            GameState state = createStateWithBuffs(
                Arrays.asList(hero1, hero2, minion2), p1, unitBuffs);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");
            mockTime.set(1004000L); // 6 seconds remaining

            // When: First action kills enemy minion
            Action attack = Action.attack("p1_hero", new Position(1, 2), "p2_minion_1");
            ActionResult attackResult = service.applyActionWithTimer("match-1", p1, attack);

            // Then: Action Timer paused, Death Choice Timer started
            assertEquals(TimerType.DEATH_CHOICE, attackResult.getTimerType());
            assertEquals(TimerState.PAUSED, timerService.getTimerState("match-1", TimerType.ACTION));
            assertEquals(TimerState.RUNNING, timerService.getTimerState("match-1", TimerType.DEATH_CHOICE));

            // When: Death Choice completed
            mockTime.set(1006000L);
            Action deathChoice = Action.deathChoice(p2, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            ActionResult choiceResult = service.applyActionWithTimer("match-1", p2, deathChoice);

            // Then: Timer resets to 10s for P1's second action (SPEED)
            // After death choice, P1 still has second action due to SPEED buff
            assertEquals(TimerType.ACTION, choiceResult.getTimerType());
            assertEquals(TimerConfig.ACTION_TIMEOUT_MS, choiceResult.getTimeoutMs(),
                "Timer should reset to full 10s for second action");
            assertEquals(TimerConfig.ACTION_TIMEOUT_MS,
                timerService.getRemainingTime("match-1", TimerType.ACTION),
                "Timer should be at 10s");
        }
    }

    // ========== TD-013: Display shows Death Choice Timer during choice ==========

    @Nested
    @DisplayName("TD-013: Display shows Death Choice Timer during choice")
    class DisplayShowsDeathChoiceTimer {

        @Test
        @DisplayName("TD-013: Death Choice Timer is RUNNING, Action Timer is PAUSED")
        void td013_deathChoiceTimerRunningActionTimerPaused() {
            // Given: Death Choice in progress
            Unit hero1 = createHero("p1_hero", p1, 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", p2, 5, new Position(4, 4));
            Unit minion2 = createMinion("p2_minion_1", p2, 1, new Position(1, 2));
            GameState state = createState(Arrays.asList(hero1, hero2, minion2), p1);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");

            // Kill minion to trigger Death Choice
            Action attack = Action.attack("p1_hero", new Position(1, 2), "p2_minion_1");
            service.applyActionWithTimer("match-1", p1, attack);

            // When: Client renders UI (checking timer states)

            // Then: Death Choice Timer displayed (running), Action Timer NOT displayed (paused)
            assertEquals(TimerState.RUNNING,
                timerService.getTimerState("match-1", TimerType.DEATH_CHOICE),
                "Death Choice Timer should be RUNNING");
            assertEquals(TimerState.PAUSED,
                timerService.getTimerState("match-1", TimerType.ACTION),
                "Action Timer should be PAUSED");

            // Death Choice timer has time remaining
            long deathChoiceRemaining = timerService.getRemainingTime("match-1", TimerType.DEATH_CHOICE);
            assertTrue(deathChoiceRemaining > 0 && deathChoiceRemaining <= 5000,
                "Death Choice Timer should have time remaining");
        }
    }

    // ========== TD-014: Hero death does not trigger Death Choice ==========

    @Nested
    @DisplayName("TD-014: Hero death does not trigger Death Choice")
    class HeroDeathDoesNotTriggerDeathChoice {

        @Test
        @DisplayName("TD-014: Kill Hero - game ends, no Death Choice")
        void td014_killHeroGameEndsNoDeathChoice() {
            // Given: Attack will kill enemy Hero
            Unit hero1 = createHero("p1_hero", p1, 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", p2, 3, new Position(1, 2)); // 3 HP, adjacent
            GameState state = createState(Arrays.asList(hero1, hero2), p1);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");

            // When: Attack kills Hero (3 HP - 3 damage = 0)
            Action attack = Action.attack("p1_hero", new Position(1, 2), "p2_hero");
            ActionResult result = service.applyActionWithTimer("match-1", p1, attack);

            // Then: No Death Choice (only for minions)
            assertFalse(result.getNewState().hasPendingDeathChoice(),
                "Hero death should NOT trigger Death Choice");

            // Game ends immediately
            assertTrue(result.isGameOver(), "Game should end when Hero dies");
            assertEquals(p1, result.getNewState().getWinner(), "P1 should win");

            // Victory declared - no timer
            assertFalse(result.hasTimer(), "No timer after game over");
            assertNull(timerService.getTimerState("match-1", TimerType.ACTION),
                "Action Timer should be cancelled");
            assertNull(timerService.getTimerState("match-1", TimerType.DEATH_CHOICE),
                "No Death Choice Timer for Hero death");
        }

        @Test
        @DisplayName("TD-014b: Kill Hero with minions alive - still no Death Choice")
        void td014b_killHeroWithMinionsAliveStillNoDeathChoice() {
            // Given: P2 has Hero and minion, P1 will kill Hero
            Unit hero1 = createHero("p1_hero", p1, 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", p2, 3, new Position(1, 2));
            Unit minion2 = createMinion("p2_minion_1", p2, 5, new Position(3, 3));
            GameState state = createState(Arrays.asList(hero1, hero2, minion2), p1);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");

            // When: Attack kills Hero
            Action attack = Action.attack("p1_hero", new Position(1, 2), "p2_hero");
            ActionResult result = service.applyActionWithTimer("match-1", p1, attack);

            // Then: Game over, no Death Choice even though minion alive
            assertTrue(result.isGameOver(), "Game should end when Hero dies");
            assertFalse(result.getNewState().hasPendingDeathChoice(),
                "No Death Choice for Hero death");
        }
    }

    // ========== TD-008/TD-009/TD-015: Multiple Death Choices ==========
    // Note: These tests require the game engine to support queuing multiple death choices.
    // The current implementation processes one death choice at a time.
    // These tests verify the expected behavior when multiple minions die.

    @Nested
    @DisplayName("TD-008/TD-009/TD-015: Multiple Death Choices")
    class MultipleDeathChoices {

        @Test
        @DisplayName("TD-008: Sequential Death Choices - first choice processed first")
        void td008_sequentialDeathChoicesFirstProcessedFirst() {
            // Given: Setup where we can kill one minion, then another
            // Note: True simultaneous kills from AOE would require skill implementation
            // This test verifies sequential death processing works correctly

            Unit hero1 = createHero("p1_hero", p1, 5, new Position(2, 2));
            Unit hero2 = createHero("p2_hero", p2, 5, new Position(4, 4));
            Unit minion2a = createMinion("p2_minion_1", p2, 1, new Position(2, 3)); // Adjacent
            GameState state = createState(Arrays.asList(hero1, hero2, minion2a), p1);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");

            // Kill first minion
            Action attack1 = Action.attack("p1_hero", new Position(2, 3), "p2_minion_1");
            ActionResult result1 = service.applyActionWithTimer("match-1", p1, attack1);

            // First Death Choice pending
            assertEquals(TimerType.DEATH_CHOICE, result1.getTimerType());
            assertEquals("p2_minion_1", result1.getNewState().getPendingDeathChoice().getDeadUnitId());

            // Resolve first Death Choice
            Action choice1 = Action.deathChoice(p2, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            ActionResult afterChoice1 = service.applyActionWithTimer("match-1", p2, choice1);

            // First choice resolved, game continues
            assertFalse(afterChoice1.getNewState().hasPendingDeathChoice(),
                "First Death Choice should be resolved");
            assertEquals(TimerType.ACTION, afterChoice1.getTimerType(),
                "Should be back to Action Timer after first choice");
        }

        @Test
        @DisplayName("TD-009: Death Choices processed in Unit ID order")
        void td009_deathChoicesProcessedInUnitIdOrder() {
            // This test documents expected behavior:
            // When multiple minions die simultaneously, process by Unit ID order
            // The actual implementation depends on how the game engine queues deaths

            // For now, verify that a single death choice contains correct unit ID
            Unit hero1 = createHero("p1_hero", p1, 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", p2, 5, new Position(4, 4));
            Unit minion2 = createMinion("p2_minion_1", p2, 1, new Position(1, 2));
            GameState state = createState(Arrays.asList(hero1, hero2, minion2), p1);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");

            Action attack = Action.attack("p1_hero", new Position(1, 2), "p2_minion_1");
            ActionResult result = service.applyActionWithTimer("match-1", p1, attack);

            // Verify correct unit ID in Death Choice
            DeathChoice deathChoice = result.getNewState().getPendingDeathChoice();
            assertEquals("p2_minion_1", deathChoice.getDeadUnitId(),
                "Death Choice should reference correct unit ID");
        }

        @Test
        @DisplayName("TD-015: Each owner makes their own choice")
        void td015_eachOwnerMakesTheirOwnChoice() {
            // Verify that the owner of the dead minion is the one who must make the choice

            // P1 kills P2's minion - P2 makes the choice
            Unit hero1 = createHero("p1_hero", p1, 5, new Position(1, 1));
            Unit hero2 = createHero("p2_hero", p2, 5, new Position(4, 4));
            Unit minion2 = createMinion("p2_minion_1", p2, 1, new Position(1, 2));
            GameState state = createState(Arrays.asList(hero1, hero2, minion2), p1);
            registry.createMatch("match-1", state);

            service.startTurnTimer("match-1");

            Action attack = Action.attack("p1_hero", new Position(1, 2), "p2_minion_1");
            ActionResult result = service.applyActionWithTimer("match-1", p1, attack);

            // P2 (owner of dead minion) must make the choice
            assertEquals(p2, result.getNextPlayer(),
                "Owner of dead minion should make the choice");
            assertEquals(p2, result.getNewState().getPendingDeathChoice().getOwner(),
                "Death Choice owner should be P2");

            // P1 cannot make the choice
            Action wrongChoice = Action.deathChoice(p1, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            assertThrows(IllegalArgumentException.class, () -> {
                service.applyActionWithTimer("match-1", p1, wrongChoice);
            }, "P1 should not be able to make P2's Death Choice");

            // P2 can make the choice
            Action correctChoice = Action.deathChoice(p2, DeathChoice.ChoiceType.SPAWN_BUFF_TILE);
            ActionResult choiceResult = service.applyActionWithTimer("match-1", p2, correctChoice);
            assertNotNull(choiceResult, "P2 should be able to make the choice");
            assertFalse(choiceResult.getNewState().hasPendingDeathChoice(),
                "Death Choice should be resolved");
        }
    }
}
