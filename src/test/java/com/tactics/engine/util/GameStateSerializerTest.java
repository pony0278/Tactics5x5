package com.tactics.engine.util;

import com.tactics.engine.buff.BuffFlags;
import com.tactics.engine.buff.BuffInstance;
import com.tactics.engine.buff.BuffModifier;
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
 * JUnit 5 test class for GameStateSerializer.
 * Tests follow GAMESTATE_SERIALIZER_TESTPLAN_V1.md exactly.
 */
@DisplayName("GameStateSerializer Tests")
class GameStateSerializerTest {

    private GameStateSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new GameStateSerializer();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private GameState createSimpleGameState() {
        return new GameState(
            new Board(5, 5),
            new ArrayList<>(),
            new PlayerId("P1"),
            false,
            null
        );
    }

    private GameState createGameStateWithUnits(List<Unit> units) {
        return new GameState(
            new Board(5, 5),
            units,
            new PlayerId("P1"),
            false,
            null
        );
    }

    private Map<String, Object> createValidMap() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> boardMap = new HashMap<>();
        boardMap.put("width", 5);
        boardMap.put("height", 5);
        map.put("board", boardMap);
        map.put("units", new ArrayList<>());
        map.put("currentPlayer", "P1");
        map.put("gameOver", false);
        map.put("winner", null);
        return map;
    }

    private Map<String, Object> createUnitMap(String id, String owner, int hp, int attack, int x, int y, boolean alive) {
        return createUnitMap(id, owner, hp, attack, 1, 1, x, y, alive);
    }

    private Map<String, Object> createUnitMap(String id, String owner, int hp, int attack, int moveRange, int attackRange, int x, int y, boolean alive) {
        Map<String, Object> unitMap = new HashMap<>();
        unitMap.put("id", id);
        unitMap.put("owner", owner);
        unitMap.put("hp", hp);
        unitMap.put("attack", attack);
        unitMap.put("moveRange", moveRange);
        unitMap.put("attackRange", attackRange);
        Map<String, Object> posMap = new HashMap<>();
        posMap.put("x", x);
        posMap.put("y", y);
        unitMap.put("position", posMap);
        unitMap.put("alive", alive);
        return unitMap;
    }

    // =========================================================================
    // TJM-Series: toJsonMap Tests
    // =========================================================================

    @Nested
    @DisplayName("toJsonMap Tests (TJM-Series)")
    class ToJsonMapTests {

        @Test
        @DisplayName("TJM1 - Board serialization")
        void tjm1_boardSerialization() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertTrue(result.containsKey("board"));
            @SuppressWarnings("unchecked")
            Map<String, Object> boardMap = (Map<String, Object>) result.get("board");
            assertNotNull(boardMap);
            assertEquals(5, boardMap.get("width"));
            assertEquals(5, boardMap.get("height"));
        }

        @Test
        @DisplayName("TJM2 - CurrentPlayer P1 serialization")
        void tjm2_currentPlayerP1Serialization() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertEquals("P1", result.get("currentPlayer"));
        }

        @Test
        @DisplayName("TJM3 - CurrentPlayer P2 serialization")
        void tjm3_currentPlayerP2Serialization() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P2"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertEquals("P2", result.get("currentPlayer"));
        }

        @Test
        @DisplayName("TJM4 - GameOver false serialization")
        void tjm4_gameOverFalseSerialization() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertEquals(false, result.get("gameOver"));
        }

        @Test
        @DisplayName("TJM5 - GameOver true serialization")
        void tjm5_gameOverTrueSerialization() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                true,
                new PlayerId("P1")
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertEquals(true, result.get("gameOver"));
        }

        @Test
        @DisplayName("TJM6 - Winner null serialization")
        void tjm6_winnerNullSerialization() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertTrue(result.containsKey("winner"));
            assertNull(result.get("winner"));
        }

        @Test
        @DisplayName("TJM7 - Winner P1 serialization")
        void tjm7_winnerP1Serialization() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                true,
                new PlayerId("P1")
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertEquals("P1", result.get("winner"));
        }

        @Test
        @DisplayName("TJM8 - Empty units list serialization")
        void tjm8_emptyUnitsListSerialization() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertTrue(result.containsKey("units"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsList = (List<Map<String, Object>>) result.get("units");
            assertNotNull(unitsList);
            assertTrue(unitsList.isEmpty());
        }

        @Test
        @DisplayName("TJM9 - Single unit serialization")
        void tjm9_singleUnitSerialization() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1_p1", new PlayerId("P1"), 10, 3, 1, 1, new Position(2, 2), true));
            GameState state = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsList = (List<Map<String, Object>>) result.get("units");
            assertNotNull(unitsList);
            assertEquals(1, unitsList.size());

            Map<String, Object> unitMap = unitsList.get(0);
            assertEquals("u1_p1", unitMap.get("id"));
            assertEquals("P1", unitMap.get("owner"));
            assertEquals(10, unitMap.get("hp"));
            assertEquals(3, unitMap.get("attack"));
            assertEquals(1, unitMap.get("moveRange"));
            assertEquals(1, unitMap.get("attackRange"));
            assertEquals(true, unitMap.get("alive"));

            @SuppressWarnings("unchecked")
            Map<String, Object> posMap = (Map<String, Object>) unitMap.get("position");
            assertNotNull(posMap);
            assertEquals(2, posMap.get("x"));
            assertEquals(2, posMap.get("y"));
        }

        @Test
        @DisplayName("TJM10 - Multiple units serialization")
        void tjm10_multipleUnitsSerialization() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 10, 3, 1, 1, new Position(0, 0), true));
            units.add(new Unit("u2", new PlayerId("P2"), 8, 4, 1, 1, new Position(4, 4), true));
            GameState state = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsList = (List<Map<String, Object>>) result.get("units");
            assertNotNull(unitsList);
            assertEquals(2, unitsList.size());

            // Verify first unit
            Map<String, Object> unit1 = unitsList.get(0);
            assertEquals("u1", unit1.get("id"));
            assertEquals("P1", unit1.get("owner"));
            assertEquals(10, unit1.get("hp"));
            assertEquals(3, unit1.get("attack"));
            assertEquals(true, unit1.get("alive"));
            @SuppressWarnings("unchecked")
            Map<String, Object> pos1 = (Map<String, Object>) unit1.get("position");
            assertEquals(0, pos1.get("x"));
            assertEquals(0, pos1.get("y"));

            // Verify second unit
            Map<String, Object> unit2 = unitsList.get(1);
            assertEquals("u2", unit2.get("id"));
            assertEquals("P2", unit2.get("owner"));
            assertEquals(8, unit2.get("hp"));
            assertEquals(4, unit2.get("attack"));
            assertEquals(true, unit2.get("alive"));
            @SuppressWarnings("unchecked")
            Map<String, Object> pos2 = (Map<String, Object>) unit2.get("position");
            assertEquals(4, pos2.get("x"));
            assertEquals(4, pos2.get("y"));
        }

        @Test
        @DisplayName("TJM11 - Dead unit serialization")
        void tjm11_deadUnitSerialization() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 0, 3, 1, 1, new Position(1, 1), false));
            GameState state = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsList = (List<Map<String, Object>>) result.get("units");
            assertNotNull(unitsList);
            assertEquals(1, unitsList.size());

            Map<String, Object> unitMap = unitsList.get(0);
            assertEquals(false, unitMap.get("alive"));
            assertEquals(0, unitMap.get("hp"));
        }

        @Test
        @DisplayName("TJM12 - Position serialization")
        void tjm12_positionSerialization() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 10, 3, 1, 1, new Position(3, 4), true));
            GameState state = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsList = (List<Map<String, Object>>) result.get("units");
            Map<String, Object> unitMap = unitsList.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> posMap = (Map<String, Object>) unitMap.get("position");
            assertNotNull(posMap);
            assertEquals(3, posMap.get("x"));
            assertEquals(4, posMap.get("y"));
        }
    }

    // =========================================================================
    // FJM-Series: fromJsonMap Tests
    // =========================================================================

    @Nested
    @DisplayName("fromJsonMap Tests (FJM-Series)")
    class FromJsonMapTests {

        @Test
        @DisplayName("FJM1 - Board deserialization")
        void fjm1_boardDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            Map<String, Object> boardMap = new HashMap<>();
            boardMap.put("width", 5);
            boardMap.put("height", 5);
            map.put("board", boardMap);

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertNotNull(result.getBoard());
            assertEquals(5, result.getBoard().getWidth());
            assertEquals(5, result.getBoard().getHeight());
        }

        @Test
        @DisplayName("FJM2 - CurrentPlayer deserialization")
        void fjm2_currentPlayerDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            map.put("currentPlayer", "P1");

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertNotNull(result.getCurrentPlayer());
            assertEquals("P1", result.getCurrentPlayer().getValue());
        }

        @Test
        @DisplayName("FJM3 - GameOver deserialization")
        void fjm3_gameOverDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            map.put("gameOver", true);
            map.put("winner", "P1");

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertTrue(result.isGameOver());
        }

        @Test
        @DisplayName("FJM4 - Winner deserialization")
        void fjm4_winnerDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            map.put("gameOver", true);
            map.put("winner", "P2");

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertNotNull(result.getWinner());
            assertEquals("P2", result.getWinner().getValue());
        }

        @Test
        @DisplayName("FJM5 - Winner null deserialization")
        void fjm5_winnerNullDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            map.put("winner", null);

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertNull(result.getWinner());
        }

        @Test
        @DisplayName("FJM6 - Empty units deserialization")
        void fjm6_emptyUnitsDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            map.put("units", new ArrayList<>());

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertNotNull(result.getUnits());
            assertTrue(result.getUnits().isEmpty());
        }

        @Test
        @DisplayName("FJM7 - Single unit deserialization")
        void fjm7_singleUnitDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            List<Map<String, Object>> unitsList = new ArrayList<>();
            unitsList.add(createUnitMap("u1", "P1", 10, 3, 2, 3, true));
            map.put("units", unitsList);

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertNotNull(result.getUnits());
            assertEquals(1, result.getUnits().size());

            Unit unit = result.getUnits().get(0);
            assertEquals("u1", unit.getId());
            assertEquals("P1", unit.getOwner().getValue());
            assertEquals(10, unit.getHp());
            assertEquals(3, unit.getAttack());
            assertEquals(1, unit.getMoveRange());
            assertEquals(1, unit.getAttackRange());
            assertEquals(2, unit.getPosition().getX());
            assertEquals(3, unit.getPosition().getY());
            assertTrue(unit.isAlive());
        }

        @Test
        @DisplayName("FJM8 - Multiple units deserialization")
        void fjm8_multipleUnitsDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            List<Map<String, Object>> unitsList = new ArrayList<>();
            unitsList.add(createUnitMap("u1", "P1", 10, 3, 0, 0, true));
            unitsList.add(createUnitMap("u2", "P2", 8, 4, 4, 4, true));
            map.put("units", unitsList);

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertNotNull(result.getUnits());
            assertEquals(2, result.getUnits().size());

            // Verify first unit
            Unit unit1 = result.getUnits().get(0);
            assertEquals("u1", unit1.getId());
            assertEquals("P1", unit1.getOwner().getValue());
            assertEquals(10, unit1.getHp());
            assertEquals(3, unit1.getAttack());
            assertEquals(0, unit1.getPosition().getX());
            assertEquals(0, unit1.getPosition().getY());
            assertTrue(unit1.isAlive());

            // Verify second unit
            Unit unit2 = result.getUnits().get(1);
            assertEquals("u2", unit2.getId());
            assertEquals("P2", unit2.getOwner().getValue());
            assertEquals(8, unit2.getHp());
            assertEquals(4, unit2.getAttack());
            assertEquals(4, unit2.getPosition().getX());
            assertEquals(4, unit2.getPosition().getY());
            assertTrue(unit2.isAlive());
        }

        @Test
        @DisplayName("FJM9 - Dead unit deserialization")
        void fjm9_deadUnitDeserialization() {
            // Given
            Map<String, Object> map = createValidMap();
            List<Map<String, Object>> unitsList = new ArrayList<>();
            unitsList.add(createUnitMap("u1", "P1", 0, 3, 1, 1, false));
            map.put("units", unitsList);

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getUnits().size());

            Unit unit = result.getUnits().get(0);
            assertFalse(unit.isAlive());
            assertEquals(0, unit.getHp());
        }
    }

    // =========================================================================
    // RT-Series: Roundtrip Tests
    // =========================================================================

    @Nested
    @DisplayName("Roundtrip Tests (RT-Series)")
    class RoundtripTests {

        @Test
        @DisplayName("RT1 - Basic roundtrip with empty units")
        void rt1_basicRoundtripWithEmptyUnits() {
            // Given
            GameState original = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals(5, reconstructed.getBoard().getWidth());
            assertEquals(5, reconstructed.getBoard().getHeight());
            assertTrue(reconstructed.getUnits().isEmpty());
            assertEquals("P1", reconstructed.getCurrentPlayer().getValue());
            assertFalse(reconstructed.isGameOver());
            assertNull(reconstructed.getWinner());
        }

        @Test
        @DisplayName("RT2 - Roundtrip with single unit")
        void rt2_roundtripWithSingleUnit() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 10, 3, 1, 1, new Position(2, 2), true));
            GameState original = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals(1, reconstructed.getUnits().size());

            Unit unit = reconstructed.getUnits().get(0);
            assertEquals("u1", unit.getId());
            assertEquals("P1", unit.getOwner().getValue());
            assertEquals(10, unit.getHp());
            assertEquals(3, unit.getAttack());
            assertEquals(1, unit.getMoveRange());
            assertEquals(1, unit.getAttackRange());
            assertEquals(2, unit.getPosition().getX());
            assertEquals(2, unit.getPosition().getY());
            assertTrue(unit.isAlive());
        }

        @Test
        @DisplayName("RT3 - Roundtrip with multiple units")
        void rt3_roundtripWithMultipleUnits() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 10, 3, 1, 1, new Position(0, 0), true));
            units.add(new Unit("u2", new PlayerId("P1"), 8, 2, 1, 1, new Position(1, 1), true));
            units.add(new Unit("u3", new PlayerId("P2"), 12, 4, 1, 1, new Position(4, 4), true));
            GameState original = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals(3, reconstructed.getUnits().size());

            // Verify all units have correct fields
            Unit unit1 = reconstructed.getUnits().get(0);
            assertEquals("u1", unit1.getId());
            assertEquals("P1", unit1.getOwner().getValue());
            assertEquals(10, unit1.getHp());
            assertEquals(3, unit1.getAttack());
            assertEquals(0, unit1.getPosition().getX());
            assertEquals(0, unit1.getPosition().getY());
            assertTrue(unit1.isAlive());

            Unit unit2 = reconstructed.getUnits().get(1);
            assertEquals("u2", unit2.getId());
            assertEquals("P1", unit2.getOwner().getValue());
            assertEquals(8, unit2.getHp());
            assertEquals(2, unit2.getAttack());
            assertEquals(1, unit2.getPosition().getX());
            assertEquals(1, unit2.getPosition().getY());
            assertTrue(unit2.isAlive());

            Unit unit3 = reconstructed.getUnits().get(2);
            assertEquals("u3", unit3.getId());
            assertEquals("P2", unit3.getOwner().getValue());
            assertEquals(12, unit3.getHp());
            assertEquals(4, unit3.getAttack());
            assertEquals(4, unit3.getPosition().getX());
            assertEquals(4, unit3.getPosition().getY());
            assertTrue(unit3.isAlive());
        }

        @Test
        @DisplayName("RT4 - Roundtrip with game over state")
        void rt4_roundtripWithGameOverState() {
            // Given
            GameState original = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                true,
                new PlayerId("P1")
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertTrue(reconstructed.isGameOver());
            assertNotNull(reconstructed.getWinner());
            assertEquals("P1", reconstructed.getWinner().getValue());
        }

        @Test
        @DisplayName("RT5 - Roundtrip with dead unit")
        void rt5_roundtripWithDeadUnit() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P2"), 0, 3, 1, 1, new Position(1, 1), false));
            GameState original = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals(1, reconstructed.getUnits().size());

            Unit unit = reconstructed.getUnits().get(0);
            assertFalse(unit.isAlive());
            assertEquals(0, unit.getHp());
        }

        @Test
        @DisplayName("RT6 - Roundtrip with P2 as current player")
        void rt6_roundtripWithP2AsCurrentPlayer() {
            // Given
            GameState original = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P2"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals("P2", reconstructed.getCurrentPlayer().getValue());
        }

        @Test
        @DisplayName("RT7 - Roundtrip preserves unit order")
        void rt7_roundtripPreservesUnitOrder() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 10, 3, 1, 1, new Position(0, 0), true));
            units.add(new Unit("u2", new PlayerId("P1"), 8, 2, 1, 1, new Position(1, 1), true));
            units.add(new Unit("u3", new PlayerId("P2"), 12, 4, 1, 1, new Position(4, 4), true));
            GameState original = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals(3, reconstructed.getUnits().size());

            // Verify order is preserved by ID
            assertEquals("u1", reconstructed.getUnits().get(0).getId());
            assertEquals("u2", reconstructed.getUnits().get(1).getId());
            assertEquals("u3", reconstructed.getUnits().get(2).getId());
        }
    }

    // =========================================================================
    // EC-Series: Edge Case Tests
    // =========================================================================

    @Nested
    @DisplayName("Edge Case Tests (EC-Series)")
    class EdgeCaseTests {

        @Test
        @DisplayName("EC1 - Null GameState to toJsonMap")
        void ec1_nullGameStateToJsonMap() {
            // Given
            GameState state = null;

            // When / Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serializer.toJsonMap(state)
            );
            assertEquals("GameState cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("EC2 - Null Map to fromJsonMap")
        void ec2_nullMapToFromJsonMap() {
            // Given
            Map<String, Object> map = null;

            // When / Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serializer.fromJsonMap(map)
            );
            assertEquals("Map cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("EC3 - Map missing required fields")
        void ec3_mapMissingRequiredFields() {
            // Given - map missing "board" key
            Map<String, Object> map = new HashMap<>();
            map.put("units", new ArrayList<>());
            map.put("currentPlayer", "P1");
            map.put("gameOver", false);
            map.put("winner", null);

            // When / Then
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> serializer.fromJsonMap(map)
            );
            assertTrue(exception.getMessage().toLowerCase().contains("board") ||
                       exception.getMessage().toLowerCase().contains("missing") ||
                       exception.getMessage().toLowerCase().contains("required"));
        }

        @Test
        @DisplayName("EC4 - Board with non-standard dimensions")
        void ec4_boardWithNonStandardDimensions() {
            // Given
            GameState state = new GameState(
                new Board(10, 10),
                new ArrayList<>(),
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            @SuppressWarnings("unchecked")
            Map<String, Object> boardMap = (Map<String, Object>) result.get("board");
            assertNotNull(boardMap);
            assertEquals(10, boardMap.get("width"));
            assertEquals(10, boardMap.get("height"));
        }

        @Test
        @DisplayName("EC5 - Unit at edge position (0,0)")
        void ec5_unitAtEdgePositionZeroZero() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 10, 3, 1, 1, new Position(0, 0), true));
            GameState original = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals(1, reconstructed.getUnits().size());
            Unit unit = reconstructed.getUnits().get(0);
            assertEquals(0, unit.getPosition().getX());
            assertEquals(0, unit.getPosition().getY());
        }

        @Test
        @DisplayName("EC6 - Unit at edge position (4,4)")
        void ec6_unitAtEdgePositionFourFour() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 10, 3, 1, 1, new Position(4, 4), true));
            GameState original = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals(1, reconstructed.getUnits().size());
            Unit unit = reconstructed.getUnits().get(0);
            assertEquals(4, unit.getPosition().getX());
            assertEquals(4, unit.getPosition().getY());
        }

        @Test
        @DisplayName("EC7 - Unit with zero attack")
        void ec7_unitWithZeroAttack() {
            // Given
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1", new PlayerId("P1"), 10, 0, 1, 1, new Position(1, 1), true));
            GameState original = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null
            );

            // When
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then
            assertNotNull(reconstructed);
            assertEquals(1, reconstructed.getUnits().size());
            Unit unit = reconstructed.getUnits().get(0);
            assertEquals(0, unit.getAttack());
        }

        @Test
        @DisplayName("EC8 - Game over with no winner (draw)")
        void ec8_gameOverWithNoWinner() {
            // Given
            GameState state = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                true,
                null
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertEquals(true, result.get("gameOver"));
            assertTrue(result.containsKey("winner"));
            assertNull(result.get("winner"));
        }
    }

    // =========================================================================
    // BS-Series: Buff Serialization Tests (BUFF_SYSTEM_V1_TESTPLAN.md)
    // =========================================================================

    @Nested
    @DisplayName("Buff Serialization Tests (BS-Series)")
    class BuffSerializationTests {

        private BuffInstance createRageBuff(String sourceUnitId) {
            BuffModifier modifiers = new BuffModifier(0, 2, 0, 0);
            BuffFlags flags = new BuffFlags(false, false, false, false, false);
            return new BuffInstance("RAGE", sourceUnitId, 1, false, modifiers, flags);
        }

        private BuffInstance createPoisonBuff(String sourceUnitId) {
            BuffModifier modifiers = new BuffModifier(0, 0, 0, 0);
            BuffFlags flags = new BuffFlags(false, false, true, false, false);
            return new BuffInstance("POISON", sourceUnitId, 2, true, modifiers, flags);
        }

        private BuffInstance createStunBuff(String sourceUnitId) {
            BuffModifier modifiers = new BuffModifier(0, 0, 0, 0);
            BuffFlags flags = new BuffFlags(true, false, false, false, false);
            return new BuffInstance("STUN", sourceUnitId, 1, false, modifiers, flags);
        }

        private BuffInstance createHasteBuff(String sourceUnitId) {
            BuffModifier modifiers = new BuffModifier(0, 0, 1, 0);
            BuffFlags flags = new BuffFlags(false, false, false, false, false);
            return new BuffInstance("HASTE", sourceUnitId, 1, false, modifiers, flags);
        }

        @Test
        @DisplayName("BS1 - toJsonMap includes unitBuffs key")
        void bs1_toJsonMapIncludesUnitBuffs() {
            // Given: GameState with one unit having one BuffInstance
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1_p1", new PlayerId("P1"), 10, 3, 1, 1, new Position(0, 0), true));

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("u1_p1", Arrays.asList(createRageBuff("u2_p1")));

            GameState state = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null,
                unitBuffs
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            assertNotNull(result);
            assertTrue(result.containsKey("unitBuffs"));

            @SuppressWarnings("unchecked")
            Map<String, Object> buffsMap = (Map<String, Object>) result.get("unitBuffs");
            assertNotNull(buffsMap);
            assertTrue(buffsMap.containsKey("u1_p1"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> buffList = (List<Map<String, Object>>) buffsMap.get("u1_p1");
            assertNotNull(buffList);
            assertEquals(1, buffList.size());
        }

        @Test
        @DisplayName("BS2 - Buff serializes all fields")
        void bs2_buffSerializesAllFields() {
            // Given: GameState with a buff that has all fields set
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1_p1", new PlayerId("P1"), 10, 3, 1, 1, new Position(0, 0), true));

            BuffModifier modifiers = new BuffModifier(5, 2, 1, 3);
            BuffFlags flags = new BuffFlags(true, true, true, true, true);
            BuffInstance buff = new BuffInstance("TEST_BUFF", "u2_p1", 3, true, modifiers, flags);

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("u1_p1", Arrays.asList(buff));

            GameState state = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null,
                unitBuffs
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(state);

            // Then
            @SuppressWarnings("unchecked")
            Map<String, Object> buffsMap = (Map<String, Object>) result.get("unitBuffs");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> buffList = (List<Map<String, Object>>) buffsMap.get("u1_p1");
            Map<String, Object> buffMap = buffList.get(0);

            // Verify buff fields
            assertEquals("TEST_BUFF", buffMap.get("buffId"));
            assertEquals("u2_p1", buffMap.get("sourceUnitId"));
            assertEquals(3, buffMap.get("duration"));
            assertEquals(true, buffMap.get("stackable"));

            // Verify modifiers
            @SuppressWarnings("unchecked")
            Map<String, Object> modifiersMap = (Map<String, Object>) buffMap.get("modifiers");
            assertNotNull(modifiersMap);
            assertEquals(5, modifiersMap.get("bonusHp"));
            assertEquals(2, modifiersMap.get("bonusAttack"));
            assertEquals(1, modifiersMap.get("bonusMoveRange"));
            assertEquals(3, modifiersMap.get("bonusAttackRange"));

            // Verify flags
            @SuppressWarnings("unchecked")
            Map<String, Object> flagsMap = (Map<String, Object>) buffMap.get("flags");
            assertNotNull(flagsMap);
            assertEquals(true, flagsMap.get("stunned"));
            assertEquals(true, flagsMap.get("rooted"));
            assertEquals(true, flagsMap.get("poison"));
            assertEquals(true, flagsMap.get("silenced"));
            assertEquals(true, flagsMap.get("taunted"));
        }

        @Test
        @DisplayName("BS3 - Roundtrip preserves buffs")
        void bs3_roundtripPreservesBuffs() {
            // Given: GameState with 2 units, each with 1-2 BuffInstances
            List<Unit> units = new ArrayList<>();
            units.add(new Unit("u1_p1", new PlayerId("P1"), 10, 3, 1, 1, new Position(0, 0), true));
            units.add(new Unit("u2_p2", new PlayerId("P2"), 8, 4, 1, 2, new Position(4, 4), true));

            Map<String, List<BuffInstance>> unitBuffs = new HashMap<>();
            unitBuffs.put("u1_p1", Arrays.asList(createRageBuff("u2_p2"), createHasteBuff(null)));
            unitBuffs.put("u2_p2", Arrays.asList(createPoisonBuff("u1_p1")));

            GameState original = new GameState(
                new Board(5, 5),
                units,
                new PlayerId("P1"),
                false,
                null,
                unitBuffs
            );

            // When: Roundtrip
            Map<String, Object> map = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(map);

            // Then: Verify unitBuffs match
            assertNotNull(reconstructed.getUnitBuffs());
            assertEquals(2, reconstructed.getUnitBuffs().size());

            // Verify u1_p1 buffs
            List<BuffInstance> u1Buffs = reconstructed.getUnitBuffs().get("u1_p1");
            assertNotNull(u1Buffs);
            assertEquals(2, u1Buffs.size());

            BuffInstance rageBuff = u1Buffs.get(0);
            assertEquals("RAGE", rageBuff.getBuffId());
            assertEquals("u2_p2", rageBuff.getSourceUnitId());
            assertEquals(1, rageBuff.getDuration());
            assertFalse(rageBuff.isStackable());
            assertEquals(2, rageBuff.getModifiers().getBonusAttack());
            assertFalse(rageBuff.getFlags().isStunned());

            BuffInstance hasteBuff = u1Buffs.get(1);
            assertEquals("HASTE", hasteBuff.getBuffId());
            assertNull(hasteBuff.getSourceUnitId());
            assertEquals(1, hasteBuff.getModifiers().getBonusMoveRange());

            // Verify u2_p2 buffs
            List<BuffInstance> u2Buffs = reconstructed.getUnitBuffs().get("u2_p2");
            assertNotNull(u2Buffs);
            assertEquals(1, u2Buffs.size());

            BuffInstance poisonBuff = u2Buffs.get(0);
            assertEquals("POISON", poisonBuff.getBuffId());
            assertEquals("u1_p1", poisonBuff.getSourceUnitId());
            assertEquals(2, poisonBuff.getDuration());
            assertTrue(poisonBuff.isStackable());
            assertTrue(poisonBuff.getFlags().isPoison());
        }

        @Test
        @DisplayName("BS4 - Empty buff map serializes and deserializes correctly")
        void bs4_emptyBuffMapSerialization() {
            // Given: GameState with empty unitBuffs
            GameState original = new GameState(
                new Board(5, 5),
                new ArrayList<>(),
                new PlayerId("P1"),
                false,
                null,
                Collections.emptyMap()
            );

            // When
            Map<String, Object> result = serializer.toJsonMap(original);

            // Then: unitBuffs should exist and be empty
            assertTrue(result.containsKey("unitBuffs"));
            @SuppressWarnings("unchecked")
            Map<String, Object> buffsMap = (Map<String, Object>) result.get("unitBuffs");
            assertNotNull(buffsMap);
            assertTrue(buffsMap.isEmpty());

            // Roundtrip should give empty map again
            GameState reconstructed = serializer.fromJsonMap(result);
            assertNotNull(reconstructed.getUnitBuffs());
            assertTrue(reconstructed.getUnitBuffs().isEmpty());
        }

        @Test
        @DisplayName("BS5 - Unknown fields in JSON are ignored")
        void bs5_unknownFieldsIgnored() {
            // Given: Valid map with buff containing unknown fields
            Map<String, Object> map = createValidMap();

            // Create buff map with extra unknown fields
            Map<String, Object> buffMap = new HashMap<>();
            buffMap.put("buffId", "RAGE");
            buffMap.put("sourceUnitId", "u2_p1");
            buffMap.put("duration", 1);
            buffMap.put("stackable", false);
            buffMap.put("unknownField1", "should be ignored");
            buffMap.put("futureExtension", 12345);

            Map<String, Object> modifiersMap = new HashMap<>();
            modifiersMap.put("bonusHp", 0);
            modifiersMap.put("bonusAttack", 2);
            modifiersMap.put("bonusMoveRange", 0);
            modifiersMap.put("bonusAttackRange", 0);
            modifiersMap.put("unknownModifier", 999);
            buffMap.put("modifiers", modifiersMap);

            Map<String, Object> flagsMap = new HashMap<>();
            flagsMap.put("stunned", false);
            flagsMap.put("rooted", false);
            flagsMap.put("poison", false);
            flagsMap.put("silenced", false);
            flagsMap.put("taunted", false);
            flagsMap.put("unknownFlag", true);
            buffMap.put("flags", flagsMap);

            List<Map<String, Object>> buffList = new ArrayList<>();
            buffList.add(buffMap);

            Map<String, Object> unitBuffsMap = new HashMap<>();
            unitBuffsMap.put("u1_p1", buffList);
            map.put("unitBuffs", unitBuffsMap);

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then: Should successfully deserialize without errors
            assertNotNull(result);
            assertNotNull(result.getUnitBuffs());
            assertEquals(1, result.getUnitBuffs().size());

            List<BuffInstance> buffs = result.getUnitBuffs().get("u1_p1");
            assertNotNull(buffs);
            assertEquals(1, buffs.size());

            BuffInstance buff = buffs.get(0);
            assertEquals("RAGE", buff.getBuffId());
            assertEquals(2, buff.getModifiers().getBonusAttack());
            assertFalse(buff.getFlags().isStunned());
        }

        @Test
        @DisplayName("BS5b - Missing unitBuffs key treated as empty map (forward compatibility)")
        void bs5b_missingUnitBuffsKeyTreatedAsEmpty() {
            // Given: Valid map without unitBuffs key (e.g., old format)
            Map<String, Object> map = createValidMap();
            // Ensure unitBuffs is NOT present
            map.remove("unitBuffs");

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then: Should successfully deserialize with empty unitBuffs
            assertNotNull(result);
            assertNotNull(result.getUnitBuffs());
            assertTrue(result.getUnitBuffs().isEmpty());
        }

        @Test
        @DisplayName("BS5c - Missing silenced/taunted flags default to false")
        void bs5c_missingFlagsDefaultToFalse() {
            // Given: Buff with only V1 flags (missing silenced/taunted)
            Map<String, Object> map = createValidMap();

            Map<String, Object> buffMap = new HashMap<>();
            buffMap.put("buffId", "STUN");
            buffMap.put("sourceUnitId", null);
            buffMap.put("duration", 1);
            buffMap.put("stackable", false);

            Map<String, Object> modifiersMap = new HashMap<>();
            modifiersMap.put("bonusHp", 0);
            modifiersMap.put("bonusAttack", 0);
            modifiersMap.put("bonusMoveRange", 0);
            modifiersMap.put("bonusAttackRange", 0);
            buffMap.put("modifiers", modifiersMap);

            // Only V1 flags, missing silenced and taunted
            Map<String, Object> flagsMap = new HashMap<>();
            flagsMap.put("stunned", true);
            flagsMap.put("rooted", false);
            flagsMap.put("poison", false);
            // silenced and taunted are missing
            buffMap.put("flags", flagsMap);

            List<Map<String, Object>> buffList = new ArrayList<>();
            buffList.add(buffMap);

            Map<String, Object> unitBuffsMap = new HashMap<>();
            unitBuffsMap.put("u1_p1", buffList);
            map.put("unitBuffs", unitBuffsMap);

            // When
            GameState result = serializer.fromJsonMap(map);

            // Then: silenced and taunted should default to false
            BuffInstance buff = result.getUnitBuffs().get("u1_p1").get(0);
            assertTrue(buff.getFlags().isStunned());
            assertFalse(buff.getFlags().isSilenced());
            assertFalse(buff.getFlags().isTaunted());
        }
    }
}
