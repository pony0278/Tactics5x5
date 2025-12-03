# SKILL_SYSTEM_V3_TESTPLAN.md — 5×5 Tactics Skill System Test Plan (Version 3)

## 1. Scope

This document defines the test cases required for the Skill System V3.

It covers:

- Skill model correctness (18 skills across 6 classes)
- Skill serialization & deserialization
- Skill cooldown mechanics
- Skill targeting and validation
- Skill execution and effects
- Skill interaction with BUFF system
- Skill interaction with Guardian passive
- Special skill states (Warp Beacon, Shadow Clone, Feint, Challenge)
- Hero class skill restrictions
- Deterministic behavior

Not covered:

- Skill UI/UX
- Skill animations/sounds
- Skill balance tuning
- Future skill additions

---

## 2. Confirmed Design Rules

| Rule | Decision |
|------|----------|
| Timeout penalty target | Hero HP -1 |
| Round 8+ pressure target | All units -1 HP |
| Simultaneous hero death | First to die loses |
| SLOW: Can be attacked while preparing | Yes |
| SLOW: Can cancel declared action | No |
| STUN blocks skill usage | Yes |
| SLOW delays skill by 1 round | Yes |
| Guardian intercepts skill damage | Yes |
| Warp Beacon disappears on Mage death | Yes |
| Warp Beacon visible to enemies | Yes |
| Shadow Clone death triggers death choice | No |
| Shadow Clone can be healed | No |
| Counter-attack damage type | Normal attack (intercepted by Guardian) |
| Counter-attack consumes action | No |

---

## 3. Test Series Overview

| Series | Purpose | Test Count |
|--------|---------|------------|
| SM-Series | Skill model correctness | 15 |
| SS-Series | Skill serialization | 8 |
| SC-Series | Skill cooldown mechanics | 12 |
| SV-Series | Skill validation (general) | 18 |
| SA-Series | Skill apply (general) | 14 |
| SW-Series | WARRIOR skills | 12 |
| SMG-Series | MAGE skills | 14 |
| SR-Series | ROGUE skills | 14 |
| SH-Series | HUNTRESS skills | 12 |
| SD-Series | DUELIST skills | 14 |
| SCL-Series | CLERIC skills | 12 |
| SB-Series | Skill + BUFF interaction | 16 |
| SG-Series | Skill + Guardian interaction | 10 |
| SSP-Series | Special skill states | 18 |
| SBC-Series | Backward compatibility | 6 |
| SDT-Series | Deterministic ordering | 6 |
| **Total** | | **201** |

---

## 4. Test Class Structure

### 4.1 Recommended File Layout

```
src/test/java/com/tactics/engine/
├── skill/
│   ├── SkillModelTest.java              # SM-Series
│   ├── SkillSerializerTest.java         # SS-Series
│   ├── SkillCooldownTest.java           # SC-Series
│   └── SkillDefinitionTest.java         # Skill data validation
├── rules/
│   ├── RuleEngineSkillValidationTest.java   # SV-Series
│   ├── RuleEngineSkillApplyTest.java        # SA-Series
│   ├── RuleEngineWarriorSkillTest.java      # SW-Series
│   ├── RuleEngineMageSkillTest.java         # SMG-Series
│   ├── RuleEngineRogueSkillTest.java        # SR-Series
│   ├── RuleEngineHuntressSkillTest.java     # SH-Series
│   ├── RuleEngineDuelistSkillTest.java      # SD-Series
│   ├── RuleEngineClericSkillTest.java       # SCL-Series
│   ├── RuleEngineSkillBuffInteractionTest.java  # SB-Series
│   ├── RuleEngineSkillGuardianTest.java     # SG-Series
│   ├── RuleEngineSpecialSkillStateTest.java # SSP-Series
│   └── RuleEngineSkillCompatibilityTest.java # SBC-Series
```

### 4.2 JUnit 5 Structure Example

```java
@DisplayName("Skill System V3 - Warrior Skills")
class RuleEngineWarriorSkillTest {
    
    @Nested
    @DisplayName("SW-Series: Heroic Leap")
    class HeroicLeapTests {
        @Test @DisplayName("SW1: Heroic Leap moves hero to target tile")
        void heroicLeapMovesToTarget() { ... }
    }
}
```

---

## 5. Test Series (Detailed)

---

### SM-Series — Skill Model Tests

#### SM1 — HeroClass enum contains all 6 classes

```java
assertThat(HeroClass.values()).containsExactlyInAnyOrder(
    WARRIOR, MAGE, ROGUE, HUNTRESS, DUELIST, CLERIC
);
```

#### SM2 — Each HeroClass has exactly 3 skills

```java
for (HeroClass heroClass : HeroClass.values()) {
    List<SkillDefinition> skills = SkillRegistry.getSkillsForClass(heroClass);
    assertThat(skills).hasSize(3);
}
```

#### SM3 — SkillDefinition stores all fields correctly

```java
SkillDefinition skill = SkillRegistry.get("warrior_heroic_leap");
assertThat(skill.getId()).isEqualTo("warrior_heroic_leap");
assertThat(skill.getName()).isEqualTo("Heroic Leap");
assertThat(skill.getHeroClass()).isEqualTo(HeroClass.WARRIOR);
assertThat(skill.getTargetType()).isEqualTo(TargetType.SINGLE_TILE);
assertThat(skill.getRange()).isEqualTo(3);
assertThat(skill.getCooldown()).isEqualTo(2);
```

#### SM4 — TargetType enum contains all types

```java
assertThat(TargetType.values()).containsExactlyInAnyOrder(
    SELF, SINGLE_ENEMY, SINGLE_ALLY, SINGLE_TILE,
    AREA_AROUND_SELF, AREA_AROUND_TARGET, LINE,
    ALL_ENEMIES, ALL_ALLIES
);
```

#### SM5 — EffectType enum contains all types

```java
assertThat(EffectType.values()).containsExactlyInAnyOrder(
    DAMAGE, HEAL, MOVE_SELF, MOVE_TARGET, APPLY_BUFF,
    REMOVE_BUFF, SPAWN_UNIT, SPAWN_OBSTACLE, STUN, MARK
);
```

#### SM6 — SkillEffect stores amount correctly

```java
SkillEffect effect = new SkillEffect(EffectType.DAMAGE, 3);
assertThat(effect.getType()).isEqualTo(EffectType.DAMAGE);
assertThat(effect.getAmount()).isEqualTo(3);
```

#### SM7 — SkillEffect stores buff type for APPLY_BUFF

```java
SkillEffect effect = SkillEffect.applyBuff(BuffType.SLOW, 2);
assertThat(effect.getBuffType()).isEqualTo(BuffType.SLOW);
assertThat(effect.getDuration()).isEqualTo(2);
```

#### SM8 — All 18 skills registered in SkillRegistry

```java
assertThat(SkillRegistry.getAllSkills()).hasSize(18);
```

#### SM9 — Skill lookup by ID works correctly

```java
SkillDefinition skill = SkillRegistry.get("rogue_smoke_bomb");
assertThat(skill).isNotNull();
assertThat(skill.getName()).isEqualTo("Smoke Bomb");
```

#### SM10 — Invalid skill ID returns null

```java
SkillDefinition skill = SkillRegistry.get("invalid_skill_id");
assertThat(skill).isNull();
```

#### SM11 — Hero stores selectedSkillId

```java
Hero hero = createHero(HeroClass.WARRIOR);
hero.setSelectedSkillId("warrior_heroic_leap");
assertThat(hero.getSelectedSkillId()).isEqualTo("warrior_heroic_leap");
```

