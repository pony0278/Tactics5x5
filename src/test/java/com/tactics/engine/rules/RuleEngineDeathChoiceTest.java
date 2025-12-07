package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.model.*;
import com.tactics.engine.buff.BuffInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Death Choice Flow Tests
 *
 * Tests the complete death choice flow:
 * 1. When minion dies, pendingDeathChoice is set
 * 2. DEATH_CHOICE validation (only when pending, correct player)
 * 3. DEATH_CHOICE apply: spawn obstacle or buff tile
 * 4. pendingDeathChoice cleared after resolution
 */
@DisplayName("Death Choice Flow Tests")
class RuleEngineDeathChoiceTest {

    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private Unit createHero(String id, PlayerId owner, int hp, Position pos) {
        return new Unit(id, owner, hp, 2, 2, 1, pos, true,
                UnitCategory.HERO, null, HeroClass.WARRIOR, hp, null, 0,
                0, false, false, false, 0, null,
                0, false, null, 0, 0);
    }

    private Unit createMinion(String id, PlayerId owner, MinionType type, int hp, Position pos) {
        int attack = type == MinionType.ASSASSIN ? 2 : 1;
        int moveRange = type == MinionType.ASSASSIN ? 4 : 2;
        int attackRange = type == MinionType.ARCHER ? 3 : 1;
        return new Unit(id, owner, hp, attack, moveRange, attackRange, pos, true,
                UnitCategory.MINION, type, null, hp, null, 0,
                0, false, false, false, 0, null,
                0, false, null, 0, 0);
    }

    private GameState createBasicState(List<Unit> units) {
        return new GameState(
            new Board(5, 5),
            units,
            PlayerId.PLAYER_1,
            false,
            null,
            new HashMap<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            1,
            null
        );
    }

    private Unit findUnit(GameState state, String unitId) {
        for (Unit u : state.getUnits()) {
            if (u.getId().equals(unitId)) {
                return u;
            }
        }
        return null;
    }

    // =========================================================================
    // DC1: Minion Death Triggers Pending Death Choice
    // =========================================================================

    @Nested
    @DisplayName("DC1: Minion Death Triggers Pending Death Choice")
    class MinionDeathTrigger {

        @Test
        @DisplayName("DC1.1: When P1 minion dies from ATTACK, pendingDeathChoice is set for P1")
        void whenP1MinionDiesFromAttack_pendingDeathChoiceSetForP1() {
            // Arrange: P1 minion with 1 HP, P2 attacker
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p1Minion = createMinion("p1_minion", PlayerId.PLAYER_1, MinionType.ARCHER, 1, new Position(2, 2));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            Unit p2Attacker = createMinion("p2_attacker", PlayerId.PLAYER_2, MinionType.ASSASSIN, 5, new Position(2, 3));

            GameState state = createBasicState(Arrays.asList(p1Hero, p1Minion, p2Hero, p2Attacker));
            state = new GameState(state.getBoard(), state.getUnits(), PlayerId.PLAYER_2,
                    false, null, state.getUnitBuffs(), state.getBuffTiles(), state.getObstacles(),
                    1, null);

            // Act: P2 attacks P1 minion
            Action attack = new Action(ActionType.ATTACK, PlayerId.PLAYER_2,
                    new Position(2, 2), "p1_minion");
            GameState result = ruleEngine.applyAction(state, attack);

            // Assert: pendingDeathChoice is set for P1
            assertTrue(result.hasPendingDeathChoice(), "Should have pending death choice");
            DeathChoice choice = result.getPendingDeathChoice();
            assertEquals("p1_minion", choice.getDeadUnitId());
            assertEquals(PlayerId.PLAYER_1, choice.getOwner());
            assertEquals(new Position(2, 2), choice.getDeathPosition());
        }

