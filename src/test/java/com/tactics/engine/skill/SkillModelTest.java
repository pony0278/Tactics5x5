package com.tactics.engine.skill;

import com.tactics.engine.model.HeroClass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SM-Series: Skill Model Tests (from SKILL_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests for skill data structures:
 * - HeroClass enum (6 values)
 * - SkillDefinition fields
 * - TargetType enum
 * - SkillEffect enum
 * - SkillRegistry content
 */
@DisplayName("SM-Series: Skill Model Tests")
public class SkillModelTest {

    // ========== SM1: HeroClass enum ==========

    @Nested
    @DisplayName("SM1: HeroClass Enum Tests")
    class HeroClassEnumTests {

        @Test
        @DisplayName("SM1: HeroClass has exactly 6 values")
        void heroClassHasSixValues() {
            // Given: HeroClass enum
            // Then: Should have exactly 6 values
            assertEquals(6, HeroClass.values().length,
                "HeroClass should have exactly 6 values");
        }

        @Test
        @DisplayName("SM1b: HeroClass contains all expected classes")
        void heroClassContainsExpectedClasses() {
            // Given: Expected hero classes
            Set<String> expected = Set.of("WARRIOR", "MAGE", "ROGUE", "HUNTRESS", "DUELIST", "CLERIC");

            // When: Get actual values
            Set<String> actual = new java.util.HashSet<>();
            for (HeroClass hc : HeroClass.values()) {
                actual.add(hc.name());
            }

            // Then: Should match
            assertEquals(expected, actual,
                "HeroClass should contain WARRIOR, MAGE, ROGUE, HUNTRESS, DUELIST, CLERIC");
        }
    }

    // ========== SM2: Each HeroClass has 3 skills ==========

    @Nested
    @DisplayName("SM2: HeroClass Skill Count Tests")
    class HeroClassSkillCountTests {

        @Test
        @DisplayName("SM2: Each HeroClass has exactly 3 skills")
        void eachHeroClassHasThreeSkills() {
            // For each HeroClass
            for (HeroClass heroClass : HeroClass.values()) {
                List<SkillDefinition> skills = SkillRegistry.getSkillsForClass(heroClass);
                assertEquals(3, skills.size(),
                    heroClass.name() + " should have exactly 3 skills, but has " + skills.size());
            }
        }
    }

    // ========== SM3: SkillDefinition stores fields correctly ==========

    @Nested
    @DisplayName("SM3: SkillDefinition Field Tests")
    class SkillDefinitionFieldTests {

        @Test
        @DisplayName("SM3: SkillDefinition stores all fields correctly (Heroic Leap)")
        void skillDefinitionStoresFieldsCorrectly() {
            // Given: Warrior's Heroic Leap skill
            SkillDefinition skill = SkillRegistry.getSkill(SkillRegistry.WARRIOR_HEROIC_LEAP);

            // Then: All fields should be correctly set
            assertNotNull(skill, "Skill should exist");
            assertEquals("warrior_heroic_leap", skill.getSkillId(), "skillId should match");
            assertEquals("Heroic Leap", skill.getName(), "name should match");
            assertEquals(HeroClass.WARRIOR, skill.getHeroClass(), "heroClass should match");
            assertEquals(TargetType.SINGLE_TILE, skill.getTargetType(), "targetType should match");
            assertEquals(3, skill.getRange(), "range should be 3");
            assertEquals(2, skill.getCooldown(), "cooldown should be 2");
        }

        @Test
        @DisplayName("SM3b: SkillDefinition stores effects list")
        void skillDefinitionStoresEffects() {
            // Given: Warrior's Heroic Leap skill
            SkillDefinition skill = SkillRegistry.getSkill(SkillRegistry.WARRIOR_HEROIC_LEAP);

            // Then: Effects list should not be null or empty
            assertNotNull(skill.getEffects(), "Effects should not be null");
            assertFalse(skill.getEffects().isEmpty(), "Effects should not be empty");
            assertTrue(skill.getEffects().contains(SkillEffect.MOVE_SELF),
                "Heroic Leap should include MOVE_SELF effect");
        }
    }

    // ========== SM4: TargetType enum ==========

    @Nested
    @DisplayName("SM4: TargetType Enum Tests")
    class TargetTypeEnumTests {

        @Test
        @DisplayName("SM4: TargetType contains all expected types")
        void targetTypeContainsAllTypes() {
            // Given: Expected target types
            Set<String> expected = Set.of(
                "SELF", "SINGLE_ENEMY", "SINGLE_ALLY", "SINGLE_TILE",
                "AREA_AROUND_SELF", "AREA_AROUND_TARGET", "LINE",
                "ALL_ENEMIES", "ALL_ALLIES"
            );

            // When: Get actual values
            Set<String> actual = new java.util.HashSet<>();
            for (TargetType tt : TargetType.values()) {
                actual.add(tt.name());
            }

            // Then: Should contain all expected types
            assertTrue(actual.containsAll(expected),
                "TargetType should contain all expected types. Missing: " +
                expected.stream().filter(e -> !actual.contains(e)).toList());
        }
    }

    // ========== SM5: SkillEffect enum ==========

    @Nested
    @DisplayName("SM5: SkillEffect Enum Tests")
    class SkillEffectEnumTests {

        @Test
        @DisplayName("SM5: SkillEffect contains core types")
        void skillEffectContainsCoreTypes() {
            // Given: Core expected effect types
            Set<String> expected = Set.of(
                "DAMAGE", "HEAL", "MOVE_SELF", "MOVE_TARGET", "APPLY_BUFF",
                "REMOVE_BUFF", "SPAWN_UNIT", "SPAWN_OBSTACLE", "MARK"
            );

            // When: Get actual values
            Set<String> actual = new java.util.HashSet<>();
            for (SkillEffect se : SkillEffect.values()) {
                actual.add(se.name());
            }

            // Then: Should contain all core types
            assertTrue(actual.containsAll(expected),
                "SkillEffect should contain all core types. Missing: " +
                expected.stream().filter(e -> !actual.contains(e)).toList());
        }
    }

    // ========== SM8: All 18 skills registered ==========

    @Nested
    @DisplayName("SM8: Skill Registry Tests")
    class SkillRegistryTests {

        @Test
        @DisplayName("SM8: All 18 skills are registered")
        void all18SkillsRegistered() {
            // Given: Skill registry
            Set<String> allSkillIds = SkillRegistry.getAllSkillIds();

            // Then: Should have exactly 18 skills (6 classes * 3 skills)
            assertEquals(18, allSkillIds.size(),
                "SkillRegistry should have exactly 18 skills");
        }
    }

    // ========== SM9: Skill lookup by ID ==========

    @Nested
    @DisplayName("SM9-SM10: Skill Lookup Tests")
    class SkillLookupTests {

        @Test
        @DisplayName("SM9: Skill lookup by ID works correctly")
        void skillLookupByIdWorks() {
            // When: Look up skill by ID
            SkillDefinition skill = SkillRegistry.getSkill("rogue_smoke_bomb");

            // Then: Should find the skill with correct name
            assertNotNull(skill, "Should find skill by ID");
            assertEquals("Smoke Bomb", skill.getName(), "Skill name should match");
        }

        @Test
        @DisplayName("SM10: Invalid skill ID returns null")
        void invalidSkillIdReturnsNull() {
            // When: Look up invalid skill ID
            SkillDefinition skill = SkillRegistry.getSkill("invalid_skill_id");

            // Then: Should return null
            assertNull(skill, "Invalid skill ID should return null");
        }
    }

    // ========== SM13: Default cooldown is 2 ==========

    @Nested
    @DisplayName("SM13: Skill Cooldown Tests")
    class SkillCooldownTests {

        @Test
        @DisplayName("SM13: Default cooldown is 2 for all skills")
        void defaultCooldownIs2() {
            // For each skill in registry
            for (String skillId : SkillRegistry.getAllSkillIds()) {
                SkillDefinition skill = SkillRegistry.getSkill(skillId);
                assertEquals(2, skill.getCooldown(),
                    "Skill " + skillId + " should have cooldown of 2");
            }
        }
    }

    // ========== SM14: SkillDefinition is immutable ==========

    @Nested
    @DisplayName("SM14-SM15: Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("SM14: SkillDefinition has no setters")
        void skillDefinitionHasNoSetters() {
            // SkillDefinition is immutable by design - verify via reflection
            java.lang.reflect.Method[] methods = SkillDefinition.class.getMethods();
            for (java.lang.reflect.Method method : methods) {
                assertFalse(method.getName().startsWith("set"),
                    "SkillDefinition should not have setter methods: " + method.getName());
            }
        }

        @Test
        @DisplayName("SM15: Skill effects list is unmodifiable")
        void skillEffectsListIsUnmodifiable() {
            // Given: A skill with effects
            SkillDefinition skill = SkillRegistry.getSkill(SkillRegistry.WARRIOR_SHOCKWAVE);
            List<SkillEffect> effects = skill.getEffects();

            // Then: Attempting to modify should throw exception
            assertThrows(UnsupportedOperationException.class, () -> {
                effects.add(SkillEffect.HEAL);
            }, "Effects list should be unmodifiable");
        }
    }

    // ========== Additional: Skill class mapping tests ==========

    @Nested
    @DisplayName("SM-Extra: Skill Class Mapping Tests")
    class SkillClassMappingTests {

        @Test
        @DisplayName("canClassUseSkill returns true for valid skill")
        void canClassUseSkillReturnsTrueForValid() {
            assertTrue(SkillRegistry.canClassUseSkill(HeroClass.WARRIOR, SkillRegistry.WARRIOR_HEROIC_LEAP),
                "WARRIOR should be able to use warrior_heroic_leap");
        }

        @Test
        @DisplayName("canClassUseSkill returns false for wrong class")
        void canClassUseSkillReturnsFalseForWrongClass() {
            assertFalse(SkillRegistry.canClassUseSkill(HeroClass.MAGE, SkillRegistry.WARRIOR_HEROIC_LEAP),
                "MAGE should NOT be able to use warrior_heroic_leap");
        }

        @Test
        @DisplayName("isValidSkillId returns true for valid ID")
        void isValidSkillIdReturnsTrueForValid() {
            assertTrue(SkillRegistry.isValidSkillId("mage_elemental_blast"),
                "mage_elemental_blast should be a valid skill ID");
        }

        @Test
        @DisplayName("isValidSkillId returns false for invalid ID")
        void isValidSkillIdReturnsFalseForInvalid() {
            assertFalse(SkillRegistry.isValidSkillId("fake_skill"),
                "fake_skill should not be a valid skill ID");
        }
    }
}