#### SM12 — Hero stores skillCooldown

```java
Hero hero = createHero(HeroClass.WARRIOR);
hero.setSkillCooldown(2);
assertThat(hero.getSkillCooldown()).isEqualTo(2);
```

#### SM13 — Default cooldown is 2 for all skills

```java
for (SkillDefinition skill : SkillRegistry.getAllSkills()) {
    assertThat(skill.getCooldown()).isEqualTo(2);
}
```

#### SM14 — SkillDefinition is immutable

```java
SkillDefinition original = SkillRegistry.get("warrior_heroic_leap");
// No setter methods should exist
// Attempting to modify should fail at compile time
```

#### SM15 — Skill effects list is immutable

```java
SkillDefinition skill = SkillRegistry.get("warrior_shockwave");
List<SkillEffect> effects = skill.getEffects();
assertThatThrownBy(() -> effects.add(new SkillEffect(...)))
    .isInstanceOf(UnsupportedOperationException.class);
```

---

### SS-Series — Skill Serialization Tests

#### SS1 — Hero selectedSkillId serializes correctly

```java
Map<String, Object> json = serializer.toJsonMap(state);
Map<String, Object> hero = getHeroFromJson(json);
assertThat(hero.get("selectedSkillId")).isEqualTo("warrior_heroic_leap");
```

#### SS2 — Hero skillCooldown serializes correctly

```java
assertThat(hero.get("skillCooldown")).isEqualTo(2);
```

#### SS3 — Deserialization reconstructs hero skill state

```java
GameState original = createStateWithHeroSkill();
String json = serializer.toJson(original);
GameState reconstructed = serializer.fromJson(json);
Hero hero = reconstructed.getHero(PLAYER_1);
assertThat(hero.getSelectedSkillId()).isEqualTo("warrior_heroic_leap");
assertThat(hero.getSkillCooldown()).isEqualTo(2);
```

#### SS4 — Null selectedSkillId serializes as null

#### SS5 — SkillState (e.g., Warp Beacon position) serializes

```java
// Mage has placed a beacon at (3, 3)
Map<String, Object> skillState = hero.getSkillState();
assertThat(skillState.get("beacon_position")).isEqualTo(Map.of("x", 3, "y", 3));
```

#### SS6 — Shadow Clone unit serializes as temporary unit

```java
// Clone should be marked as temporary/summon
Unit clone = state.getUnitById("clone_001");
assertThat(clone.isTemporary()).isTrue();
```

#### SS7 — Preparing skill action serializes (for SLOW)

```java
// Hero with SLOW has declared a skill
assertThat(hero.getPreparingAction().getType()).isEqualTo(USE_SKILL);
```

#### SS8 — Roundtrip preserves all skill state

---

### SC-Series — Skill Cooldown Tests

#### SC1 — Skill usable when cooldown = 0

```java
hero.setSkillCooldown(0);
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isTrue();
```

#### SC2 — Skill NOT usable when cooldown > 0

```java
hero.setSkillCooldown(1);
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("cooldown");
```

#### SC3 — Using skill sets cooldown to 2

```java
hero.setSkillCooldown(0);
applyAction(useSkill);
assertThat(hero.getSkillCooldown()).isEqualTo(2);
```

#### SC4 — Cooldown decrements at round end

```java
hero.setSkillCooldown(2);
processRoundEnd();
assertThat(hero.getSkillCooldown()).isEqualTo(1);
```

#### SC5 — Cooldown does not go below 0

```java
hero.setSkillCooldown(0);
processRoundEnd();
assertThat(hero.getSkillCooldown()).isEqualTo(0);
```

#### SC6 — Cooldown tracked per hero independently

```java
hero1.setSkillCooldown(2);
hero2.setSkillCooldown(1);
processRoundEnd();
assertThat(hero1.getSkillCooldown()).isEqualTo(1);
assertThat(hero2.getSkillCooldown()).isEqualTo(0);
```

#### SC7 — Dead hero cooldown still decrements (for respawn scenarios)

```java
// If respawn is implemented in future
```

#### SC8 — Skill cooldown reset on new match

```java
GameState newMatch = GameStateFactory.createNewMatch(...);
Hero hero = newMatch.getHero(PLAYER_1);
assertThat(hero.getSkillCooldown()).isEqualTo(0);
```

#### SC9 — SLOW buff: Skill delayed but cooldown starts immediately

```java
applyBuff(hero, BuffType.SLOW);
applyAction(useSkill);  // Skill enters preparing
assertThat(hero.getSkillCooldown()).isEqualTo(2);  // Cooldown already set
```

#### SC10 — SLOW buff: Delayed skill cancelled doesn't refund cooldown

```java
applyBuff(hero, BuffType.SLOW);
applyAction(useSkill);  // preparing
killUnit(hero);
// Cooldown is NOT refunded
```

#### SC11 — Warp Beacon: First use (place) does NOT trigger cooldown

```java
hero.setSelectedSkillId("mage_warp_beacon");
applyAction(useSkillToPlaceBeacon);
assertThat(hero.getSkillCooldown()).isEqualTo(0);  // No cooldown yet
```

#### SC12 — Warp Beacon: Second use (teleport) triggers cooldown

```java
applyAction(useSkillToTeleport);
assertThat(hero.getSkillCooldown()).isEqualTo(2);
```

---

### SV-Series — Skill Validation (General)

#### SV1 — USE_SKILL requires hero unit

```java
// Try to use skill with minion
Action action = Action.useSkill(minionId, targetPosition);
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("only heroes");
```

#### SV2 — USE_SKILL requires selectedSkillId not null

```java
hero.setSelectedSkillId(null);
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("no skill selected");
```

#### SV3 — USE_SKILL requires cooldown = 0

Verified by SC2.

#### SV4 — USE_SKILL requires hero alive

```java
killUnit(hero);
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isFalse();
```

#### SV5 — USE_SKILL requires correct player's turn

```java
// It's PLAYER_1's turn
Action action = Action.useSkill(player2HeroId, target);
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
```

#### SV6 — USE_SKILL checks target in range

```java
// Heroic Leap range = 3
hero.setPosition(pos(0, 0));
Action action = Action.useSkill(heroId, pos(4, 4));  // Distance = 8
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("out of range");
```

#### SV7 — SINGLE_TILE target must be empty

```java
// Heroic Leap targets occupied tile
placeUnit(otherUnit, pos(2, 2));
Action action = Action.useSkill(heroId, pos(2, 2));
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("occupied");
```

#### SV8 — SINGLE_ENEMY target must be enemy

```java
// Elemental Blast targets friendly
Action action = Action.useSkill(heroId, friendlyUnit.getPosition(), friendlyUnitId);
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("must target enemy");
```

#### SV9 — SINGLE_ENEMY target must be alive

```java
killUnit(enemyUnit);
Action action = Action.useSkill(heroId, enemyUnit.getPosition(), enemyUnitId);
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("target is dead");
```

#### SV10 — SINGLE_ALLY target must be friendly

```java
// Trinity targets enemy
Action action = Action.useSkill(clericId, enemyPos, enemyId);
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("must target ally");
```

#### SV11 — SELF target ignores targetPosition

```java
// Endure is SELF target
Action action = Action.useSkill(warriorId, null, null);
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isTrue();
```

#### SV12 — LINE target must be orthogonal direction

```java
// Spectral Blades must be horizontal or vertical
hero.setPosition(pos(2, 2));
Action action = Action.useSkill(huntressId, pos(3, 3));  // Diagonal
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("orthogonal");
```

