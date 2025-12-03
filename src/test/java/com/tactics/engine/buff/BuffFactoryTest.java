package com.tactics.engine.buff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V3 BuffFactory Tests (BM-Series from BUFF_SYSTEM_V3_TESTPLAN.md)
 *
 * Tests that BuffFactory creates correct buff instances with:
 * - Correct modifiers (ATK, HP)
 * - Correct flags (powerBuff, speedBuff, slowBuff, bleedBuff)
 * - Correct duration (2 rounds)
 * - Correct instant HP bonus
 */
@DisplayName("V3 BuffFactory Tests")
class BuffFactoryTest {

    private static final String SOURCE_ID = "test_source";

    @Nested
    @DisplayName("BM-Series: Buff Model Correctness")
    class BuffModelCorrectness {

        @Test
        @DisplayName("BM1: BuffType enum contains all 6 types")
        void buffTypeContainsAll6Types() {
            BuffType[] types = BuffType.values();
            assertEquals(6, types.length, "BuffType should have 6 values");

            assertNotNull(BuffType.POWER);
            assertNotNull(BuffType.LIFE);
            assertNotNull(BuffType.SPEED);
            assertNotNull(BuffType.WEAKNESS);
            assertNotNull(BuffType.BLEED);
            assertNotNull(BuffType.SLOW);
        }

        @Test
        @DisplayName("BM5: POWER buff has correct default modifiers")
        void powerBuffHasCorrectModifiers() {
            BuffInstance power = BuffFactory.createPower(SOURCE_ID);

            assertEquals(BuffType.POWER, power.getType(), "Type should be POWER");
            assertEquals(3, power.getModifiers().getBonusAttack(), "POWER should grant +3 ATK");
            assertEquals(1, power.getInstantHpBonus(), "POWER should grant +1 instant HP");
            assertTrue(power.getFlags().isPowerBuff(), "POWER flag should be set");
            assertEquals(2, power.getDuration(), "Duration should be 2");
        }

        @Test
        @DisplayName("BM6: LIFE buff has correct default modifiers")
        void lifeBuffHasCorrectModifiers() {
            BuffInstance life = BuffFactory.createLife(SOURCE_ID);

            assertEquals(BuffType.LIFE, life.getType(), "Type should be LIFE");
            assertEquals(0, life.getModifiers().getBonusAttack(), "LIFE should not affect ATK");
            assertEquals(3, life.getInstantHpBonus(), "LIFE should grant +3 instant HP");
            assertEquals(2, life.getDuration(), "Duration should be 2");
        }

        @Test
        @DisplayName("BM7: SPEED buff has correct default modifiers")
        void speedBuffHasCorrectModifiers() {
            BuffInstance speed = BuffFactory.createSpeed(SOURCE_ID);

            assertEquals(BuffType.SPEED, speed.getType(), "Type should be SPEED");
            assertEquals(-1, speed.getModifiers().getBonusAttack(), "SPEED should grant -1 ATK");
            assertEquals(0, speed.getInstantHpBonus(), "SPEED should not affect instant HP");
            assertTrue(speed.getFlags().isSpeedBuff(), "SPEED flag should be set");
            assertEquals(2, speed.getDuration(), "Duration should be 2");
        }

        @Test
        @DisplayName("BM8: WEAKNESS buff has correct default modifiers")
        void weaknessBuffHasCorrectModifiers() {
            BuffInstance weakness = BuffFactory.createWeakness(SOURCE_ID);

            assertEquals(BuffType.WEAKNESS, weakness.getType(), "Type should be WEAKNESS");
            assertEquals(-2, weakness.getModifiers().getBonusAttack(), "WEAKNESS should grant -2 ATK");
            assertEquals(-1, weakness.getInstantHpBonus(), "WEAKNESS should grant -1 instant HP");
            assertEquals(2, weakness.getDuration(), "Duration should be 2");
        }

        @Test
        @DisplayName("BM9: BLEED buff has correct default flags")
        void bleedBuffHasCorrectFlags() {
            BuffInstance bleed = BuffFactory.createBleed(SOURCE_ID);

            assertEquals(BuffType.BLEED, bleed.getType(), "Type should be BLEED");
            assertTrue(bleed.getFlags().isBleedBuff(), "BLEED flag should be set");
            assertEquals(0, bleed.getModifiers().getBonusAttack(), "BLEED should not affect ATK");
            assertEquals(0, bleed.getInstantHpBonus(), "BLEED should not affect instant HP");
            assertEquals(2, bleed.getDuration(), "Duration should be 2");
        }

        @Test
        @DisplayName("BM10: SLOW buff has correct default flags")
        void slowBuffHasCorrectFlags() {
            BuffInstance slow = BuffFactory.createSlow(SOURCE_ID);

            assertEquals(BuffType.SLOW, slow.getType(), "Type should be SLOW");
            assertTrue(slow.getFlags().isSlowBuff(), "SLOW flag should be set");
            assertEquals(0, slow.getModifiers().getBonusAttack(), "SLOW should not affect ATK");
            assertEquals(0, slow.getInstantHpBonus(), "SLOW should not affect instant HP");
            assertEquals(2, slow.getDuration(), "Duration should be 2");
        }

        @Test
        @DisplayName("BM11: All buffs have duration = 2 by default")
        void allBuffsHaveDuration2() {
            for (BuffType type : BuffType.values()) {
                BuffInstance buff = BuffFactory.create(type, SOURCE_ID);
                assertEquals(2, buff.getDuration(),
                    String.format("Buff type %s should have duration 2", type));
            }
        }

        @Test
        @DisplayName("BM12: BuffInstance is immutable - withDecreasedDuration returns new instance")
        void buffInstanceIsImmutable() {
            BuffInstance original = BuffFactory.createPower(SOURCE_ID);
            BuffInstance decreased = original.withDecreasedDuration();

            assertEquals(2, original.getDuration(), "Original duration should be unchanged");
            assertEquals(1, decreased.getDuration(), "Decreased duration should be 1");
            assertNotSame(original, decreased, "Should be different instances");
        }

        @Test
        @DisplayName("BuffFactory.create works for all types")
        void factoryCreateWorksForAllTypes() {
            for (BuffType type : BuffType.values()) {
                BuffInstance buff = BuffFactory.create(type, SOURCE_ID);
                assertNotNull(buff, String.format("BuffFactory.create should work for %s", type));
                assertEquals(type, buff.getType(), String.format("Type should match for %s", type));
            }
        }
    }

    @Nested
    @DisplayName("Buff Expiration Tests")
    class BuffExpiration {

        @Test
        @DisplayName("Buff with duration 0 is expired")
        void buffWithDuration0IsExpired() {
            BuffInstance buff = BuffFactory.createPower(SOURCE_ID);
            BuffInstance expired = buff.withDuration(0);

            assertTrue(expired.isExpired(), "Buff with duration 0 should be expired");
        }

        @Test
        @DisplayName("Buff with duration > 0 is not expired")
        void buffWithPositiveDurationIsNotExpired() {
            BuffInstance buff = BuffFactory.createPower(SOURCE_ID);

            assertFalse(buff.isExpired(), "Buff with duration 2 should not be expired");

            BuffInstance decreased = buff.withDecreasedDuration();
            assertFalse(decreased.isExpired(), "Buff with duration 1 should not be expired");
        }
    }
}
