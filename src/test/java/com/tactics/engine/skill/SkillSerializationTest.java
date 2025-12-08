package com.tactics.engine.skill;

import com.tactics.engine.model.*;
import com.tactics.engine.util.GameStateSerializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SS-Series: Skill Serialization Tests (from SKILL_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for skill-related serialization:
 * - Hero selectedSkillId serializes
 * - Hero skillCooldown serializes
 * - SkillState serializes
 * - Roundtrip preserves skill state
 */
@DisplayName("SS-Series: Skill Serialization Tests")
public class SkillSerializationTest {

    private GameStateSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new GameStateSerializer();
    }

    // ========== Helper Methods ==========

    private Unit createHero(String id, PlayerId owner, int hp, Position pos,
                           HeroClass heroClass, String skillId, int cooldown) {
        return new Unit(id, owner, hp, 3, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, cooldown,
            0, false, false, false, 0, null,
            0, false, null, 0, 0);
    }

    private Unit createHeroWithSkillState(String id, PlayerId owner, int hp, Position pos,
                                          HeroClass heroClass, String skillId, int cooldown,
                                          Map<String, Object> skillState) {
        return new Unit(id, owner, hp, 3, 2, 1, pos, true,
            UnitCategory.HERO, null, heroClass, hp,
            skillId, cooldown,
            0, false, false, false, 0, skillState,  // skillState is after temporaryDuration
            0, false, null, 0, 0);  // preparingAction is null
    }

    private Unit createMinion(String id, PlayerId owner, int hp, Position pos, MinionType type) {
        return new Unit(id, owner, hp, 2, 2, 1, pos, true,
            UnitCategory.MINION, type, null, hp,
            null, 0,
            0, false, false, false, 0, null,
            0, false, null, 0, 0);
    }

    private GameState createGameState(List<Unit> units, PlayerId currentPlayer) {
        return new GameState(
            new Board(5, 5),
            units,
            currentPlayer,
            false, null,
            new HashMap<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            1, null
        );
    }

    // ========== SS1: Hero selectedSkillId serializes ==========

    @Nested
    @DisplayName("SS1-SS2: Hero Skill Fields Serialization")
    class HeroSkillFieldsSerializationTests {

        @Test
        @DisplayName("SS1: Hero selectedSkillId serializes correctly")
        void heroSelectedSkillIdSerializes() {
            // Given: Hero with selected skill
            Unit hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP, 0);
            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Serialize to JSON map
            Map<String, Object> json = serializer.toJsonMap(state);

            // Then: Find hero unit and verify selectedSkillId
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> units = (List<Map<String, Object>>) json.get("units");
            Map<String, Object> heroJson = units.stream()
                .filter(u -> "p1_hero".equals(u.get("id")))
                .findFirst()
                .orElse(null);

            assertNotNull(heroJson, "Hero should be serialized");
            assertEquals(SkillRegistry.WARRIOR_HEROIC_LEAP, heroJson.get("selectedSkillId"),
                "selectedSkillId should be serialized correctly");
        }

        @Test
        @DisplayName("SS2: Hero skillCooldown serializes correctly")
        void heroSkillCooldownSerializes() {
            // Given: Hero with skill on cooldown
            Unit hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(2, 2),
                HeroClass.MAGE, SkillRegistry.MAGE_ELEMENTAL_BLAST, 2);  // cooldown = 2
            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.TANK);

            GameState state = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Serialize
            Map<String, Object> json = serializer.toJsonMap(state);

            // Then: Verify cooldown
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> units = (List<Map<String, Object>>) json.get("units");
            Map<String, Object> heroJson = units.stream()
                .filter(u -> "p1_hero".equals(u.get("id")))
                .findFirst()
                .orElse(null);

            assertNotNull(heroJson, "Hero should be serialized");
            assertEquals(2, heroJson.get("skillCooldown"),
                "skillCooldown should be serialized correctly");
        }
    }

    // ========== SS3: Deserialization reconstructs hero skill state ==========

    @Nested
    @DisplayName("SS3: Skill State Roundtrip Tests")
    class SkillStateRoundtripTests {

        @Test
        @DisplayName("SS3: Deserialization reconstructs hero skill state")
        void deserializationReconstructsHeroSkillState() {
            // Given: Hero with skill configuration
            Unit hero = createHero("p1_hero", PlayerId.PLAYER_1, 10, new Position(2, 2),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP, 2);
            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.ARCHER);

            GameState original = createGameState(Arrays.asList(hero, enemy), PlayerId.PLAYER_1);

            // When: Serialize then deserialize
            Map<String, Object> json = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(json);

            // Then: Hero skill state should be preserved
            Unit heroReconstructed = reconstructed.getUnits().stream()
                .filter(u -> u.getId().equals("p1_hero"))
                .findFirst()
                .orElse(null);

            assertNotNull(heroReconstructed, "Hero should be reconstructed");
            assertEquals(SkillRegistry.WARRIOR_HEROIC_LEAP, heroReconstructed.getSelectedSkillId(),
                "selectedSkillId should be preserved after roundtrip");
            assertEquals(2, heroReconstructed.getSkillCooldown(),
                "skillCooldown should be preserved after roundtrip");
        }

        @Test
        @DisplayName("SS8: Full roundtrip preserves all skill state")
        void fullRoundtripPreservesAllSkillState() {
            // Given: Multiple heroes with different skill states
            Unit warrior = createHero("p1_warrior", PlayerId.PLAYER_1, 10, new Position(1, 1),
                HeroClass.WARRIOR, SkillRegistry.WARRIOR_SHOCKWAVE, 1);
            Unit mage = createHero("p1_mage", PlayerId.PLAYER_1, 10, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WILD_MAGIC, 0);
            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.TANK);

            GameState original = createGameState(Arrays.asList(warrior, mage, enemy), PlayerId.PLAYER_1);

            // When: Roundtrip
            Map<String, Object> json = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(json);

            // Then: All skill states preserved
            Unit warriorAfter = reconstructed.getUnits().stream()
                .filter(u -> u.getId().equals("p1_warrior"))
                .findFirst().orElseThrow();
            Unit mageAfter = reconstructed.getUnits().stream()
                .filter(u -> u.getId().equals("p1_mage"))
                .findFirst().orElseThrow();

            assertEquals(SkillRegistry.WARRIOR_SHOCKWAVE, warriorAfter.getSelectedSkillId());
            assertEquals(1, warriorAfter.getSkillCooldown());
            assertEquals(SkillRegistry.MAGE_WILD_MAGIC, mageAfter.getSelectedSkillId());
            assertEquals(0, mageAfter.getSkillCooldown());
        }
    }

    // ========== SS4: Null selectedSkillId serializes as null ==========

    @Nested
    @DisplayName("SS4: Null Skill Handling Tests")
    class NullSkillHandlingTests {

        @Test
        @DisplayName("SS4: Null selectedSkillId serializes as null")
        void nullSelectedSkillIdSerializesAsNull() {
            // Given: Hero with no selected skill (minion doesn't have skills)
            Unit minion = createMinion("p1_minion", PlayerId.PLAYER_1, 10, new Position(2, 2),
                MinionType.TANK);
            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(minion, enemy), PlayerId.PLAYER_1);

            // When: Serialize
            Map<String, Object> json = serializer.toJsonMap(state);

            // Then: selectedSkillId should be null
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> units = (List<Map<String, Object>>) json.get("units");
            Map<String, Object> minionJson = units.stream()
                .filter(u -> "p1_minion".equals(u.get("id")))
                .findFirst()
                .orElse(null);

            assertNotNull(minionJson, "Minion should be serialized");
            assertNull(minionJson.get("selectedSkillId"),
                "Minion selectedSkillId should be null");
        }
    }

    // ========== SS5: SkillState (Warp Beacon) serializes ==========

    @Nested
    @DisplayName("SS5: SkillState Serialization Tests")
    class SkillStateSerializationTests {

        @Test
        @DisplayName("SS5: SkillState (beacon_position) serializes")
        void skillStateSerializes() {
            // Given: Mage with Warp Beacon placed at (3, 3)
            Map<String, Object> skillState = new HashMap<>();
            skillState.put("beacon_x", 3);
            skillState.put("beacon_y", 3);

            Unit mage = createHeroWithSkillState("p1_mage", PlayerId.PLAYER_1, 10, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON, 0, skillState);
            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(mage, enemy), PlayerId.PLAYER_1);

            // When: Serialize
            Map<String, Object> json = serializer.toJsonMap(state);

            // Then: Skill state should be preserved
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> units = (List<Map<String, Object>>) json.get("units");
            Map<String, Object> mageJson = units.stream()
                .filter(u -> "p1_mage".equals(u.get("id")))
                .findFirst()
                .orElse(null);

            assertNotNull(mageJson, "Mage should be serialized");
            @SuppressWarnings("unchecked")
            Map<String, Object> serializedSkillState = (Map<String, Object>) mageJson.get("skillState");
            assertNotNull(serializedSkillState, "SkillState should be serialized");
            assertEquals(3, serializedSkillState.get("beacon_x"), "beacon_x should be preserved");
            assertEquals(3, serializedSkillState.get("beacon_y"), "beacon_y should be preserved");
        }

        @Test
        @DisplayName("SS5b: SkillState roundtrip preserves beacon position")
        void skillStateRoundtripPreservesBeacon() {
            // Given: Mage with Warp Beacon
            Map<String, Object> skillState = new HashMap<>();
            skillState.put("beacon_x", 3);
            skillState.put("beacon_y", 3);

            Unit mage = createHeroWithSkillState("p1_mage", PlayerId.PLAYER_1, 10, new Position(0, 0),
                HeroClass.MAGE, SkillRegistry.MAGE_WARP_BEACON, 0, skillState);
            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.ARCHER);

            GameState original = createGameState(Arrays.asList(mage, enemy), PlayerId.PLAYER_1);

            // When: Roundtrip
            Map<String, Object> json = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(json);

            // Then: Beacon position preserved
            Unit mageAfter = reconstructed.getUnits().stream()
                .filter(u -> u.getId().equals("p1_mage"))
                .findFirst().orElseThrow();

            assertNotNull(mageAfter.getSkillState(), "SkillState should be reconstructed");
            assertEquals(3, mageAfter.getSkillState().get("beacon_x"));
            assertEquals(3, mageAfter.getSkillState().get("beacon_y"));
        }
    }

    // ========== SS6: Shadow Clone temporary unit serializes ==========

    @Nested
    @DisplayName("SS6: Temporary Unit Serialization Tests")
    class TemporaryUnitSerializationTests {

        @Test
        @DisplayName("SS6: Shadow Clone unit serializes as temporary")
        void shadowCloneSerializesAsTemporary() {
            // Given: A Shadow Clone (temporary unit)
            // Constructor: shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
            //              actionsUsed, preparing, preparingAction, bonusAttackDamage, bonusAttackCharges
            Unit clone = new Unit("p1_rogue_clone_123", PlayerId.PLAYER_1, 1, 1, 2, 1,
                new Position(2, 3), true,
                UnitCategory.MINION, MinionType.ASSASSIN, null, 1,
                null, 0,
                0, false, false, true, 2, null,  // isTemporary=true, temporaryDuration=2
                0, false, null, 0, 0);

            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.ARCHER);

            GameState state = createGameState(Arrays.asList(clone, enemy), PlayerId.PLAYER_1);

            // When: Serialize
            Map<String, Object> json = serializer.toJsonMap(state);

            // Then: Clone should be marked as temporary
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> units = (List<Map<String, Object>>) json.get("units");
            Map<String, Object> cloneJson = units.stream()
                .filter(u -> ((String) u.get("id")).startsWith("p1_rogue_clone"))
                .findFirst()
                .orElse(null);

            assertNotNull(cloneJson, "Clone should be serialized");
            assertEquals(true, cloneJson.get("isTemporary"), "Clone should be marked as temporary");
            assertEquals(2, cloneJson.get("temporaryDuration"), "Clone duration should be 2");
        }

        @Test
        @DisplayName("SS6b: Temporary unit roundtrip preserves temporary flag")
        void temporaryUnitRoundtripPreservesFlag() {
            // Given: Shadow Clone
            // Constructor: shield, invisible, invulnerable, isTemporary, temporaryDuration, skillState,
            //              actionsUsed, preparing, preparingAction, bonusAttackDamage, bonusAttackCharges
            Unit clone = new Unit("p1_rogue_clone_123", PlayerId.PLAYER_1, 1, 1, 2, 1,
                new Position(2, 3), true,
                UnitCategory.MINION, MinionType.ASSASSIN, null, 1,
                null, 0,
                0, false, false, true, 2, null,  // isTemporary=true, temporaryDuration=2
                0, false, null, 0, 0);

            Unit enemy = createMinion("p2_minion", PlayerId.PLAYER_2, 10, new Position(4, 4),
                MinionType.ARCHER);

            GameState original = createGameState(Arrays.asList(clone, enemy), PlayerId.PLAYER_1);

            // When: Roundtrip
            Map<String, Object> json = serializer.toJsonMap(original);
            GameState reconstructed = serializer.fromJsonMap(json);

            // Then: Temporary flag preserved
            Unit cloneAfter = reconstructed.getUnits().stream()
                .filter(u -> u.getId().startsWith("p1_rogue_clone"))
                .findFirst().orElseThrow();

            assertTrue(cloneAfter.isTemporary(), "Clone should still be temporary after roundtrip");
            assertEquals(2, cloneAfter.getTemporaryDuration(), "Duration should be preserved");
        }
    }
}