#### SV13 — Hero can only use skills from their class

```java
// Warrior tries to use Mage skill
hero.setSelectedSkillId("mage_elemental_blast");
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("wrong class");
```

#### SV14 — STUN prevents USE_SKILL

```java
applyBuff(hero, BuffType.STUN);
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("stunned");
```

#### SV15 — ROOT does NOT prevent USE_SKILL (unless skill involves movement)

```java
applyBuff(hero, BuffType.ROOT);
hero.setSelectedSkillId("mage_elemental_blast");  // No movement
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isTrue();
```

#### SV16 — ROOT prevents Heroic Leap

```java
applyBuff(hero, BuffType.ROOT);
hero.setSelectedSkillId("warrior_heroic_leap");  // Movement skill
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("rooted");
```

#### SV17 — SLOW allows skill declaration (enters preparing)

```java
applyBuff(hero, BuffType.SLOW);
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isTrue();
// But skill doesn't execute immediately
```

#### SV18 — Game over prevents USE_SKILL

```java
state.setGameOver(true);
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isFalse();
```

---

### SA-Series — Skill Apply (General)

#### SA1 — DAMAGE effect reduces target HP

```java
int originalHp = target.getHp();
applyAction(elementalBlast);  // 3 damage
assertThat(target.getHp()).isEqualTo(originalHp - 3);
```

#### SA2 — DAMAGE can kill target

```java
target.setHp(2);
applyAction(elementalBlast);  // 3 damage
assertThat(target.isAlive()).isFalse();
```

#### SA3 — HEAL effect increases target HP

```java
int originalHp = ally.getHp();
applyAction(trinity);  // 3 heal
assertThat(ally.getHp()).isEqualTo(originalHp + 3);
```

#### SA4 — HEAL can exceed maxHp

```java
ally.setHp(5);  // maxHp = 5
applyAction(trinity);  // +3 heal + LIFE buff (+3)
assertThat(ally.getHp()).isEqualTo(11);  // 5 + 3 + 3
```

#### SA5 — MOVE_SELF updates hero position

```java
hero.setPosition(pos(0, 0));
applyAction(heroicLeap, pos(2, 2));
assertThat(hero.getPosition()).isEqualTo(pos(2, 2));
```

#### SA6 — MOVE_TARGET pushes enemy

```java
enemy.setPosition(pos(2, 2));
hero.setPosition(pos(2, 1));
applyAction(shockwave);  // Pushes adjacent enemies
assertThat(enemy.getPosition()).isEqualTo(pos(2, 3));  // Pushed away
```

#### SA7 — APPLY_BUFF adds buff to target

```java
applyAction(elementalStrike);  // Applies chosen debuff
assertThat(target.hasBuff(BuffType.SLOW)).isTrue();
```

#### SA8 — SPAWN_UNIT creates temporary unit

```java
applyAction(shadowClone);
Unit clone = state.getUnitById("clone_...");
assertThat(clone).isNotNull();
assertThat(clone.isTemporary()).isTrue();
```

#### SA9 — Temporary unit removed after duration

```java
applyAction(shadowClone);  // Clone lasts 2 rounds
processRoundEnd();
processRoundEnd();
Unit clone = state.getUnitById(cloneId);
assertThat(clone).isNull();  // Removed
```

#### SA10 — STUN effect prevents target action

```java
// If a skill applies stun
applyAction(stunSkill);
assertThat(target.hasBuff(BuffType.STUN)).isTrue();
```

#### SA11 — MARK effect increases damage taken

```java
applyAction(deathMark);  // +2 damage from all sources
int originalHp = target.getHp();
applyAction(attack);  // Normal attack
assertThat(target.getHp()).isEqualTo(originalHp - (baseAttack + 2));
```

#### SA12 — Skill sets cooldown after execution

Verified by SC3.

#### SA13 — Skill with SLOW executes next round

```java
applyBuff(hero, BuffType.SLOW);
applyAction(elementalBlast);
assertThat(target.getHp()).isEqualTo(originalHp);  // No damage yet

processRoundEnd();
processNextRoundStart();
assertThat(target.getHp()).isEqualTo(originalHp - 3);  // Now damaged
```

#### SA14 — Multiple effects apply in order

```java
// Trinity: HEAL + REMOVE_BUFF + APPLY_BUFF
applyAction(trinity);
// All effects should apply
assertThat(ally.getHp()).isGreaterThan(originalHp);
assertThat(ally.hasBuff(BuffType.LIFE)).isTrue();
```

---

### SW-Series — WARRIOR Skills

#### SW1 — Heroic Leap: Moves hero to target tile

```java
hero.setPosition(pos(0, 0));
applyAction(heroicLeap, pos(2, 2));
assertThat(hero.getPosition()).isEqualTo(pos(2, 2));
```

#### SW2 — Heroic Leap: Deals 2 damage to adjacent enemies on landing

```java
placeEnemy(pos(2, 1));  // Adjacent to landing position
placeEnemy(pos(3, 2));  // Adjacent to landing position
int hp1 = enemy1.getHp();
int hp2 = enemy2.getHp();

applyAction(heroicLeap, pos(2, 2));

assertThat(enemy1.getHp()).isEqualTo(hp1 - 2);
assertThat(enemy2.getHp()).isEqualTo(hp2 - 2);
```

#### SW3 — Heroic Leap: Does not damage non-adjacent enemies

```java
placeEnemy(pos(4, 4));  // Not adjacent to (2, 2)
int originalHp = enemy.getHp();
applyAction(heroicLeap, pos(2, 2));
assertThat(enemy.getHp()).isEqualTo(originalHp);
```

#### SW4 — Heroic Leap: Cannot leap to occupied tile

Verified by SV7.

#### SW5 — Shockwave: Deals 1 damage to all adjacent enemies

```java
placeEnemy(pos(2, 1));  // Adjacent
placeEnemy(pos(2, 3));  // Adjacent
hero.setPosition(pos(2, 2));

applyAction(shockwave);

assertThat(enemy1.getHp()).isEqualTo(originalHp - 1);
assertThat(enemy2.getHp()).isEqualTo(originalHp - 1);
```

#### SW6 — Shockwave: Pushes enemies 1 tile away

```java
enemy.setPosition(pos(2, 1));
hero.setPosition(pos(2, 2));
applyAction(shockwave);
assertThat(enemy.getPosition()).isEqualTo(pos(2, 0));  // Pushed away
```

#### SW7 — Shockwave: Blocked push deals +1 damage

```java
enemy.setPosition(pos(2, 1));
placeObstacle(pos(2, 0));  // Blocks push direction
hero.setPosition(pos(2, 2));

applyAction(shockwave);

assertThat(enemy.getPosition()).isEqualTo(pos(2, 1));  // Not moved
assertThat(enemy.getHp()).isEqualTo(originalHp - 2);   // 1 + 1 bonus
```

#### SW8 — Shockwave: Does not affect friendly units

```java
placeFriendly(pos(2, 1));
hero.setPosition(pos(2, 2));
applyAction(shockwave);
assertThat(friendly.getHp()).isEqualTo(originalHp);  // No damage
```

#### SW9 — Endure: Grants 3 temporary HP (shield)

```java
hero.setHp(5);
applyAction(endure);
assertThat(hero.getShield()).isEqualTo(3);
```

#### SW10 — Endure: Shield absorbs damage first

