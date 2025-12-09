package com.tactics.engine.rules;

import com.tactics.engine.buff.BuffFactory;
import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
import com.tactics.engine.buff.BuffType;
import com.tactics.engine.model.*;
import com.tactics.engine.util.GameStateSerializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BS-Series: Buff Serialization Tests (from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for buff serialization and deserialization:
 * - GameStateSerializer includes unitBuffs
 * - BuffInstance serializes all fields
 * - BuffType serializes as string
 * - Roundtrip serialization preserves state
 * - Empty buff list handling
 * - BuffTile serialization
 * - Forward compatibility with unknown fields
 */
@DisplayName("BS-Series: Buff Serialization Tests")
public class RuleEngineBuffSerializationTest {

    private GameStateSerializer serializer;
    private PlayerId p1;
    private PlayerId p2;
    private Board board;

    @BeforeEach
    void setUp() {
        serializer = new GameStateSerializer();
        p1 = new PlayerId("P1");
        p2 = new PlayerId("P2");
        board = new Board(5, 5);
    }

    // ========== Helper Methods ==========

    private Unit createUnit(String id, PlayerId owner, Position pos) {
        return new Unit(id, owner, 10, 3, 1, 1, pos, true);
    }

    private GameState createStateWithBuffs(List<Unit> units, Map<String, List<BuffInstance>> unitBuffs) {
        return new GameState(board, units, p1, false, null, unitBuffs,
            new ArrayList<>(), new ArrayList<>(), 1, null, false, false);
    }

    private GameState createStateWithBuffTiles(List<Unit> units, List<BuffTile> buffTiles) {
        return new GameState(board, units, p1, false, null, Collections.emptyMap(),
            buffTiles, new ArrayList<>(), 1, null, false, false);
    }

    // ========== BS-Series Tests ==========

    @Nested
    @DisplayName("BS1: GameStateSerializer.toJsonMap includes unitBuffs")
    class SerializerIncludesUnitBuffsTests {

        @Test
        @DisplayName("BS1: toJsonMap includes unitBuffs key")
        void toJsonMapIncludesUnitBuffs() {
            // Given: GameState with unit buffs
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("test_source");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: Serialize to JSON map
            Map<String, Object> json = serializer.toJsonMap(state);

            // Then: Should contain unitBuffs key
            assertTrue(json.containsKey("unitBuffs"),
                "Serialized JSON should contain 'unitBuffs' key");
        }
    }

    @Nested
    @DisplayName("BS2: BuffInstance serializes all fields")
    class BuffInstanceSerializationTests {

        @Test
        @DisplayName("BS2: BuffInstance serializes all fields correctly")
        @SuppressWarnings("unchecked")
        void buffInstanceSerializesAllFields() {
            // Given: GameState with POWER buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source_unit");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: Serialize
            Map<String, Object> json = serializer.toJsonMap(state);
            Map<String, Object> serializedBuffs = (Map<String, Object>) json.get("unitBuffs");
            List<Map<String, Object>> p1Buffs = (List<Map<String, Object>>) serializedBuffs.get("p1_unit");
            Map<String, Object> buffJson = p1Buffs.get(0);

            // Then: All fields should be present
            assertTrue(buffJson.containsKey("buffId"), "Serialized buff should have buffId");
            assertTrue(buffJson.containsKey("buffType"), "Serialized buff should have buffType");
            assertTrue(buffJson.containsKey("duration"), "Serialized buff should have duration");
            assertTrue(buffJson.containsKey("sourceUnitId"), "Serialized buff should have sourceUnitId");
            assertTrue(buffJson.containsKey("modifiers"), "Serialized buff should have modifiers");
            assertTrue(buffJson.containsKey("flags"), "Serialized buff should have flags");
            assertTrue(buffJson.containsKey("instantHpBonus"), "Serialized buff should have instantHpBonus");

            // Verify modifiers sub-fields
            Map<String, Object> modifiers = (Map<String, Object>) buffJson.get("modifiers");
            assertTrue(modifiers.containsKey("bonusAttack"), "Modifiers should have bonusAttack");
            assertTrue(modifiers.containsKey("bonusMoveRange"), "Modifiers should have bonusMoveRange");
            assertTrue(modifiers.containsKey("bonusAttackRange"), "Modifiers should have bonusAttackRange");

            // Verify flags sub-fields
            Map<String, Object> flags = (Map<String, Object>) buffJson.get("flags");
            assertTrue(flags.containsKey("powerBuff"), "Flags should have powerBuff");
            assertTrue(flags.containsKey("speedBuff"), "Flags should have speedBuff");
            assertTrue(flags.containsKey("slowBuff"), "Flags should have slowBuff");
            assertTrue(flags.containsKey("bleedBuff"), "Flags should have bleedBuff");
        }
    }

    @Nested
    @DisplayName("BS3: BuffType serializes as string")
    class BuffTypeSerializationTests {

        @Test
        @DisplayName("BS3: BuffType serializes as string, not ordinal")
        @SuppressWarnings("unchecked")
        void buffTypeSerializesAsString() {
            // Given: GameState with POWER buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source_unit");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState state = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: Serialize
            Map<String, Object> json = serializer.toJsonMap(state);
            Map<String, Object> serializedBuffs = (Map<String, Object>) json.get("unitBuffs");
            List<Map<String, Object>> p1Buffs = (List<Map<String, Object>>) serializedBuffs.get("p1_unit");
            Map<String, Object> buffJson = p1Buffs.get(0);

            // Then: buffType should be "POWER" string
            Object buffType = buffJson.get("buffType");
            assertTrue(buffType instanceof String, "BuffType should serialize as String");
            assertEquals("POWER", buffType, "BuffType should serialize as 'POWER'");
        }
    }

    @Nested
    @DisplayName("BS4: fromJsonMap reconstructs buff list exactly")
    class RoundtripSerializationTests {

        @Test
        @DisplayName("BS4: Roundtrip serialization preserves buffs")
        void roundtripPreservesBuffs() {
            // Given: GameState with multiple buffs
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source_unit");
            BuffInstance bleedBuff = BuffFactory.createBleed("enemy_unit");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Arrays.asList(powerBuff, bleedBuff));

            GameState originalState = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: Serialize and deserialize
            Map<String, Object> json = serializer.toJsonMap(originalState);
            GameState reconstructedState = serializer.fromJsonMap(json);

            // Then: Buffs should match
            List<BuffInstance> originalBuffs = originalState.getUnitBuffs().get("p1_unit");
            List<BuffInstance> reconstructedBuffs = reconstructedState.getUnitBuffs().get("p1_unit");

            assertNotNull(reconstructedBuffs, "Reconstructed state should have buffs for p1_unit");
            assertEquals(originalBuffs.size(), reconstructedBuffs.size(),
                "Reconstructed buff count should match original");

            // Verify each buff
            for (int i = 0; i < originalBuffs.size(); i++) {
                BuffInstance orig = originalBuffs.get(i);
                BuffInstance recon = reconstructedBuffs.get(i);

                assertEquals(orig.getType(), recon.getType(), "Buff type should match");
                assertEquals(orig.getDuration(), recon.getDuration(), "Buff duration should match");
                assertEquals(orig.getSourceUnitId(), recon.getSourceUnitId(), "Source unit ID should match");
                assertEquals(orig.getInstantHpBonus(), recon.getInstantHpBonus(), "Instant HP bonus should match");
            }
        }
    }

    @Nested
    @DisplayName("BS5: Empty buff list serializes & deserializes correctly")
    class EmptyBuffListTests {

        @Test
        @DisplayName("BS5: Empty buff list serializes and deserializes correctly")
        void emptyBuffListRoundtrip() {
            // Given: GameState with no buffs
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            GameState originalState = createStateWithBuffs(Arrays.asList(unit, p2Unit), Collections.emptyMap());

            // When: Serialize and deserialize
            Map<String, Object> json = serializer.toJsonMap(originalState);
            GameState reconstructedState = serializer.fromJsonMap(json);

            // Then: UnitBuffs should be empty
            assertTrue(reconstructedState.getUnitBuffs().isEmpty(),
                "Reconstructed state should have empty unitBuffs");
        }
    }

    @Nested
    @DisplayName("BS6: BuffTile list serializes correctly")
    class BuffTileSerializationTests {

        @Test
        @DisplayName("BS6: BuffTile list serializes correctly")
        void buffTileListSerializesCorrectly() {
            // Given: GameState with buff tiles
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffTile powerTile = new BuffTile("tile1", new Position(1, 1), BuffType.POWER, 2, false);
            BuffTile lifeTile = new BuffTile("tile2", new Position(3, 3), BuffType.LIFE, 1, false);

            GameState state = createStateWithBuffTiles(Arrays.asList(unit, p2Unit),
                Arrays.asList(powerTile, lifeTile));

            // When: Serialize
            Map<String, Object> json = serializer.toJsonMap(state);

            // Then: Should contain buffTiles key
            assertTrue(json.containsKey("buffTiles"), "Serialized JSON should contain 'buffTiles' key");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> buffTiles = (List<Map<String, Object>>) json.get("buffTiles");
            assertEquals(2, buffTiles.size(), "Should have 2 buff tiles");
        }
    }

    @Nested
    @DisplayName("BS7: BuffTile serializes position, type, duration, triggered")
    class BuffTileFieldSerializationTests {

        @Test
        @DisplayName("BS7: BuffTile serializes all required fields")
        @SuppressWarnings("unchecked")
        void buffTileSerializesAllFields() {
            // Given: GameState with buff tile
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffTile powerTile = new BuffTile("tile1", new Position(1, 1), BuffType.POWER, 2, false);

            GameState state = createStateWithBuffTiles(Arrays.asList(unit, p2Unit),
                Collections.singletonList(powerTile));

            // When: Serialize
            Map<String, Object> json = serializer.toJsonMap(state);
            List<Map<String, Object>> buffTiles = (List<Map<String, Object>>) json.get("buffTiles");
            Map<String, Object> tileJson = buffTiles.get(0);

            // Then: All required fields should be present
            assertTrue(tileJson.containsKey("id"), "BuffTile should have id");
            assertTrue(tileJson.containsKey("position"), "BuffTile should have position");
            assertTrue(tileJson.containsKey("buffType"), "BuffTile should have buffType");
            assertTrue(tileJson.containsKey("duration"), "BuffTile should have duration");
            assertTrue(tileJson.containsKey("triggered"), "BuffTile should have triggered");

            // Verify position sub-fields
            Map<String, Object> position = (Map<String, Object>) tileJson.get("position");
            assertTrue(position.containsKey("x"), "Position should have x");
            assertTrue(position.containsKey("y"), "Position should have y");
            assertEquals(1, position.get("x"), "Position x should be 1");
            assertEquals(1, position.get("y"), "Position y should be 1");

            // Verify values
            assertEquals("POWER", tileJson.get("buffType"), "BuffType should be POWER");
            assertEquals(2, tileJson.get("duration"), "Duration should be 2");
            assertEquals(false, tileJson.get("triggered"), "Triggered should be false");
        }
    }

    @Nested
    @DisplayName("BS8: Unknown buff fields in JSON are ignored (forward compatibility)")
    class ForwardCompatibilityTests {

        @Test
        @DisplayName("BS8: Unknown fields in buff JSON are ignored")
        void unknownFieldsIgnored() {
            // Given: GameState with buff
            Unit unit = createUnit("p1_unit", p1, new Position(2, 2));
            Unit p2Unit = createUnit("p2_unit", p2, new Position(4, 4));

            BuffInstance powerBuff = BuffFactory.createPower("source_unit");
            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("p1_unit", Collections.singletonList(powerBuff));

            GameState originalState = createStateWithBuffs(Arrays.asList(unit, p2Unit), unitBuffs);

            // When: Serialize, add unknown field, then deserialize
            Map<String, Object> json = serializer.toJsonMap(originalState);

            // Add unknown field to buff
            @SuppressWarnings("unchecked")
            Map<String, Object> serializedBuffs = (Map<String, Object>) json.get("unitBuffs");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> p1Buffs = (List<Map<String, Object>>) serializedBuffs.get("p1_unit");
            p1Buffs.get(0).put("unknownFutureField", "some_value");
            p1Buffs.get(0).put("anotherUnknownField", 12345);

            // Should not throw exception
            GameState reconstructedState = serializer.fromJsonMap(json);

            // Then: State should be valid with known fields intact
            List<BuffInstance> reconstructedBuffs = reconstructedState.getUnitBuffs().get("p1_unit");
            assertNotNull(reconstructedBuffs, "Buffs should be reconstructed");
            assertEquals(1, reconstructedBuffs.size(), "Should have 1 buff");
            assertEquals(BuffType.POWER, reconstructedBuffs.get(0).getType(), "Buff type should be POWER");
        }
    }
}