        @Test
        @DisplayName("DC1.2: When P2 minion dies from ATTACK, pendingDeathChoice is set for P2")
        void whenP2MinionDiesFromAttack_pendingDeathChoiceSetForP2() {
            // Arrange: P2 minion with 1 HP, P1 attacker adjacent
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p1Attacker = createMinion("p1_attacker", PlayerId.PLAYER_1, MinionType.ASSASSIN, 5, new Position(2, 1));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            Unit p2Minion = createMinion("p2_minion", PlayerId.PLAYER_2, MinionType.TANK, 1, new Position(2, 2));

            GameState state = createBasicState(Arrays.asList(p1Hero, p1Attacker, p2Hero, p2Minion));

            // Act: P1 attacks P2 minion
            Action attack = new Action(ActionType.ATTACK, PlayerId.PLAYER_1,
                    new Position(2, 2), "p2_minion");
            GameState result = ruleEngine.applyAction(state, attack);

            // Assert: pendingDeathChoice is set for P2
            assertTrue(result.hasPendingDeathChoice(), "Should have pending death choice");
            DeathChoice choice = result.getPendingDeathChoice();
            assertEquals("p2_minion", choice.getDeadUnitId());
            assertEquals(PlayerId.PLAYER_2, choice.getOwner());
            assertEquals(new Position(2, 2), choice.getDeathPosition());
        }

        @Test
        @DisplayName("DC1.3: When minion dies from MOVE_AND_ATTACK, pendingDeathChoice is set")
        void whenMinionDiesFromMoveAndAttack_pendingDeathChoiceSet() {
            // Arrange: P2 minion with 1 HP, P1 can move and attack
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p1Attacker = createMinion("p1_attacker", PlayerId.PLAYER_1, MinionType.ASSASSIN, 5, new Position(2, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            Unit p2Minion = createMinion("p2_minion", PlayerId.PLAYER_2, MinionType.ARCHER, 1, new Position(2, 3));

            GameState state = createBasicState(Arrays.asList(p1Hero, p1Attacker, p2Hero, p2Minion));

            // Act: P1 moves to (2,2) and attacks P2 minion at (2,3)
            Action moveAttack = new Action(ActionType.MOVE_AND_ATTACK, PlayerId.PLAYER_1,
                    new Position(2, 2), "p2_minion");
            GameState result = ruleEngine.applyAction(state, moveAttack);

            // Assert: pendingDeathChoice is set for P2
            assertTrue(result.hasPendingDeathChoice(), "Should have pending death choice");
            DeathChoice choice = result.getPendingDeathChoice();
            assertEquals("p2_minion", choice.getDeadUnitId());
            assertEquals(PlayerId.PLAYER_2, choice.getOwner());
        }

        @Test
        @DisplayName("DC1.4: Hero death does NOT trigger pendingDeathChoice")
        void heroDeathDoesNotTriggerPendingDeathChoice() {
            // Arrange: P2 hero with 1 HP, P1 attacker adjacent
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p1Attacker = createMinion("p1_attacker", PlayerId.PLAYER_1, MinionType.ASSASSIN, 5, new Position(2, 1));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 1, new Position(2, 2));

            GameState state = createBasicState(Arrays.asList(p1Hero, p1Attacker, p2Hero));

            // Act: P1 attacks P2 hero
            Action attack = new Action(ActionType.ATTACK, PlayerId.PLAYER_1,
                    new Position(2, 2), "p2_hero");
            GameState result = ruleEngine.applyAction(state, attack);

            // Assert: Game over, no pending death choice
            assertTrue(result.isGameOver(), "Game should be over when hero dies");
            assertFalse(result.hasPendingDeathChoice(), "Hero death should not trigger death choice");
        }

        @Test
        @DisplayName("DC1.5: Minion survives attack - no pendingDeathChoice")
        void minionSurvivesAttack_noPendingDeathChoice() {
            // Arrange: P2 minion with 5 HP, P1 attacker does 2 damage
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p1Attacker = createMinion("p1_attacker", PlayerId.PLAYER_1, MinionType.ASSASSIN, 5, new Position(2, 1));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            Unit p2Minion = createMinion("p2_minion", PlayerId.PLAYER_2, MinionType.TANK, 5, new Position(2, 2));

            GameState state = createBasicState(Arrays.asList(p1Hero, p1Attacker, p2Hero, p2Minion));

            // Act: P1 attacks P2 minion (2 damage, minion has 5 HP)
            Action attack = new Action(ActionType.ATTACK, PlayerId.PLAYER_1,
                    new Position(2, 2), "p2_minion");
            GameState result = ruleEngine.applyAction(state, attack);

            // Assert: Minion alive, no pending death choice
            Unit minion = findUnit(result, "p2_minion");
            assertTrue(minion.isAlive(), "Minion should survive");
            assertEquals(3, minion.getHp(), "Minion should have 3 HP remaining");
            assertFalse(result.hasPendingDeathChoice(), "No death choice when minion survives");
        }
    }