```java
hero.setHp(5);
hero.setShield(3);
applyAction(enemyAttack);  // 2 damage
assertThat(hero.getShield()).isEqualTo(1);
assertThat(hero.getHp()).isEqualTo(5);  // HP unchanged
```

#### SW11 — Endure: Removes BLEED debuff

```java
applyBuff(hero, BuffType.BLEED);
applyAction(endure);
assertThat(hero.hasBuff(BuffType.BLEED)).isFalse();
```

#### SW12 — Endure: Shield lasts 2 rounds then expires

```java
applyAction(endure);
assertThat(hero.getShield()).isEqualTo(3);
processRoundEnd();
processRoundEnd();
assertThat(hero.getShield()).isEqualTo(0);
```

---

### SMG-Series — MAGE Skills

#### SMG1 — Elemental Blast: Deals 3 damage to target

```java
int originalHp = target.getHp();
applyAction(elementalBlast, targetPos, targetId);
assertThat(target.getHp()).isEqualTo(originalHp - 3);
```

#### SMG2 — Elemental Blast: 50% chance to apply debuff

```java
// Statistical test
int debuffCount = 0;
for (int i = 0; i < 100; i++) {
    resetState();
    applyAction(elementalBlast, targetPos, targetId);
    if (target.hasAnyDebuff()) debuffCount++;
}
assertThat(debuffCount).isBetween(35, 65);  // ~50%
```

#### SMG3 — Elemental Blast: Debuff is WEAKNESS, BLEED, or SLOW

```java
applyAction(elementalBlast);
if (target.hasAnyDebuff()) {
    assertThat(target.getDebuffType()).isIn(
        BuffType.WEAKNESS, BuffType.BLEED, BuffType.SLOW
    );
}
```

#### SMG4 — Elemental Blast: Range is 3

```java
hero.setPosition(pos(0, 0));
target.setPosition(pos(3, 0));  // Distance = 3
ValidationResult result = validateAction(elementalBlast);
assertThat(result.isValid()).isTrue();
```

#### SMG5 — Warp Beacon: First use places beacon

```java
applyAction(warpBeacon, pos(3, 3));
assertThat(hero.getSkillState().get("beacon_position")).isEqualTo(pos(3, 3));
assertThat(hero.getSkillCooldown()).isEqualTo(0);  // No cooldown yet
```

#### SMG6 — Warp Beacon: Beacon visible to enemy

```java
applyAction(warpBeacon, pos(3, 3));
GameState enemyView = state.getViewFor(PLAYER_2);
assertThat(enemyView.getBeaconPositions()).contains(pos(3, 3));
```

#### SMG7 — Warp Beacon: Second use teleports to beacon

```java
hero.setPosition(pos(0, 0));
hero.getSkillState().put("beacon_position", pos(3, 3));
applyAction(warpBeacon);
assertThat(hero.getPosition()).isEqualTo(pos(3, 3));
```

#### SMG8 — Warp Beacon: Teleport triggers cooldown

```java
// After teleporting
assertThat(hero.getSkillCooldown()).isEqualTo(2);
```

#### SMG9 — Warp Beacon: Beacon removed after teleport

```java
applyAction(warpBeaconTeleport);
assertThat(hero.getSkillState().get("beacon_position")).isNull();
```

#### SMG10 — Warp Beacon: Mage death removes beacon

```java
applyAction(warpBeacon, pos(3, 3));
killUnit(hero);
assertThat(state.getBeaconPositions()).isEmpty();
```

#### SMG11 — Wild Magic: Deals 1 damage to ALL enemies

```java
placeEnemy(pos(0, 0));
placeEnemy(pos(4, 4));
applyAction(wildMagic);
assertThat(enemy1.getHp()).isEqualTo(originalHp1 - 1);
assertThat(enemy2.getHp()).isEqualTo(originalHp2 - 1);
```

#### SMG12 — Wild Magic: 33% chance per enemy to apply debuff

```java
// Statistical test
```

#### SMG13 — Wild Magic: Does not damage friendly units

```java
applyAction(wildMagic);
assertThat(friendlyMinion.getHp()).isEqualTo(originalHp);
```

#### SMG14 — Wild Magic: No targeting required (ALL_ENEMIES)

```java
Action action = Action.useSkill(mageId, null, null);  // No target
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isTrue();
```

---

### SR-Series — ROGUE Skills

#### SR1 — Smoke Bomb: Teleports to target tile

```java
hero.setPosition(pos(0, 0));
applyAction(smokeBomb, pos(2, 2));
assertThat(hero.getPosition()).isEqualTo(pos(2, 2));
```

#### SR2 — Smoke Bomb: Grants invisible for 1 round

```java
applyAction(smokeBomb);
assertThat(hero.isInvisible()).isTrue();
```

#### SR3 — Smoke Bomb: Invisible prevents targeting by enemies

```java
applyAction(smokeBomb);
Action enemyAttack = Action.attack(enemyId, heroPos, heroId);
ValidationResult result = validateAction(enemyAttack);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("invisible");
```

#### SR4 — Smoke Bomb: Invisible does not prevent AoE damage

```java
applyAction(smokeBomb);
// Enemy uses Wild Magic (AoE)
applyAction(wildMagic);
assertThat(hero.getHp()).isEqualTo(originalHp - 1);  // Still takes damage
```

#### SR5 — Smoke Bomb: Blinds adjacent enemies at original position

```java
placeEnemy(pos(0, 1));  // Adjacent to (0, 0)
hero.setPosition(pos(0, 0));
applyAction(smokeBomb, pos(2, 2));
assertThat(enemy.hasBuff(BuffType.BLIND)).isTrue();
```

#### SR6 — Death Mark: Marks target for 2 rounds

```java
applyAction(deathMark, targetPos, targetId);
assertThat(target.hasMark()).isTrue();
assertThat(target.getMarkDuration()).isEqualTo(2);
```

#### SR7 — Death Mark: Marked target takes +2 damage

```java
applyAction(deathMark, targetPos, targetId);
int originalHp = target.getHp();
applyAction(attack);  // Normal attack (1 damage base)
assertThat(target.getHp()).isEqualTo(originalHp - 3);  // 1 + 2 mark bonus
```

#### SR8 — Death Mark: Rogue heals 2 HP if marked target dies

```java
target.setHp(1);
applyAction(deathMark, targetPos, targetId);
hero.setHp(3);
applyAction(attack);  // Kills marked target
assertThat(hero.getHp()).isEqualTo(5);  // 3 + 2 heal
```

#### SR9 — Shadow Clone: Spawns clone on adjacent tile

```java
hero.setPosition(pos(2, 2));
applyAction(shadowClone, pos(2, 3));
Unit clone = state.getUnitAt(pos(2, 3));
assertThat(clone).isNotNull();
assertThat(clone.isTemporary()).isTrue();
```

#### SR10 — Shadow Clone: Clone has 1 HP, 1 ATK

```java
Unit clone = getClone();
assertThat(clone.getHp()).isEqualTo(1);
assertThat(clone.getAttack()).isEqualTo(1);
```

#### SR11 — Shadow Clone: Clone can move and attack

```java
Unit clone = getClone();
Action cloneMove = Action.move(cloneId, newPos);
ValidationResult result = validateAction(cloneMove);
assertThat(result.isValid()).isTrue();
```

#### SR12 — Shadow Clone: Clone lasts 2 rounds

```java
applyAction(shadowClone);
processRoundEnd();
assertThat(state.getUnitById(cloneId)).isNotNull();
processRoundEnd();
assertThat(state.getUnitById(cloneId)).isNull();  // Removed
```

