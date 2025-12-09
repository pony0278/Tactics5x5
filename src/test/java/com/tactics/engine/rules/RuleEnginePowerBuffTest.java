package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BP-Series: POWER Buff Special Behavior Tests (from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for POWER buff:
 * - +3 ATK bonus
 * - +1 HP instant on acquisition
 * - Blocks MOVE_AND_ATTACK
 * - Allows MOVE
 * - Allows ATTACK
 * - Enables DESTROY_OBSTACLE (via attack on obstacle)
 * - Cannot destroy empty tile
 * - Cannot destroy unit
 */
@DisplayName("BP-Series: POWER Buff Special Behavior")
public class RuleEnginePowerBuffTest {

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

    private Unit createUnit(String id, PlayerId owner, Position pos, int hp, int attack) {
        return new Unit(id, owner, hp, attack, 1, 1, pos, true);
    }

    private GameState createStateWithBuffs(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, p1, false, null, unitBuffs,
            new ArrayList<>(), new ArrayList<>(), 1, null, false, false);
    }

    private GameState createStateWithObstacles(List<Unit> units, List<Obstacle> obstacles,
                                                Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, p1, false, null, unitBuffs,
            new ArrayList<>(), obstacles, 1, null, false, false);
    }

    private Unit findUnit(GameState state, String id) {
        return state.getUnits().stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    // ========== BP-Series Tests ==========

    @Nested
    @DisplayName("BP1: POWER buff grants +3 ATK")
    class PowerBuffAttackBonusTests {

        @Test
        @DisplayName("BP1: POWER buff grants +3 ATK damage")
        void powerBuffGrants3AtkDamage() {
            // Given: Unit with POWER buff attacks target
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);
            int targetOriginalHp = target.getHp();

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Attack
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Then: Target should take base (3) + 3 = 6 damage
            Unit updatedTarget = findUnit(afterAttack, "p2_unit");
            assertEquals(targetOriginalHp - 6, updatedTarget.getHp(),
                "POWER buff should grant +3 ATK, dealing 6 total damage (3 base + 3 bonus)");
        }
    }

    @Nested
    @DisplayName("BP2: POWER buff grants +1 HP on acquisition")
    class PowerBuffInstantHpTests {

        @Test
        @DisplayName("BP2: POWER buff grants +1 HP instantly")
        void powerBuffGrants1HpInstantly() {
            // Given: Unit moves to POWER tile
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            int hpBefore = unit.getHp();

            BuffTile powerTile = new BuffTile("tile1", new Position(2, 3), BuffType.POWER, 2, false);

            GameState state = new GameState(board, Arrays.asList(unit, p2Unit), p1, false, null,
                Collections.emptyMap(), Collections.singletonList(powerTile),
                Collections.emptyList(), 1, null, false, false);

            // When: Move to buff tile
            Action move = Action.move("p1_unit", new Position(2, 3));
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have +1 HP
            Unit updatedUnit = findUnit(afterMove, "p1_unit");
            assertEquals(hpBefore + 1, updatedUnit.getHp(),
                "POWER buff should grant +1 HP instantly on acquisition");
        }
    }

    @Nested
    @DisplayName("BP3: POWER buff blocks MOVE_AND_ATTACK")
    class PowerBuffBlocksMoveAndAttackTests {

        @Test
        @DisplayName("BP3: POWER buff blocks MOVE_AND_ATTACK")
        void powerBuffBlocksMoveAndAttack() {
            // Given: Unit with POWER buff
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Try MOVE_AND_ATTACK
            Action moveAndAttack = new Action(ActionType.MOVE_AND_ATTACK, p1,
                new Position(2, 3), "p2_unit");
            ValidationResult result = ruleEngine.validateAction(state, moveAndAttack);

            // Then: Should be invalid
            assertFalse(result.isValid(),
                "MOVE_AND_ATTACK should be blocked for unit with POWER buff");
            assertTrue(result.getErrorMessage().toLowerCase().contains("power") ||
                result.getErrorMessage().toLowerCase().contains("move_and_attack"),
                "Error message should mention POWER buff blocking MOVE_AND_ATTACK");
        }
    }

    @Nested
    @DisplayName("BP4: POWER buff allows MOVE")
    class PowerBuffAllowsMoveTests {

        @Test
        @DisplayName("BP4: POWER buff allows MOVE action")
        void powerBuffAllowsMove() {
            // Given: Unit with POWER buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: Try MOVE
            Action move = Action.move("p1_unit", new Position(2, 3));
            ValidationResult result = ruleEngine.validateAction(state, move);

            // Then: Should be valid
            assertTrue(result.isValid(),
                "MOVE should be allowed for unit with POWER buff");
        }
    }

    @Nested
    @DisplayName("BP5: POWER buff allows ATTACK")
    class PowerBuffAllowsAttackTests {

        @Test
        @DisplayName("BP5: POWER buff allows ATTACK action")
        void powerBuffAllowsAttack() {
            // Given: Unit with POWER buff adjacent to enemy
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2));
            Unit target = createUnit("p2_unit", p2, new Position(2, 3));

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Try ATTACK
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            ValidationResult result = ruleEngine.validateAction(state, attack);

            // Then: Should be valid
            assertTrue(result.isValid(),
                "ATTACK should be allowed for unit with POWER buff");
        }
    }

    @Nested
    @DisplayName("BP6: POWER buff enables obstacle destruction")
    class PowerBuffEnablesObstacleDestructionTests {

        @Test
        @DisplayName("BP6: POWER buff enables 1-hit obstacle destruction via ATTACK")
        void powerBuffEnables1HitObstacleDestruction() {
            // Given: Unit with POWER buff adjacent to obstacle
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            // Obstacle with 3 HP at adjacent position
            Obstacle obstacle = new Obstacle("obs1", new Position(2, 3), 3);

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithObstacles(Arrays.asList(unit, p2Unit),
                Collections.singletonList(obstacle), unitBuffs);

            // Verify obstacle exists
            assertTrue(state.hasObstacleAt(new Position(2, 3)), "Obstacle should exist before attack");

            // When: Attack the obstacle position (POWER buff enables instant destruction)
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), null);
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Then: Obstacle should be destroyed (POWER buff enables 1-hit destruction)
            assertFalse(afterAttack.hasObstacleAt(new Position(2, 3)),
                "POWER buff should enable 1-hit obstacle destruction");
        }
    }

    @Nested
    @DisplayName("BP7: POWER buff cannot destroy empty tile")
    class PowerBuffCannotDestroyEmptyTileTests {

        @Test
        @DisplayName("BP7: Cannot attack empty tile even with POWER buff")
        void cannotAttackEmptyTile() {
            // Given: Unit with POWER buff, empty adjacent tile
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            // No obstacle at target position
            GameState state = createStateWithObstacles(Arrays.asList(unit, p2Unit),
                Collections.emptyList(), unitBuffs);

            // When: Try to attack empty tile
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), null);
            ValidationResult result = ruleEngine.validateAction(state, attack);

            // Then: Should be invalid (no target at position)
            assertFalse(result.isValid(),
                "Cannot attack empty tile even with POWER buff");
        }
    }

    @Nested
    @DisplayName("BP8: POWER buff cannot destroy unit (normal attack instead)")
    class PowerBuffCannotDestroyUnitTests {

        @Test
        @DisplayName("BP8: Attacking unit with POWER buff deals damage, not instant destruction")
        void attackingUnitDealsDamageNotInstantDestruction() {
            // Given: Unit with POWER buff attacks enemy unit
            Unit attacker = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit target = createUnit("p2_unit", p2, new Position(2, 3), 10, 3);
            int targetOriginalHp = target.getHp();

            BuffInstance powerBuff = BuffFactory.createPower("source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(attacker, target), unitBuffs);

            // When: Attack enemy unit
            Action attack = new Action(ActionType.ATTACK, p1, new Position(2, 3), "p2_unit");
            GameState afterAttack = ruleEngine.applyAction(state, attack);

            // Then: Target should take damage (not be instantly destroyed)
            Unit updatedTarget = findUnit(afterAttack, "p2_unit");
            assertTrue(updatedTarget.isAlive(),
                "Target should still be alive (not instantly destroyed)");
            assertTrue(updatedTarget.getHp() < targetOriginalHp,
                "Target should have taken damage");
            assertEquals(targetOriginalHp - 6, updatedTarget.getHp(),
                "Target should take normal damage (+3 from POWER) = 6 total");
        }
    }
}