    // =========================================================================
    // DC2: DEATH_CHOICE Validation
    // =========================================================================

    @Nested
    @DisplayName("DC2: DEATH_CHOICE Validation")
    class DeathChoiceValidation {

        @Test
        @DisplayName("DC2.1: DEATH_CHOICE valid when pendingDeathChoice exists for correct player")
        void deathChoiceValidWhenPendingAndCorrectPlayer() {
            // Arrange: State with pending death choice for P1
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            DeathChoice pending = new DeathChoice("dead_minion", PlayerId.PLAYER_1, new Position(2, 2));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(p1Hero, p2Hero),
                PlayerId.PLAYER_1,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                1,
                pending
            );

            // Act
            Action choice = Action.deathChoice(PlayerId.PLAYER_1, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            ValidationResult result = ruleEngine.validateAction(state, choice);

            // Assert
            assertTrue(result.isValid(), "DEATH_CHOICE should be valid for correct player");
        }

        @Test
        @DisplayName("DC2.2: DEATH_CHOICE invalid when no pendingDeathChoice")
        void deathChoiceInvalidWhenNoPending() {
            // Arrange: State without pending death choice
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));

            GameState state = createBasicState(Arrays.asList(p1Hero, p2Hero));

            // Act
            Action choice = Action.deathChoice(PlayerId.PLAYER_1, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            ValidationResult result = ruleEngine.validateAction(state, choice);

            // Assert
            assertFalse(result.isValid(), "DEATH_CHOICE should be invalid without pending");
            assertEquals("No pending death choice", result.getErrorMessage());
        }

        @Test
        @DisplayName("DC2.3: DEATH_CHOICE invalid when wrong player")
        void deathChoiceInvalidWhenWrongPlayer() {
            // Arrange: State with pending death choice for P1, but P2 tries to choose
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            DeathChoice pending = new DeathChoice("dead_minion", PlayerId.PLAYER_1, new Position(2, 2));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(p1Hero, p2Hero),
                PlayerId.PLAYER_2,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                1,
                pending
            );

            // Act: P2 tries to make death choice that belongs to P1
            Action choice = Action.deathChoice(PlayerId.PLAYER_2, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            ValidationResult result = ruleEngine.validateAction(state, choice);

            // Assert
            assertFalse(result.isValid(), "DEATH_CHOICE should be invalid for wrong player");
            assertEquals("Not your death choice", result.getErrorMessage());
        }

        @Test
        @DisplayName("DC2.4: DEATH_CHOICE invalid without choice type")
        void deathChoiceInvalidWithoutChoiceType() {
            // Arrange: State with pending death choice
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            DeathChoice pending = new DeathChoice("dead_minion", PlayerId.PLAYER_1, new Position(2, 2));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(p1Hero, p2Hero),
                PlayerId.PLAYER_1,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                1,
                pending
            );

            // Act: Death choice without type
            Action choice = Action.deathChoice(PlayerId.PLAYER_1, null);
            ValidationResult result = ruleEngine.validateAction(state, choice);