#### SR13 — Shadow Clone: Clone death does NOT trigger death choice

```java
Unit clone = getClone();
killUnit(clone);
assertThat(state.getPendingDeathChoice()).isNull();
```

#### SR14 — Shadow Clone: Clone cannot be healed

```java
Unit clone = getClone();
Action heal = Action.useSkill(clericId, clonePos, cloneId);  // Trinity
ValidationResult result = validateAction(heal);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("cannot heal");
```

---

### SH-Series — HUNTRESS Skills

#### SH1 — Spirit Hawk: Deals 2 damage at range 4

```java
target.setPosition(pos(4, 0));
hero.setPosition(pos(0, 0));  // Distance = 4
applyAction(spiritHawk, targetPos, targetId);
assertThat(target.getHp()).isEqualTo(originalHp - 2);
```

#### SH2 — Spirit Hawk: Can target any enemy in range

```java
// No line-of-sight requirement
placeObstacle(pos(2, 0));  // Between hero and target
applyAction(spiritHawk, targetPos, targetId);
assertThat(target.getHp()).isEqualTo(originalHp - 2);  // Still works
```

#### SH3 — Spectral Blades: Deals 1 damage in a line

```java
hero.setPosition(pos(0, 2));
placeEnemy(pos(1, 2));
placeEnemy(pos(2, 2));
placeEnemy(pos(3, 2));

applyAction(spectralBlades, pos(3, 2));  // Direction: right

assertThat(enemy1.getHp()).isEqualTo(originalHp - 1);
assertThat(enemy2.getHp()).isEqualTo(originalHp - 1);
assertThat(enemy3.getHp()).isEqualTo(originalHp - 1);
```

#### SH4 — Spectral Blades: Pierces through enemies

Verified by SH3.

#### SH5 — Spectral Blades: Range is 3

```java
hero.setPosition(pos(0, 0));
Action action = Action.useSkill(huntressId, pos(4, 0));  // Distance 4
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
```

#### SH6 — Spectral Blades: Must be orthogonal

Verified by SV12.

#### SH7 — Spectral Blades: Does not damage friendly units

```java
placeFriendly(pos(1, 0));
placeEnemy(pos(2, 0));
applyAction(spectralBlades, pos(3, 0));
assertThat(friendly.getHp()).isEqualTo(originalHp);
assertThat(enemy.getHp()).isEqualTo(originalHp - 1);
```

#### SH8 — Nature's Power: Grants +2 damage for next 2 attacks

```java
applyAction(naturesPower);
assertThat(hero.getBonusAttackCharges()).isEqualTo(2);

applyAction(attack);
assertThat(target.getHp()).isEqualTo(originalHp - (baseAttack + 2));
assertThat(hero.getBonusAttackCharges()).isEqualTo(1);
```

#### SH9 — Nature's Power: Applies LIFE buff to self

```java
applyAction(naturesPower);
assertThat(hero.hasBuff(BuffType.LIFE)).isTrue();
```

#### SH10 — Nature's Power: Bonus damage uses charges

```java
applyAction(naturesPower);  // 2 charges
applyAction(attack);        // 1 charge left
applyAction(attack);        // 0 charges left
applyAction(attack);        // Normal damage
assertThat(target.getHp()).isEqualTo(originalHp - (2 * (base + 2)) - base);
```

#### SH11 — Nature's Power: Charges persist across rounds

```java
applyAction(naturesPower);
processRoundEnd();
assertThat(hero.getBonusAttackCharges()).isEqualTo(2);  // Still there
```

#### SH12 — Nature's Power: Charges consumed by MOVE_AND_ATTACK

```java
applyAction(naturesPower);
applyAction(moveAndAttack);
assertThat(hero.getBonusAttackCharges()).isEqualTo(1);
```

---

### SD-Series — DUELIST Skills

#### SD1 — Challenge: Marks enemy as "Challenged" for 2 rounds

```java
applyAction(challenge, targetPos, targetId);
assertThat(target.isChallenged()).isTrue();
assertThat(target.getChallengedDuration()).isEqualTo(2);
```

#### SD2 — Challenge: Challenged enemy deals 50% damage to non-Duelist

```java
applyAction(challenge, targetPos, targetId);
int friendlyOriginalHp = friendly.getHp();
// Challenged enemy attacks friendly minion
applyAction(challengedEnemyAttack);
int expectedDamage = enemyBaseAttack / 2;  // 50% damage
assertThat(friendly.getHp()).isEqualTo(friendlyOriginalHp - expectedDamage);
```

#### SD3 — Challenge: Challenged enemy deals full damage to Duelist

```java
applyAction(challenge, targetPos, targetId);
int heroOriginalHp = hero.getHp();
// Challenged enemy attacks Duelist
applyAction(challengedEnemyAttackDuelist);
assertThat(hero.getHp()).isEqualTo(heroOriginalHp - enemyBaseAttack);
```

#### SD4 — Challenge: Duelist counter-attacks when attacked by Challenged enemy

```java
applyAction(challenge, targetPos, targetId);
int targetOriginalHp = target.getHp();
// Challenged enemy attacks Duelist
applyAction(challengedEnemyAttackDuelist);
assertThat(target.getHp()).isEqualTo(targetOriginalHp - 2);  // Counter damage
```

#### SD5 — Challenge: Counter-attack does not consume action

```java
applyAction(challenge, targetPos, targetId);
applyAction(challengedEnemyAttackDuelist);  // Triggers counter
assertThat(hero.hasActed()).isFalse();  // Duelist can still act
```

#### SD6 — Challenge: Counter-attack is normal attack (Guardian intercepts)

```java
// TANK adjacent to Duelist
applyAction(challenge, targetPos, targetId);
applyAction(challengedEnemyAttackDuelist);  // Counter triggers
// Guardian intercepts counter-attack damage
assertThat(tank.getHp()).isLessThan(originalTankHp);
```

#### SD7 — Elemental Strike: Deals 3 damage to adjacent enemy

```java
placeEnemy(pos(2, 1));
hero.setPosition(pos(2, 2));
int originalHp = enemy.getHp();
applyAction(elementalStrike, enemyPos, enemyId);
assertThat(enemy.getHp()).isEqualTo(originalHp - 3);
```

#### SD8 — Elemental Strike: Player chooses debuff (BLEED, SLOW, or WEAKNESS)

```java
Action action = Action.useSkillWithOption(duelistId, enemyPos, enemyId, BuffType.SLOW);
applyAction(action);
assertThat(enemy.hasBuff(BuffType.SLOW)).isTrue();
```

#### SD9 — Elemental Strike: Range is 1 (adjacent only)

```java
enemy.setPosition(pos(4, 4));
hero.setPosition(pos(2, 2));  // Distance > 1
ValidationResult result = validateAction(elementalStrike);
assertThat(result.isValid()).isFalse();
```

#### SD10 — Feint: Grants "dodge next attack" state

```java
applyAction(feint);
assertThat(hero.hasFeintActive()).isTrue();
```

#### SD11 — Feint: Next attack against Duelist misses

```java
applyAction(feint);
int originalHp = hero.getHp();
applyAction(enemyAttack);
assertThat(hero.getHp()).isEqualTo(originalHp);  // No damage
```

#### SD12 — Feint: Triggers counter-attack on dodge

```java
applyAction(feint);
int enemyOriginalHp = enemy.getHp();
applyAction(enemyAttack);  // Misses, triggers counter
assertThat(enemy.getHp()).isEqualTo(enemyOriginalHp - 2);
```

