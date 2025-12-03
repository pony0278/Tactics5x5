package com.tactics.engine.rules;

import com.tactics.engine.action.Action;
import com.tactics.engine.action.ActionType;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.Board;
import com.tactics.engine.model.BuffTile;
import com.tactics.engine.model.GameState;
import com.tactics.engine.model.PlayerId;
import com.tactics.engine.model.Position;
import com.tactics.engine.model.Unit;
import com.tactics.engine.util.RngProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V3 Buff Tile Tests (BT-Series from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for buff tile triggering mechanics:
 * - Tile triggers when unit moves onto it
 * - Buff is applied to the triggering unit
 * - Tile is marked as triggered after use
 * - Random buff selection via RngProvider
 */
@DisplayName("V3 Buff Tile Tests")
class BuffTileTest {

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

    private BuffTile createBuffTile(String id, Position pos, BuffType type) {
        return new BuffTile(id, pos, type, 2, false);
    }

    private BuffTile createRandomBuffTile(String id, Position pos) {
        return new BuffTile(id, pos, null, 2, false);  // null type = random on trigger
    }

    private GameState createStateWithBuffTile(List<Unit> units, BuffTile buffTile) {
        List<BuffTile> buffTiles = Collections.singletonList(buffTile);
        return new GameState(board, units, p1, false, null, Collections.emptyMap(),
            buffTiles, Collections.emptyList(), 1, null, false, false);
    }

    private GameState createStateWithBuffTiles(List<Unit> units, List<BuffTile> buffTiles) {
        return new GameState(board, units, p1, false, null, Collections.emptyMap(),
            buffTiles, Collections.emptyList(), 1, null, false, false);
    }

    // ========== BT-Series: Buff Tile Trigger Tests ==========

    @Nested
    @DisplayName("BT-Series: Buff Tile Triggering")
    class BuffTileTrigger {

        @Test
        @DisplayName("BT1: Moving onto buff tile triggers it")
        void movingOntoBuffTileTriggers() {
            // Given: Unit at (2,2), POWER buff tile at (2,3)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile tile = createBuffTile("tile1", new Position(2, 3), BuffType.POWER);

            GameState state = createStateWithBuffTile(Arrays.asList(unit, p2Unit), tile);

            // When: Unit moves to (2,3)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Tile should be marked as triggered
            BuffTile updatedTile = afterMove.getBuffTiles().stream()
                .filter(t -> t.getId().equals("tile1"))
                .findFirst().orElse(null);

            assertNotNull(updatedTile, "Tile should still exist");
            assertTrue(updatedTile.isTriggered(), "Tile should be marked as triggered");
        }

        @Test
        @DisplayName("BT2: Unit receives buff when triggering tile")
        void unitReceivesBuffFromTile() {
            // Given: Unit at (2,2), POWER buff tile at (2,3)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile tile = createBuffTile("tile1", new Position(2, 3), BuffType.POWER);

            GameState state = createStateWithBuffTile(Arrays.asList(unit, p2Unit), tile);

            // When: Unit moves to (2,3)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have POWER buff
            List<BuffInstance> buffs = afterMove.getUnitBuffs().get("p1_unit");
            assertNotNull(buffs, "Unit should have buffs");
            assertEquals(1, buffs.size(), "Unit should have 1 buff");
            assertEquals(BuffType.POWER, buffs.get(0).getType(), "Buff should be POWER");
        }

        @Test
        @DisplayName("BT3: POWER buff tile applies +1 instant HP")
        void powerBuffTileAppliesInstantHp() {
            // Given: Unit with 10 HP at (2,2), POWER buff tile at (2,3)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile tile = createBuffTile("tile1", new Position(2, 3), BuffType.POWER);

            GameState state = createStateWithBuffTile(Arrays.asList(unit, p2Unit), tile);

            // When: Unit moves to (2,3)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have 11 HP (10 + 1 instant from POWER)
            Unit updatedUnit = afterMove.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();

            assertEquals(11, updatedUnit.getHp(), "Unit HP should be 11 (10 + 1 instant HP from POWER)");
        }

        @Test
        @DisplayName("BT4: LIFE buff tile applies +3 instant HP")
        void lifeBuffTileAppliesInstantHp() {
            // Given: Unit with 10 HP at (2,2), LIFE buff tile at (2,3)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile tile = createBuffTile("tile1", new Position(2, 3), BuffType.LIFE);

            GameState state = createStateWithBuffTile(Arrays.asList(unit, p2Unit), tile);

            // When: Unit moves to (2,3)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have 13 HP (10 + 3 instant from LIFE)
            Unit updatedUnit = afterMove.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();

            assertEquals(13, updatedUnit.getHp(), "Unit HP should be 13 (10 + 3 instant HP from LIFE)");
        }

        @Test
        @DisplayName("BT5: WEAKNESS buff tile applies -1 instant HP")
        void weaknessBuffTileAppliesInstantHp() {
            // Given: Unit with 10 HP at (2,2), WEAKNESS buff tile at (2,3)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile tile = createBuffTile("tile1", new Position(2, 3), BuffType.WEAKNESS);

            GameState state = createStateWithBuffTile(Arrays.asList(unit, p2Unit), tile);

            // When: Unit moves to (2,3)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have 9 HP (10 - 1 instant from WEAKNESS)
            Unit updatedUnit = afterMove.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();

            assertEquals(9, updatedUnit.getHp(), "Unit HP should be 9 (10 - 1 instant HP from WEAKNESS)");
        }

        @Test
        @DisplayName("BT6: Triggered tile is not triggered again")
        void triggeredTileNotTriggeredAgain() {
            // Given: Unit at (2,2), already-triggered tile at (2,3)
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile triggeredTile = new BuffTile("tile1", new Position(2, 3), BuffType.POWER, 2, true);  // already triggered

            GameState state = createStateWithBuffTile(Arrays.asList(unit, p2Unit), triggeredTile);

            // When: Unit moves to (2,3)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should NOT receive a buff (tile already triggered)
            List<BuffInstance> buffs = afterMove.getUnitBuffs().get("p1_unit");
            assertTrue(buffs == null || buffs.isEmpty(), "Unit should not receive buff from already-triggered tile");

            // HP should remain unchanged
            Unit updatedUnit = afterMove.getUnits().stream()
                .filter(u -> u.getId().equals("p1_unit"))
                .findFirst().orElseThrow();
            assertEquals(10, updatedUnit.getHp(), "Unit HP should remain 10 (tile already triggered)");
        }
    }

    @Nested
    @DisplayName("BT-Series: Random Buff Selection")
    class RandomBuffSelection {

        @Test
        @DisplayName("BT7: Random buff tile uses RngProvider")
        void randomBuffTileUsesRngProvider() {
            // Given: RngProvider seeded to return 0 (POWER)
            RngProvider seededRng = new RngProvider(12345L);
            ruleEngine.setRngProvider(seededRng);

            Unit unit = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile randomTile = createRandomBuffTile("tile1", new Position(2, 3));

            GameState state = createStateWithBuffTile(Arrays.asList(unit, p2Unit), randomTile);

            // When: Unit moves to (2,3)
            Action move = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove = ruleEngine.applyAction(state, move);

            // Then: Unit should have a buff (type determined by RngProvider)
            List<BuffInstance> buffs = afterMove.getUnitBuffs().get("p1_unit");
            assertNotNull(buffs, "Unit should have buffs");
            assertEquals(1, buffs.size(), "Unit should have 1 buff");
            assertNotNull(buffs.get(0).getType(), "Buff should have a type");
        }

        @Test
        @DisplayName("BT8: Seeded RngProvider produces deterministic results")
        void seededRngProducesDeterministicResults() {
            // Given: Same seed for two separate runs
            long seed = 42L;

            // First run
            RngProvider rng1 = new RngProvider(seed);
            ruleEngine.setRngProvider(rng1);

            Unit unit1 = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit p2Unit1 = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile randomTile1 = createRandomBuffTile("tile1", new Position(2, 3));
            GameState state1 = createStateWithBuffTile(Arrays.asList(unit1, p2Unit1), randomTile1);

            Action move1 = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove1 = ruleEngine.applyAction(state1, move1);
            BuffType type1 = afterMove1.getUnitBuffs().get("p1_unit").get(0).getType();

            // Second run with same seed (need new RuleEngine)
            RuleEngine ruleEngine2 = new RuleEngine();
            RngProvider rng2 = new RngProvider(seed);
            ruleEngine2.setRngProvider(rng2);

            Unit unit2 = createUnit("p1_unit", p1, new Position(2, 2), 10, 3);
            Unit p2Unit2 = createUnit("p2_unit", p2, new Position(4, 4));
            BuffTile randomTile2 = createRandomBuffTile("tile1", new Position(2, 3));
            GameState state2 = createStateWithBuffTile(Arrays.asList(unit2, p2Unit2), randomTile2);

            Action move2 = new Action(ActionType.MOVE, p1, new Position(2, 3), null);
            GameState afterMove2 = ruleEngine2.applyAction(state2, move2);
            BuffType type2 = afterMove2.getUnitBuffs().get("p1_unit").get(0).getType();

            // Then: Both runs should produce the same buff type
            assertEquals(type1, type2, "Same seed should produce same buff type");
        }
    }

    @Nested
    @DisplayName("BT-Series: Buff Tile Model Tests")
    class BuffTileModel {

        @Test
        @DisplayName("BT9: BuffTile stores all properties correctly")
        void buffTileStoresProperties() {
            // Given: A buff tile with specific properties
            String id = "test_tile";
            Position pos = new Position(2, 3);
            BuffType type = BuffType.SPEED;
            int duration = 2;
            boolean triggered = false;

            // When: Creating a BuffTile
            BuffTile tile = new BuffTile(id, pos, type, duration, triggered);

            // Then: All properties should be accessible
            assertEquals(id, tile.getId(), "ID should match");
            assertEquals(pos, tile.getPosition(), "Position should match");
            assertEquals(type, tile.getBuffType(), "BuffType should match");
            assertEquals(duration, tile.getDuration(), "Duration should match");
            assertEquals(triggered, tile.isTriggered(), "Triggered flag should match");
        }

        @Test
        @DisplayName("BT10: BuffTile equals and hashCode work correctly")
        void buffTileEqualsAndHashCode() {
            BuffTile tile1 = new BuffTile("tile1", new Position(2, 3), BuffType.POWER, 2, false);
            BuffTile tile2 = new BuffTile("tile1", new Position(2, 3), BuffType.POWER, 2, false);
            BuffTile tile3 = new BuffTile("tile2", new Position(2, 3), BuffType.POWER, 2, false);

            assertEquals(tile1, tile2, "Tiles with same properties should be equal");
            assertNotEquals(tile1, tile3, "Tiles with different IDs should not be equal");
            assertEquals(tile1.hashCode(), tile2.hashCode(), "Equal tiles should have same hashCode");
        }
    }
}