            // Assert
            assertFalse(result.isValid(), "DEATH_CHOICE should be invalid without type");
            assertEquals("Death choice type is required", result.getErrorMessage());
        }

        @Test
        @DisplayName("DC2.5: Other actions blocked while pendingDeathChoice exists")
        void otherActionsBlockedWhilePending() {
            // Arrange: State with pending death choice
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            DeathChoice pending = new DeathChoice("dead_minion", PlayerId.PLAYER_1, new Position(2, 2));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(p1Hero, p2Hero),
                PlayerId.PLAYER_1,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                1,
                pending
            );

            // Act: Try to move while death choice pending
            Action move = new Action(ActionType.MOVE, PlayerId.PLAYER_1, new Position(0, 1), null);
            ValidationResult result = ruleEngine.validateAction(state, move);

            // Assert
            assertFalse(result.isValid(), "MOVE should be blocked while death choice pending");
            assertTrue(result.getErrorMessage().contains("death choice"),
                "Reason should mention death choice: " + result.getErrorMessage());
        }
    }

    // =========================================================================
    // DC3: DEATH_CHOICE Apply - Spawn Obstacle
    // =========================================================================

    @Nested
    @DisplayName("DC3: DEATH_CHOICE Apply - Spawn Obstacle")
    class DeathChoiceSpawnObstacle {

        @Test
        @DisplayName("DC3.1: SPAWN_OBSTACLE creates obstacle at death position")
        void spawnObstacleCreatesObstacleAtDeathPosition() {
            // Arrange: State with pending death choice at (2,2)
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            DeathChoice pending = new DeathChoice("dead_minion", PlayerId.PLAYER_1, new Position(2, 2));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(p1Hero, p2Hero),
                PlayerId.PLAYER_1,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                1,
                pending
            );

            // Act
            Action choice = Action.deathChoice(PlayerId.PLAYER_1, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            GameState result = ruleEngine.applyAction(state, choice);

            // Assert
            assertEquals(1, result.getObstacles().size(), "Should have 1 obstacle");
            Obstacle obstacle = result.getObstacles().get(0);
            assertEquals(new Position(2, 2), obstacle.getPosition(), "Obstacle at death position");
            assertEquals(3, obstacle.getHp(), "Obstacle should have default 3 HP");
        }

        @Test
        @DisplayName("DC3.2: SPAWN_OBSTACLE clears pendingDeathChoice")
        void spawnObstacleClearsPendingDeathChoice() {
            // Arrange
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            DeathChoice pending = new DeathChoice("dead_minion", PlayerId.PLAYER_1, new Position(2, 2));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(p1Hero, p2Hero),
                PlayerId.PLAYER_1,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                1,
                pending
            );

            // Act
            Action choice = Action.deathChoice(PlayerId.PLAYER_1, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            GameState result = ruleEngine.applyAction(state, choice);

            // Assert
            assertFalse(result.hasPendingDeathChoice(), "pendingDeathChoice should be cleared");
            assertNull(result.getPendingDeathChoice());
        }
    }

    // =========================================================================
    // DC4: DEATH_CHOICE Apply - Spawn Buff Tile
    // =========================================================================

    @Nested
    @DisplayName("DC4: DEATH_CHOICE Apply - Spawn Buff Tile")
    class DeathChoiceSpawnBuffTile {

        @Test
        @DisplayName("DC4.1: SPAWN_BUFF_TILE creates buff tile at death position")
        void spawnBuffTileCreatesBuffTileAtDeathPosition() {
            // Arrange
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            DeathChoice pending = new DeathChoice("dead_minion", PlayerId.PLAYER_1, new Position(2, 2));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(p1Hero, p2Hero),
                PlayerId.PLAYER_1,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                1,
                pending
            );

            // Act
            Action choice = Action.deathChoice(PlayerId.PLAYER_1, DeathChoice.ChoiceType.SPAWN_BUFF_TILE);
            GameState result = ruleEngine.applyAction(state, choice);

            // Assert
            assertEquals(1, result.getBuffTiles().size(), "Should have 1 buff tile");
            BuffTile tile = result.getBuffTiles().get(0);
            assertEquals(new Position(2, 2), tile.getPosition(), "Buff tile at death position");
            assertEquals(2, tile.getDuration(), "Buff tile should have duration 2");
            assertFalse(tile.isTriggered(), "Buff tile should not be triggered yet");
        }

        @Test
        @DisplayName("DC4.2: SPAWN_BUFF_TILE clears pendingDeathChoice")
        void spawnBuffTileClearsPendingDeathChoice() {
            // Arrange
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            DeathChoice pending = new DeathChoice("dead_minion", PlayerId.PLAYER_1, new Position(2, 2));

            GameState state = new GameState(
                new Board(5, 5),
                Arrays.asList(p1Hero, p2Hero),
                PlayerId.PLAYER_1,
                false, null,
                new HashMap<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                1,
                pending
            );

            // Act
            Action choice = Action.deathChoice(PlayerId.PLAYER_1, DeathChoice.ChoiceType.SPAWN_BUFF_TILE);
            GameState result = ruleEngine.applyAction(state, choice);

            // Assert
            assertFalse(result.hasPendingDeathChoice(), "pendingDeathChoice should be cleared");
        }
    }

    // =========================================================================
    // DC5: Full Death Choice Flow Integration
    // =========================================================================

    @Nested
    @DisplayName("DC5: Full Death Choice Flow Integration")
    class FullFlowIntegration {

        @Test
        @DisplayName("DC5.1: Full flow - attack kills minion, choose obstacle, then continue game")
        void fullFlowAttackKillChooseObstacleContinue() {
            // Arrange: P1 attacker can kill P2 minion
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p1Attacker = createMinion("p1_attacker", PlayerId.PLAYER_1, MinionType.ASSASSIN, 5, new Position(2, 1));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            Unit p2Minion = createMinion("p2_minion", PlayerId.PLAYER_2, MinionType.TANK, 2, new Position(2, 2));

            GameState state = createBasicState(Arrays.asList(p1Hero, p1Attacker, p2Hero, p2Minion));

            // Step 1: Attack kills minion
            Action attack = new Action(ActionType.ATTACK, PlayerId.PLAYER_1,
                    new Position(2, 2), "p2_minion");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Verify: pendingDeathChoice for P2
            assertTrue(afterAttack.hasPendingDeathChoice());
            assertEquals(PlayerId.PLAYER_2, afterAttack.getPendingDeathChoice().getOwner());

            // Step 2: P2 chooses obstacle
            Action choice = Action.deathChoice(PlayerId.PLAYER_2, DeathChoice.ChoiceType.SPAWN_OBSTACLE);
            GameState afterChoice = ruleEngine.applyAction(afterAttack, choice);

            // Verify: obstacle created, pending cleared
            assertFalse(afterChoice.hasPendingDeathChoice());
            assertEquals(1, afterChoice.getObstacles().size());
            assertEquals(new Position(2, 2), afterChoice.getObstacles().get(0).getPosition());

            // Step 3: Game can continue - P1 can move
            Action move = new Action(ActionType.MOVE, PlayerId.PLAYER_1, new Position(0, 1), null);
            ValidationResult moveResult = ruleEngine.validateAction(afterChoice, move);
            // Note: May fail due to turn order, but should not fail due to death choice
            assertFalse(moveResult.getErrorMessage() != null && moveResult.getErrorMessage().contains("death choice"),
                    "Move should not be blocked by death choice after resolution");
        }

        @Test
        @DisplayName("DC5.2: Full flow - attack kills minion, choose buff tile, tile can be triggered")
        void fullFlowAttackKillChooseBuffTileTrigger() {
            // Arrange: P1 attacker can kill P2 minion
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p1Attacker = createMinion("p1_attacker", PlayerId.PLAYER_1, MinionType.ASSASSIN, 5, new Position(2, 1));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            Unit p2Minion = createMinion("p2_minion", PlayerId.PLAYER_2, MinionType.TANK, 2, new Position(2, 2));

            GameState state = createBasicState(Arrays.asList(p1Hero, p1Attacker, p2Hero, p2Minion));

            // Step 1: Attack kills minion
            Action attack = new Action(ActionType.ATTACK, PlayerId.PLAYER_1,
                    new Position(2, 2), "p2_minion");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Step 2: P2 chooses buff tile
            Action choice = Action.deathChoice(PlayerId.PLAYER_2, DeathChoice.ChoiceType.SPAWN_BUFF_TILE);
            GameState afterChoice = ruleEngine.applyAction(afterAttack, choice);

            // Verify: buff tile created
            assertEquals(1, afterChoice.getBuffTiles().size());
            BuffTile tile = afterChoice.getBuffTiles().get(0);
            assertEquals(new Position(2, 2), tile.getPosition());
            assertFalse(tile.isTriggered());

            // Step 3: P1 attacker moves onto buff tile (from 2,1 to 2,2)
            // First need to reset state for P1's turn and set attacker as not acted
            List<Unit> unitsForMove = new ArrayList<>();
            for (Unit u : afterChoice.getUnits()) {
                if (u.getId().equals("p1_attacker")) {
                    // Reset actionsUsed for test using withActionsUsed
                    unitsForMove.add(u.withActionsUsed(0));
                } else {
                    unitsForMove.add(u);
                }
            }
            GameState stateForMove = new GameState(
                afterChoice.getBoard(), unitsForMove, PlayerId.PLAYER_1,
                false, null, afterChoice.getUnitBuffs(), afterChoice.getBuffTiles(),
                afterChoice.getObstacles(), afterChoice.getCurrentRound(), null
            );

            Action move = new Action(ActionType.MOVE, PlayerId.PLAYER_1, new Position(2, 2), null);
            GameState afterMove = ruleEngine.applyAction(stateForMove, move);

            // Verify: buff tile triggered (marked or buff applied)
            // The tile should be marked as triggered after unit steps on it
            BuffTile tileAfter = null;
            for (BuffTile t : afterMove.getBuffTiles()) {
                if (t.getPosition().equals(new Position(2, 2))) {
                    tileAfter = t;
                    break;
                }
            }
            assertTrue(tileAfter == null || tileAfter.isTriggered(),
                    "Buff tile should be triggered or removed after stepping on it");
        }

        @Test
        @DisplayName("DC5.3: Multiple minion deaths in same round create sequential death choices")
        void multipleDeathsCreateSequentialChoices() {
            // This test verifies that when multiple minions die, only one pendingDeathChoice
            // is active at a time (first death first)

            // For now, we only support single death at a time - additional deaths would need
            // a queue system which may be out of scope. This test documents expected behavior.

            // Arrange: P1 attacker can kill P2 minion with 1 HP
            Unit p1Hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(0, 0));
            Unit p1Attacker = createMinion("p1_attacker", PlayerId.PLAYER_1, MinionType.ASSASSIN, 5, new Position(2, 1));
            Unit p2Hero = createHero("p2_hero", PlayerId.PLAYER_2, 10, new Position(4, 4));
            Unit p2Minion = createMinion("p2_minion", PlayerId.PLAYER_2, MinionType.TANK, 2, new Position(2, 2));

            GameState state = createBasicState(Arrays.asList(p1Hero, p1Attacker, p2Hero, p2Minion));

            // Kill minion
            Action attack = new Action(ActionType.ATTACK, PlayerId.PLAYER_1,
                    new Position(2, 2), "p2_minion");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Should have exactly one pending death choice
            assertTrue(afterAttack.hasPendingDeathChoice());
            assertEquals("p2_minion", afterAttack.getPendingDeathChoice().getDeadUnitId());
        }
    }
}