#### SD13 — Feint: Counter-attack does not consume action

```java
applyAction(feint);
applyAction(enemyAttack);
assertThat(hero.hasActed()).isFalse();
```

#### SD14 — Feint: Expires after 2 rounds if not triggered

```java
applyAction(feint);
processRoundEnd();
processRoundEnd();
assertThat(hero.hasFeintActive()).isFalse();
```

---

### SCL-Series — CLERIC Skills

#### SCL1 — Trinity: Heals target for 3 HP

```java
ally.setHp(2);
applyAction(trinity, allyPos, allyId);
assertThat(ally.getHp()).isEqualTo(5);  // 2 + 3
```

#### SCL2 — Trinity: Can target self

```java
hero.setHp(2);
applyAction(trinity, heroPos, heroId);
assertThat(hero.getHp()).isEqualTo(5);
```

#### SCL3 — Trinity: Removes one random debuff

```java
applyBuff(ally, BuffType.WEAKNESS);
applyBuff(ally, BuffType.BLEED);
applyAction(trinity, allyPos, allyId);
// One debuff removed
assertThat(ally.getDebuffs()).hasSize(1);
```

#### SCL4 — Trinity: Grants LIFE buff (+3 HP)

```java
int originalHp = ally.getHp();
applyAction(trinity, allyPos, allyId);
// 3 (heal) + 3 (LIFE buff instant)
assertThat(ally.getHp()).isEqualTo(originalHp + 6);
```

#### SCL5 — Trinity: Range is 2

```java
ally.setPosition(pos(0, 3));
hero.setPosition(pos(0, 0));  // Distance = 3
ValidationResult result = validateAction(trinity);
assertThat(result.isValid()).isFalse();
```

#### SCL6 — Power of Many: Heals ALL friendly units for 1 HP

```java
setHp(hero, 4);
setHp(minion1, 3);
setHp(minion2, 2);

applyAction(powerOfMany);

assertThat(hero.getHp()).isEqualTo(5);
assertThat(minion1.getHp()).isEqualTo(4);
assertThat(minion2.getHp()).isEqualTo(3);
```

#### SCL7 — Power of Many: Grants +1 ATK for 1 round

```java
applyAction(powerOfMany);
assertThat(getEffectiveAttack(minion1)).isEqualTo(baseAttack + 1);

processRoundEnd();
assertThat(getEffectiveAttack(minion1)).isEqualTo(baseAttack);  // Expired
```

#### SCL8 — Power of Many: No targeting required (ALL_ALLIES)

```java
Action action = Action.useSkill(clericId, null, null);
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isTrue();
```

#### SCL9 — Ascended Form: Grants invulnerability for 1 round

```java
applyAction(ascendedForm);
assertThat(hero.isInvulnerable()).isTrue();
```

#### SCL10 — Ascended Form: Invulnerable prevents all damage

```java
applyAction(ascendedForm);
int originalHp = hero.getHp();
applyAction(enemyAttack);
applyAction(enemySkill);
assertThat(hero.getHp()).isEqualTo(originalHp);  // No damage
```

#### SCL11 — Ascended Form: Cannot attack while invulnerable

```java
applyAction(ascendedForm);
Action attack = Action.attack(clericId, enemyPos, enemyId);
ValidationResult result = validateAction(attack);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("invulnerable");
```

#### SCL12 — Ascended Form: Healing effects doubled

```java
applyAction(ascendedForm);
hero.setHp(2);
// Another unit heals Cleric
applyAction(otherClericTrinity, heroPos, heroId);
// 3 heal * 2 = 6
assertThat(hero.getHp()).isEqualTo(8);
```

---

### SB-Series — Skill + BUFF Interaction

#### SB1 — STUN prevents USE_SKILL

Verified by SV14.

#### SB2 — ROOT prevents movement skills (Heroic Leap, Smoke Bomb, Warp Beacon teleport)

```java
applyBuff(hero, BuffType.ROOT);
hero.setSelectedSkillId("warrior_heroic_leap");
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isFalse();
```

#### SB3 — ROOT does not prevent non-movement skills

Verified by SV15.

#### SB4 — SLOW delays skill by 1 round

```java
applyBuff(hero, BuffType.SLOW);
applyAction(elementalBlast);
assertThat(target.getHp()).isEqualTo(originalHp);  // Not yet

processRoundEnd();
processNextRoundStart();
assertThat(target.getHp()).isEqualTo(originalHp - 3);  // Now
```

#### SB5 — SLOW + Skill: Preparing state stored

```java
applyBuff(hero, BuffType.SLOW);
applyAction(elementalBlast);
assertThat(hero.isPreparing()).isTrue();
assertThat(hero.getPreparingAction().getType()).isEqualTo(USE_SKILL);
```

#### SB6 — SLOW + Skill: If hero dies while preparing, skill cancelled

```java
applyBuff(hero, BuffType.SLOW);
applyAction(elementalBlast);
killUnit(hero);
processRoundEnd();
assertThat(target.getHp()).isEqualTo(originalHp);  // Skill never executed
```

#### SB7 — SPEED + Skill: Can use skill as one of two actions

```java
applyBuff(hero, BuffType.SPEED);
applyAction(useSkill);
assertThat(hero.getRemainingActions()).isEqualTo(1);  // 1 action left
applyAction(attack);
assertThat(hero.getRemainingActions()).isEqualTo(0);
```

#### SB8 — POWER + Skill damage: Skill damage NOT affected by ATK bonus

```java
applyBuff(hero, BuffType.POWER);  // +3 ATK
applyAction(elementalBlast);  // Fixed 3 damage
assertThat(target.getHp()).isEqualTo(originalHp - 3);  // Not 6
```

#### SB9 — WEAKNESS + Skill damage: Skill damage NOT affected by ATK penalty

```java
applyBuff(hero, BuffType.WEAKNESS);  // -2 ATK
applyAction(elementalBlast);  // Fixed 3 damage
assertThat(target.getHp()).isEqualTo(originalHp - 3);  // Still 3
```

#### SB10 — Skill that applies BUFF: Duration is 2

```java
applyAction(elementalStrike);  // Applies SLOW
assertThat(target.getBuffDuration(BuffType.SLOW)).isEqualTo(2);
```

#### SB11 — Death Mark + BUFF damage bonus interaction

```java
applyAction(deathMark);  // +2 damage from all sources
applyBuff(attacker, BuffType.POWER);  // +3 ATK
applyAction(attack);
// Damage = base + 3 (POWER) + 2 (Mark) = base + 5
assertThat(target.getHp()).isEqualTo(originalHp - (baseAttack + 5));
```

#### SB12 — Invisible + Skills: Can use skills while invisible

```java
applyAction(smokeBomb);  // Becomes invisible
assertThat(hero.isInvisible()).isTrue();
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isTrue();
```

#### SB13 — Invisible: Using skill breaks invisibility

```java
applyAction(smokeBomb);
applyAction(useOtherSkill);
assertThat(hero.isInvisible()).isFalse();
```

#### SB14 — Invulnerable + Skills: Can use healing skills

```java
applyAction(ascendedForm);
hero.setSelectedSkillId("cleric_trinity");
ValidationResult result = validateAction(trinity);
assertThat(result.isValid()).isTrue();
```

#### SB15 — BLEED does not affect skill usage

```java
applyBuff(hero, BuffType.BLEED);
ValidationResult result = validateAction(useSkill);
assertThat(result.isValid()).isTrue();
```

#### SB16 — Skill that heals: Affected by LIFE buff instant bonus

```java
// If target has LIFE buff that hasn't applied instant bonus yet
// Complex interaction - may not apply
```

---

### SG-Series — Skill + Guardian Interaction

#### SG1 — Skill damage IS intercepted by Guardian

```java
// TANK adjacent to target
placeUnit(tank, pos(2, 1));
placeUnit(target, pos(2, 2));

int tankOriginalHp = tank.getHp();
int targetOriginalHp = target.getHp();

applyAction(elementalBlast, targetPos, targetId);  // 3 damage

assertThat(tank.getHp()).isEqualTo(tankOriginalHp - 3);  // Tank took damage
assertThat(target.getHp()).isEqualTo(targetOriginalHp);   // Target protected
```

#### SG2 — AoE skill damage: Each target checked for Guardian

```java
// Shockwave hits multiple enemies, each with potential Guardian
placeEnemyTank(pos(2, 1));  // Adjacent to target1
placeEnemy(target1, pos(2, 2));
placeEnemy(target2, pos(1, 2));  // No adjacent tank

applyAction(shockwave);

assertThat(enemyTank.getHp()).isEqualTo(originalHp - 1);  // Intercepted
assertThat(target1.getHp()).isEqualTo(originalHp);         // Protected
assertThat(target2.getHp()).isEqualTo(originalHp - 1);     // Not protected
```

#### SG3 — Heroic Leap AoE: Guardian intercepts landing damage

```java
placeEnemyTank(pos(3, 2));
placeEnemy(pos(2, 2));  // Landing position adjacent

applyAction(heroicLeap, pos(2, 2));

assertThat(enemyTank.getHp()).isEqualTo(originalHp - 2);  // Intercepted
assertThat(enemy.getHp()).isEqualTo(originalHp);           // Protected
```

#### SG4 — Guardian: Cannot intercept damage to self

```java
// Only TANK in range
placeEnemyTank(pos(2, 2));
applyAction(elementalBlast, tankPos, tankId);
assertThat(tank.getHp()).isEqualTo(originalHp - 3);  // Takes damage
```

#### SG5 — Guardian: Intercepts counter-attack damage (Feint, Challenge)

```java
// Duelist uses Feint, Tank adjacent
applyAction(feint);
placeEnemyTank(pos(2, 1));
placeEnemy(pos(2, 2));

// Enemy attacks, triggers counter
applyAction(enemyAttack);

// Counter-attack intercepted by Tank
assertThat(enemyTank.getHp()).isEqualTo(originalHp - 2);
assertThat(enemy.getHp()).isEqualTo(originalHp);
```

#### SG6 — Guardian: Multiple damage instances each checked

```java
// Nature's Power grants bonus damage, then attack
// Guardian intercepts the total damage (not separately)
```

#### SG7 — Guardian: Dead Tank cannot intercept

```java
killUnit(tank);
applyAction(elementalBlast, targetPos, targetId);
assertThat(target.getHp()).isEqualTo(originalHp - 3);  // No protection
```

#### SG8 — Spirit Hawk: Guardian intercepts at target location

```java
placeEnemyTank(pos(4, 1));
placeEnemy(pos(4, 0));

applyAction(spiritHawk, enemyPos, enemyId);

assertThat(enemyTank.getHp()).isEqualTo(originalHp - 2);
assertThat(enemy.getHp()).isEqualTo(originalHp);
```

#### SG9 — Wild Magic: Each enemy checked for Guardian

```java
// Complex scenario with multiple targets and tanks
```

#### SG10 — Guardian: Priority is lowest unit ID if multiple tanks

```java
placeEnemyTank1(pos(2, 1));  // id = "p2_tank1"
placeEnemyTank2(pos(2, 3));  // id = "p2_tank2"
placeEnemy(pos(2, 2));

applyAction(elementalBlast, enemyPos, enemyId);

// p2_tank1 intercepts (lower ID)
assertThat(tank1.getHp()).isEqualTo(originalHp - 3);
assertThat(tank2.getHp()).isEqualTo(originalHp);
```

---

### SSP-Series — Special Skill States

#### SSP1 — Warp Beacon state: beacon_position stored

Verified by SMG5.

#### SSP2 — Warp Beacon state: Cleared after teleport

Verified by SMG9.

#### SSP3 — Warp Beacon state: Cleared on Mage death

Verified by SMG10.

#### SSP4 — Warp Beacon: Cannot place on occupied tile

```java
placeUnit(pos(3, 3));
Action action = Action.useSkill(mageId, pos(3, 3));
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
```

#### SSP5 — Warp Beacon: Cannot teleport to occupied tile

```java
hero.getSkillState().put("beacon_position", pos(3, 3));
placeUnit(pos(3, 3));  // Someone moved there
Action action = Action.useSkill(mageId, null);  // Teleport
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
```

#### SSP6 — Shadow Clone state: Clone tracked as temporary unit

```java
applyAction(shadowClone);
Unit clone = getClone();
assertThat(clone.isTemporary()).isTrue();
assertThat(clone.getTemporaryDuration()).isEqualTo(2);
```

#### SSP7 — Shadow Clone: Duration decrements at round end

```java
applyAction(shadowClone);
processRoundEnd();
assertThat(clone.getTemporaryDuration()).isEqualTo(1);
```

#### SSP8 — Shadow Clone: Removed when duration reaches 0

Verified by SR12.

#### SSP9 — Shadow Clone: Multiple clones can exist

```java
applyAction(shadowClone, pos(2, 1));
// Wait for cooldown
processRoundEnd();
processRoundEnd();
applyAction(shadowClone, pos(2, 3));
assertThat(state.getClones()).hasSize(2);
```

#### SSP10 — Feint state: feintActive flag stored

Verified by SD10.

#### SSP11 — Feint state: Cleared after trigger

```java
applyAction(feint);
applyAction(enemyAttack);  // Triggers feint
assertThat(hero.hasFeintActive()).isFalse();
```

#### SSP12 — Feint state: Cleared after 2 rounds

Verified by SD14.

#### SSP13 — Challenge state: challengedTargetId stored

```java
applyAction(challenge, targetPos, targetId);
assertThat(hero.getChallengedTargetId()).isEqualTo(targetId);
```

#### SSP14 — Challenge state: Cleared when duration expires

```java
applyAction(challenge, targetPos, targetId);
processRoundEnd();
processRoundEnd();
assertThat(hero.getChallengedTargetId()).isNull();
```

#### SSP15 — Challenge state: Cleared when target dies

```java
applyAction(challenge, targetPos, targetId);
killUnit(target);
assertThat(hero.getChallengedTargetId()).isNull();
```

#### SSP16 — Nature's Power state: bonusAttackCharges stored

Verified by SH8.

#### SSP17 — Nature's Power state: Charges persist until used

Verified by SH11.

#### SSP18 — Invulnerable state: Cleared at round end

```java
applyAction(ascendedForm);
assertThat(hero.isInvulnerable()).isTrue();
processRoundEnd();
assertThat(hero.isInvulnerable()).isFalse();
```

---

### SBC-Series — Backward Compatibility

#### SBC1 — No skills: Game functions normally

```java
// Hero with no selectedSkillId
hero.setSelectedSkillId(null);
// Game should still work
ValidationResult result = validateAction(move);
assertThat(result.isValid()).isTrue();
```

#### SBC2 — No skills: USE_SKILL action is invalid

```java
hero.setSelectedSkillId(null);
Action action = Action.useSkill(heroId, targetPos);
ValidationResult result = validateAction(action);
assertThat(result.isValid()).isFalse();
```

#### SBC3 — V2 rules still apply with skills

```java
// moveRange/attackRange still work
hero.setSelectedSkillId("warrior_heroic_leap");
// Normal move still uses moveRange
Action move = Action.move(heroId, pos(0, 3));  // Distance > moveRange
ValidationResult result = validateAction(move);
assertThat(result.isValid()).isFalse();
```

#### SBC4 — V1 BUFF rules still apply with skills

```java
// Stun from V1 BUFF system still prevents actions
applyBuff(hero, BuffType.STUN);
Action attack = Action.attack(heroId, targetPos, targetId);
ValidationResult result = validateAction(attack);
assertThat(result.isValid()).isFalse();
```

#### SBC5 — Minions cannot use skills

Verified by SV1.

#### SBC6 — Skill system does not break existing tests

```java
// All RuleEngineValidateActionTest should pass
// All RuleEngineApplyActionTest should pass
```

---

### SDT-Series — Deterministic Ordering

#### SDT1 — Skill effects apply in defined order

```java
// Skill with multiple effects: order matters
SkillDefinition skill = SkillRegistry.get("cleric_trinity");
List<SkillEffect> effects = skill.getEffects();
assertThat(effects.get(0).getType()).isEqualTo(EffectType.HEAL);
assertThat(effects.get(1).getType()).isEqualTo(EffectType.REMOVE_BUFF);
assertThat(effects.get(2).getType()).isEqualTo(EffectType.APPLY_BUFF);
```

#### SDT2 — AoE damage applies in unit ID order

```java
// Wild Magic damages all enemies in deterministic order
```

#### SDT3 — Counter-attacks resolve in deterministic order

```java
// If multiple counter-attacks could trigger
```

#### SDT4 — Temporary unit removal in deterministic order

```java
// Multiple clones expire same round
```

#### SDT5 — Replay: Same actions produce identical state

```java
GameState state1 = replayActions(initial, actions);
GameState state2 = replayActions(initial, actions);
assertThat(serialize(state1)).isEqualTo(serialize(state2));
```

#### SDT6 — Random effects use RngProvider

```java
// Elemental Blast 50% debuff chance
// Must use RngProvider for deterministic replay
```

---

## 6. Test Data Fixtures

### 6.1 Standard Test State with Skills

```java
GameState createSkillTestState() {
    return GameState.builder()
        .board(Board.create(5, 5))
        .currentPlayer(PLAYER_1)
        .units(Arrays.asList(
            createHero("p1_hero", PLAYER_1, HeroClass.WARRIOR, pos(2, 0), 5)
                .withSelectedSkillId("warrior_heroic_leap")
                .withSkillCooldown(0),
            createMinion("p1_tank", PLAYER_1, TANK, pos(0, 0), 5),
            createMinion("p1_archer", PLAYER_1, ARCHER, pos(4, 0), 3),
            createHero("p2_hero", PLAYER_2, HeroClass.MAGE, pos(2, 4), 5)
                .withSelectedSkillId("mage_elemental_blast")
                .withSkillCooldown(0),
            createMinion("p2_tank", PLAYER_2, TANK, pos(0, 4), 5),
            createMinion("p2_assassin", PLAYER_2, ASSASSIN, pos(4, 4), 2)
        ))
        .build();
}
```

### 6.2 Skill Action Helpers

```java
Action useSkill(String heroId, Position target) {
    return Action.builder()
        .type(ActionType.USE_SKILL)
        .unitId(heroId)
        .targetPosition(target)
        .build();
}

Action useSkill(String heroId, Position target, String targetUnitId) {
    return Action.builder()
        .type(ActionType.USE_SKILL)
        .unitId(heroId)
        .targetPosition(target)
        .targetUnitId(targetUnitId)
        .build();
}
```

---

## 7. Error Message Standards

### 7.1 Skill Validation Error Messages

| Scenario | Expected Message |
|----------|------------------|
| Non-hero uses skill | "USE_SKILL is only available to heroes" |
| No skill selected | "Hero has no skill selected" |
| Skill on cooldown | "Skill is on cooldown (X rounds remaining)" |
| Target out of range | "Target is out of skill range" |
| Target is friendly (for enemy-target skill) | "Skill must target an enemy" |
| Target is enemy (for ally-target skill) | "Skill must target an ally or self" |
| Target tile occupied (for tile-target skill) | "Target tile is occupied" |
| Wrong class for skill | "Hero cannot use skills from other classes" |
| Stunned | "Hero is stunned and cannot use skills" |
| Rooted (movement skill) | "Hero is rooted and cannot use movement skills" |
| Invulnerable (attack action) | "Hero cannot attack while invulnerable" |
| Clone cannot be healed | "Temporary units cannot be healed" |

---

## 8. Implementation Checklist

### Models
- [ ] HeroClass enum
- [ ] TargetType enum
- [ ] EffectType enum
- [ ] SkillDefinition class
- [ ] SkillEffect class
- [ ] SkillRegistry with all 18 skills
- [ ] Hero.selectedSkillId field
- [ ] Hero.skillCooldown field
- [ ] Hero.skillState map (for Warp Beacon, etc.)
- [ ] Unit.isTemporary flag (for Shadow Clone)
- [ ] Unit.temporaryDuration field

### Validation
- [ ] USE_SKILL action type
- [ ] Skill cooldown check
- [ ] Target type validation
- [ ] Range validation
- [ ] Class restriction validation
- [ ] BUFF interaction (STUN, ROOT, SLOW)

### Execution
- [ ] DAMAGE effect handler
- [ ] HEAL effect handler
- [ ] MOVE_SELF effect handler
- [ ] MOVE_TARGET effect handler
- [ ] APPLY_BUFF effect handler
- [ ] SPAWN_UNIT effect handler
- [ ] STUN effect handler
- [ ] MARK effect handler
- [ ] Guardian interception for skill damage
- [ ] Counter-attack system (Feint, Challenge)
- [ ] Temporary unit lifecycle

### Per-Skill
- [ ] Heroic Leap (WARRIOR)
- [ ] Shockwave (WARRIOR)
- [ ] Endure (WARRIOR)
- [ ] Elemental Blast (MAGE)
- [ ] Warp Beacon (MAGE)
- [ ] Wild Magic (MAGE)
- [ ] Smoke Bomb (ROGUE)
- [ ] Death Mark (ROGUE)
- [ ] Shadow Clone (ROGUE)
- [ ] Spirit Hawk (HUNTRESS)
- [ ] Spectral Blades (HUNTRESS)
- [ ] Nature's Power (HUNTRESS)
- [ ] Challenge (DUELIST)
- [ ] Elemental Strike (DUELIST)
- [ ] Feint (DUELIST)
- [ ] Trinity (CLERIC)
- [ ] Power of Many (CLERIC)
- [ ] Ascended Form (CLERIC)

---

## 9. Summary

This test plan defines:

- **201 mandatory test cases** across 16 series
- Complete coverage of skill model and registry
- Skill cooldown mechanics
- All 18 skills across 6 hero classes
- Skill + BUFF interaction (STUN blocks, SLOW delays, etc.)
- Skill + Guardian interaction (skill damage intercepted)
- Special skill states (Warp Beacon, Shadow Clone, Feint, Challenge)
- Counter-attack mechanics
- Deterministic behavior for replay
- Backward compatibility with V1/V2

Upon completing this test plan, the V3 Skill System implementation will be fully test-driven and correct.

---

*End of SKILL_SYSTEM_V3_TESTPLAN.md*
