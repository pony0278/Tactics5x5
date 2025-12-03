# BUFF_SYSTEM_V3_TESTPLAN.md — 5×5 Tactics Buff System Test Plan (Version 3)

## 1. Scope

This document defines the test cases required for the Buff System V3.

It covers:

- Buff model correctness (6 buff types)
- Buff serialization & deserialization
- Buff lifecycle behavior (fixed 2-round duration)
- Buff acquisition via Buff Tiles
- Buff effects on movement, attack, and action restrictions
- POWER buff: blocks MOVE_AND_ATTACK, enables obstacle destruction
- SPEED buff: grants double action per round
- SLOW buff: delays actions by 1 round ("preparing" state)
- BLEED buff: damage over time
- Buff duration reduction & expiration
- Buff stacking rules
- Interaction with RuleEngine validation
- Interaction with RuleEngine apply
- Backward compatibility with V1/V2 rules
- Deterministic ordering
- Buff Tile spawning, triggering, and expiration

Not covered:

- Skill-triggered buffs (see SKILL_SYSTEM_V3_TESTPLAN.md)
- Hero-specific buff interactions
- Network synchronization
- UI/UX behavior
- Sound/animation effects

---

## 2. Test Series Overview

| Series | Purpose | Test Count |
|--------|---------|------------|
| BM-Series | Buff model correctness | 12 |
| BS-Series | Buff serialization | 8 |
| BL-Series | Buff lifecycle (duration, removal) | 10 |
| BT-Series | Buff Tile mechanics | 15 |
| BV-Series | Buff effects on validation | 20 |
| BA-Series | Buff effects on applyAction | 18 |
| BP-Series | POWER buff special behavior | 8 |
| BSP-Series | SPEED buff double action | 10 |
| BSL-Series | SLOW buff preparing state | 12 |
| BBL-Series | BLEED buff damage over time | 8 |
| BST-Series | Buff stacking rules | 8 |
| BC-Series | Backward compatibility | 6 |
| BD-Series | Deterministic ordering | 6 |
| **Total** | | **141** |

---

## 3. Test Class Structure

### 3.1 Recommended File Layout

```
src/test/java/com/tactics/engine/
├── buff/
│   ├── BuffModelTest.java           # BM-Series
│   ├── BuffSerializerTest.java      # BS-Series
│   ├── BuffLifecycleTest.java       # BL-Series
│   ├── BuffTileTest.java            # BT-Series
│   └── BuffStackingTest.java        # BST-Series
├── rules/
│   ├── RuleEngineBuffValidationTest.java    # BV-Series
│   ├── RuleEngineBuffApplyTest.java         # BA-Series
│   ├── RuleEnginePowerBuffTest.java         # BP-Series
│   ├── RuleEngineSpeedBuffTest.java         # BSP-Series
│   ├── RuleEngineSlowBuffTest.java          # BSL-Series
│   ├── RuleEngineBleedBuffTest.java         # BBL-Series
│   ├── RuleEngineBuffCompatibilityTest.java # BC-Series
│   └── RuleEngineBuffDeterminismTest.java   # BD-Series
```

### 3.2 JUnit 5 Structure Example

```java
@DisplayName("BUFF System V3 - Power Buff Tests")
class RuleEnginePowerBuffTest {
    
    @Nested
    @DisplayName("BP-Series: POWER Buff Validation")
    class PowerBuffValidation {
        @Test @DisplayName("BP1: POWER buff blocks MOVE_AND_ATTACK")
        void powerBuffBlocksMoveAndAttack() { ... }
    }
    
    @Nested
    @DisplayName("BP-Series: POWER Buff Obstacle Destruction")
    class PowerBuffObstacleDestruction {
        @Test @DisplayName("BP5: POWER buff enables DESTROY_OBSTACLE action")
        void powerBuffEnablesDestroyObstacle() { ... }
    }
}
```

---

## 4. Test Series (Detailed)

---

### BM-Series — Buff Model Tests

Tests for `BuffInstance`, `BuffModifier`, `BuffFlags`, and `BuffType` classes.

#### BM1 — BuffType enum contains all 6 types

```java
assertThat(BuffType.values()).containsExactlyInAnyOrder(
    POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW
);
```

#### BM2 — BuffInstance stores all fields correctly

Given a constructed BuffInstance with:
- buffId = "buff_001"
- type = BuffType.POWER
- duration = 2
- sourceUnitId = "unit_p1_hero"

Assert all fields are preserved via getters.

#### BM3 — BuffModifier correctly stores numeric modifiers

```java
BuffModifier mod = new BuffModifier(+3, 0, 0);  // +3 ATK
assertThat(mod.getBonusAttack()).isEqualTo(3);
```

#### BM4 — BuffFlags correctly stores behavioral flags

```java
BuffFlags flags = BuffFlags.builder()
    .powerBuff(true)
    .bleedBuff(false)
    .build();
assertThat(flags.isPowerBuff()).isTrue();
```

#### BM5 — POWER buff has correct default modifiers

```java
BuffInstance power = BuffFactory.createPower(sourceId);
assertThat(power.getModifier().getBonusAttack()).isEqualTo(3);
assertThat(power.getFlags().isPowerBuff()).isTrue();
```

#### BM6 — LIFE buff has correct default modifiers

```java
BuffInstance life = BuffFactory.createLife(sourceId);
// No ongoing modifier, only instant HP
assertThat(life.getInstantHpBonus()).isEqualTo(3);
```

#### BM7 — SPEED buff has correct default modifiers

```java
BuffInstance speed = BuffFactory.createSpeed(sourceId);
assertThat(speed.getModifier().getBonusAttack()).isEqualTo(-1);
assertThat(speed.getFlags().isSpeedBuff()).isTrue();
```

#### BM8 — WEAKNESS buff has correct default modifiers

```java
BuffInstance weakness = BuffFactory.createWeakness(sourceId);
assertThat(weakness.getModifier().getBonusAttack()).isEqualTo(-2);
assertThat(weakness.getInstantHpBonus()).isEqualTo(-1);
```

#### BM9 — BLEED buff has correct default flags

```java
BuffInstance bleed = BuffFactory.createBleed(sourceId);
assertThat(bleed.getFlags().isBleedBuff()).isTrue();
```

#### BM10 — SLOW buff has correct default flags

```java
BuffInstance slow = BuffFactory.createSlow(sourceId);
assertThat(slow.getFlags().isSlowBuff()).isTrue();
```

#### BM11 — All buffs have duration = 2 by default

```java
for (BuffType type : BuffType.values()) {
    BuffInstance buff = BuffFactory.create(type, sourceId);
    assertThat(buff.getDuration()).isEqualTo(2);
}
```

#### BM12 — BuffInstance is immutable

```java
BuffInstance original = BuffFactory.createPower(sourceId);
BuffInstance decreased = original.withDecreasedDuration();
assertThat(original.getDuration()).isEqualTo(2);
assertThat(decreased.getDuration()).isEqualTo(1);
```

---

### BS-Series — Buff Serialization Tests

#### BS1 — GameStateSerializer.toJsonMap includes unitBuffs

```java
Map<String, Object> json = serializer.toJsonMap(state);
assertThat(json).containsKey("unitBuffs");
```

#### BS2 — BuffInstance serializes all fields

Serialized JSON contains:
- buffId
- type
- duration
- sourceUnitId
- modifier { bonusAttack, bonusMoveRange, bonusAttackRange }
- flags { powerBuff, speedBuff, slowBuff, bleedBuff, ... }
- instantHpBonus

#### BS3 — BuffType serializes as string

```java
// Serializes as "POWER", not enum ordinal
assertThat(json.get("type")).isEqualTo("POWER");
```

#### BS4 — fromJsonMap reconstructs buff list exactly

Roundtrip: GameState → Map → GameState
Buffs must match instance-by-instance.

#### BS5 — Empty buff list serializes & deserializes correctly

#### BS6 — BuffTile list serializes correctly

```java
Map<String, Object> json = serializer.toJsonMap(state);
assertThat(json).containsKey("buffTiles");
```

#### BS7 — BuffTile serializes position, type, duration, triggered

#### BS8 — Unknown buff fields in JSON are ignored (forward compatibility)

---

### BL-Series — Buff Lifecycle Tests

#### BL1 — Duration reduces by 1 at end of each round

After a full round completes (all units have acted), all buffs duration--.

#### BL2 — Buff with duration=1 is removed after round end

```java
// Start: buff.duration = 1
// After round end: buff removed from unit
assertThat(unit.getBuffs()).isEmpty();
```

#### BL3 — Multiple buffs decrease durations together

If unit has POWER (dur=2) and BLEED (dur=2), both become dur=1 after round end.

#### BL4 — Buff with duration=0 is removed immediately

#### BL5 — LIFE buff: +3 HP applied instantly on acquisition

```java
int hpBefore = unit.getHp();
applyBuff(unit, BuffType.LIFE);
assertThat(unit.getHp()).isEqualTo(hpBefore + 3);
```

#### BL6 — POWER buff: +1 HP applied instantly on acquisition

#### BL7 — WEAKNESS buff: -1 HP applied instantly on acquisition

```java
int hpBefore = unit.getHp();
applyBuff(unit, BuffType.WEAKNESS);
assertThat(unit.getHp()).isEqualTo(hpBefore - 1);
```

#### BL8 — Instant HP loss can kill unit

```java
unit.setHp(1);
applyBuff(unit, BuffType.WEAKNESS);  // -1 HP
assertThat(unit.isAlive()).isFalse();
```

#### BL9 — Instant HP gain can exceed maxHp

```java
unit.setHp(5);  // maxHp = 5
applyBuff(unit, BuffType.LIFE);  // +3 HP
assertThat(unit.getHp()).isEqualTo(8);
```

#### BL10 — Removing buff does not affect other buffs on the unit

---

### BT-Series — Buff Tile Tests

#### BT1 — Minion death triggers death choice prompt

```java
// When minion dies, player must choose SPAWN_OBSTACLE or SPAWN_BUFF_TILE
DeathChoice choice = gameState.getPendingDeathChoice();
assertThat(choice).isNotNull();
assertThat(choice.getOptions()).containsExactly(SPAWN_OBSTACLE, SPAWN_BUFF_TILE);
```

#### BT2 — Choosing SPAWN_BUFF_TILE creates BuffTile at death position

```java
processDeathChoice(SPAWN_BUFF_TILE);
BuffTile tile = gameState.getBuffTileAt(deathPosition);
assertThat(tile).isNotNull();
```

#### BT3 — BuffTile has duration = 2

```java
assertThat(tile.getDuration()).isEqualTo(2);
```

#### BT4 — BuffTile type is random (equal probability)

```java
// Statistical test over 1000 trials
Map<BuffType, Integer> counts = new HashMap<>();
for (int i = 0; i < 1000; i++) {
    BuffTile tile = createRandomBuffTile();
    counts.merge(tile.getBuffType(), 1, Integer::sum);
}
// Each type should appear ~166 times (1000/6)
for (BuffType type : BuffType.values()) {
    assertThat(counts.get(type)).isBetween(100, 250);
}
```

#### BT5 — Unit stepping on BuffTile triggers buff application

```java
// Unit moves onto tile
applyAction(moveToBuffTile);
// Unit now has the buff
assertThat(unit.getBuffs()).hasSize(1);
```

#### BT6 — BuffTile removed after being triggered

```java
assertThat(gameState.getBuffTileAt(position)).isNull();
```

#### BT7 — BuffTile only triggers on movement end, not pass-through

```java
// Unit moves through tile to another position
applyAction(moveThroughTile);
// Tile NOT triggered
assertThat(gameState.getBuffTileAt(tilePosition)).isNotNull();
assertThat(unit.getBuffs()).isEmpty();
```

#### BT8 — BuffTile triggers before attack in MOVE_AND_ATTACK

```java
// Unit moves onto buff tile, then attacks
// Buff should be applied BEFORE attack damage calculation
applyAction(moveAndAttack);  // lands on POWER tile
// Attack should include +3 ATK from POWER buff
assertThat(target.getHp()).isEqualTo(originalHp - (baseAtk + 3));
```

#### BT9 — BuffTile duration decreases at round end

```java
// Tile at duration=2
processRoundEnd();
assertThat(tile.getDuration()).isEqualTo(1);
```

#### BT10 — BuffTile with duration=0 is removed (expired)

```java
// After 2 rounds, tile should be removed even if not triggered
assertThat(gameState.getBuffTileAt(position)).isNull();
```

#### BT11 — Choosing SPAWN_OBSTACLE creates obstacle at death position

```java
processDeathChoice(SPAWN_OBSTACLE);
assertThat(gameState.hasObstacleAt(deathPosition)).isTrue();
```

#### BT12 — Obstacle blocks movement

```java
ValidationResult result = ruleEngine.validateAction(moveToObstacle);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("occupied");
```

#### BT13 — Obstacle persists until destroyed

```java
// After 10 rounds, obstacle still exists
for (int i = 0; i < 10; i++) processRoundEnd();
assertThat(gameState.hasObstacleAt(position)).isTrue();
```

#### BT14 — Hero death does NOT trigger death choice

```java
// Hero dies
killUnit(hero);
// No death choice, game ends
assertThat(gameState.getPendingDeathChoice()).isNull();
assertThat(gameState.isGameOver()).isTrue();
```

#### BT15 — Multiple BuffTiles can exist on map

```java
killMinion(minion1, SPAWN_BUFF_TILE);
killMinion(minion2, SPAWN_BUFF_TILE);
assertThat(gameState.getBuffTiles()).hasSize(2);
```

---

### BV-Series — RuleEngine validateAction() + Buff Effects

#### BV1 — POWER buff: bonusAttack reflected in damage calculation

Validation allows attack; damage verified in BA-series.

#### BV2 — POWER buff: MOVE_AND_ATTACK is INVALID

```java
applyBuff(unit, BuffType.POWER);
ValidationResult result = validateAction(moveAndAttack);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("Power buff");
```

#### BV3 — POWER buff: MOVE is still valid

#### BV4 — POWER buff: ATTACK is still valid

#### BV5 — POWER buff: DESTROY_OBSTACLE is valid (new action)

```java
applyBuff(unit, BuffType.POWER);
ValidationResult result = validateAction(destroyObstacle);
assertThat(result.isValid()).isTrue();
```

#### BV6 — Without POWER buff: DESTROY_OBSTACLE is INVALID

```java
// No POWER buff
ValidationResult result = validateAction(destroyObstacle);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("requires Power buff");
```

#### BV7 — SPEED buff: First action of round is valid

#### BV8 — SPEED buff: Second action of round is valid

```java
applyBuff(unit, BuffType.SPEED);
applyAction(firstAction);
ValidationResult result = validateAction(secondAction);
assertThat(result.isValid()).isTrue();
```

#### BV9 — Without SPEED buff: Second action is INVALID

```java
// No SPEED buff
applyAction(firstAction);
ValidationResult result = validateAction(secondAction);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("already acted");
```

#### BV10 — SPEED buff: Third action is INVALID

```java
applyBuff(unit, BuffType.SPEED);
applyAction(firstAction);
applyAction(secondAction);
ValidationResult result = validateAction(thirdAction);
assertThat(result.isValid()).isFalse();
```

#### BV11 — SLOW buff: Unit can declare action (enters preparing)

```java
applyBuff(unit, BuffType.SLOW);
ValidationResult result = validateAction(attack);
assertThat(result.isValid()).isTrue();
// Unit enters "preparing" state
assertThat(unit.isPreparing()).isTrue();
```

#### BV12 — SLOW buff: Preparing unit cannot declare new action

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);  // enters preparing
ValidationResult result = validateAction(move);  // try another action
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("preparing");
```

#### BV13 — SLOW buff: Preparing action cannot be cancelled

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);  // enters preparing
ValidationResult result = validateAction(cancelAction);
assertThat(result.isValid()).isFalse();
```

#### BV14 — WEAKNESS buff: bonusAttack = -2 reflected

Damage = base - 2 (minimum 0).

#### BV15 — WEAKNESS buff: Does not block any action types

All action types remain valid.

#### BV16 — BLEED buff: Does not block any action types

#### BV17 — LIFE buff: Does not block any action types

#### BV18 — Multiple buffs: Modifiers are additive

```java
applyBuff(unit, BuffType.POWER);    // +3 ATK
applyBuff(unit, BuffType.WEAKNESS); // -2 ATK
// Net: +1 ATK
assertThat(getEffectiveAttack(unit)).isEqualTo(baseAttack + 1);
```

#### BV19 — SPEED + SLOW: Effects stack (net = normal speed)

```java
applyBuff(unit, BuffType.SPEED);  // +1 action
applyBuff(unit, BuffType.SLOW);   // -1 action (delayed)
// Net effect: 1 action, no delay (they cancel out)
```

#### BV20 — Validation uses buffed stats, not base stats

---

### BA-Series — RuleEngine applyAction() + Buff Effects

#### BA1 — POWER buff: ATTACK deals base + 3 damage

```java
applyBuff(attacker, BuffType.POWER);
int originalHp = target.getHp();
applyAction(attack);
assertThat(target.getHp()).isEqualTo(originalHp - (baseAttack + 3));
```

#### BA2 — WEAKNESS buff: ATTACK deals base - 2 damage (min 0)

```java
applyBuff(attacker, BuffType.WEAKNESS);  // -2 ATK
applyAction(attack);
int expectedDamage = Math.max(0, baseAttack - 2);
assertThat(target.getHp()).isEqualTo(originalHp - expectedDamage);
```

#### BA3 — LIFE buff: No ongoing effect (instant only)

Verified by BL5.

#### BA4 — SPEED buff: Unit can act twice per round

```java
applyBuff(unit, BuffType.SPEED);
applyAction(move);
assertThat(unit.hasActed()).isFalse();  // Can still act
applyAction(attack);
assertThat(unit.hasActed()).isTrue();   // Now exhausted
```

#### BA5 — SLOW buff: Action executes next round

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);  // Declared, enters preparing
assertThat(target.getHp()).isEqualTo(originalHp);  // No damage yet

processRoundEnd();
processRoundStart();  // Preparing action executes
assertThat(target.getHp()).isEqualTo(originalHp - damage);
```

#### BA6 — SLOW buff: Preparing unit can be attacked

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);  // unit is preparing

// Enemy attacks the preparing unit
applyAction(enemyAttack);
assertThat(unit.getHp()).isEqualTo(originalHp - enemyDamage);
```

#### BA7 — SLOW buff: If preparing unit dies, action is cancelled

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);  // preparing
killUnit(unit);

processRoundEnd();
processRoundStart();
// Attack should NOT execute
assertThat(target.getHp()).isEqualTo(originalHp);
```

#### BA8 — SLOW buff: Preparing state persists across round

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);  // preparing
processRoundEnd();
assertThat(unit.isPreparing()).isTrue();  // Still preparing
```

#### BA9 — DESTROY_OBSTACLE removes obstacle from map

```java
applyBuff(unit, BuffType.POWER);
createObstacle(adjacentPosition);
applyAction(destroyObstacle);
assertThat(gameState.hasObstacleAt(adjacentPosition)).isFalse();
```

#### BA10 — DESTROY_OBSTACLE only works on adjacent tiles

```java
applyBuff(unit, BuffType.POWER);
createObstacle(farPosition);  // 2 tiles away
ValidationResult result = validateAction(destroyObstacle);
assertThat(result.isValid()).isFalse();
```

#### BA11 — Round end: All buffs duration--

```java
applyBuff(unit, BuffType.POWER);  // duration=2
processRoundEnd();
assertThat(getBuffDuration(unit, BuffType.POWER)).isEqualTo(1);
```

#### BA12 — Round end: Expired buffs removed

```java
applyBuff(unit, BuffType.POWER);  // duration=2
processRoundEnd();  // duration=1
processRoundEnd();  // duration=0, removed
assertThat(unit.hasBuff(BuffType.POWER)).isFalse();
```

#### BA13 — Round end: Buff expiration removes modifier effects

```java
applyBuff(unit, BuffType.POWER);  // +3 ATK
processRoundEnd();
processRoundEnd();  // POWER expired
assertThat(getEffectiveAttack(unit)).isEqualTo(baseAttack);
```

#### BA14 — Buff acquired mid-round has full duration

```java
// Round starts
applyBuff(unit, BuffType.POWER);  // duration=2
// Later in same round
processRoundEnd();
assertThat(getBuffDuration(unit, BuffType.POWER)).isEqualTo(1);
```

#### BA15 — SPEED buff: Both actions can be same type

```java
applyBuff(unit, BuffType.SPEED);
applyAction(attack);
applyAction(attack);  // Second attack is valid
assertThat(target.getHp()).isEqualTo(originalHp - (2 * damage));
```

#### BA16 — SPEED buff: Both actions can be different types

```java
applyBuff(unit, BuffType.SPEED);
applyAction(move);
applyAction(attack);
// Both executed successfully
```

#### BA17 — Buff effects apply in consistent order

```java
// POWER (+3) and WEAKNESS (-2) applied
// Net should always be +1, regardless of application order
applyBuff(unit, BuffType.WEAKNESS);
applyBuff(unit, BuffType.POWER);
assertThat(getEffectiveAttack(unit)).isEqualTo(baseAttack + 1);
```

#### BA18 — Buff from tile applies before attack in same action

Verified by BT8.

---

### BP-Series — POWER Buff Special Behavior

#### BP1 — POWER buff grants +3 ATK

Verified by BA1.

#### BP2 — POWER buff grants +1 HP on acquisition

Verified by BL6.

#### BP3 — POWER buff blocks MOVE_AND_ATTACK

Verified by BV2.

#### BP4 — POWER buff allows MOVE

#### BP5 — POWER buff allows ATTACK

#### BP6 — POWER buff enables DESTROY_OBSTACLE

Verified by BV5.

#### BP7 — POWER buff: Cannot DESTROY_OBSTACLE on empty tile

```java
applyBuff(unit, BuffType.POWER);
ValidationResult result = validateAction(destroyEmptyTile);
assertThat(result.isValid()).isFalse();
assertThat(result.getMessage()).contains("no obstacle");
```

#### BP8 — POWER buff: Cannot DESTROY_OBSTACLE on unit

```java
applyBuff(unit, BuffType.POWER);
ValidationResult result = validateAction(destroyUnitTile);
assertThat(result.isValid()).isFalse();
```

---

### BSP-Series — SPEED Buff Double Action

#### BSP1 — SPEED buff grants -1 ATK

```java
applyBuff(unit, BuffType.SPEED);
assertThat(getEffectiveAttack(unit)).isEqualTo(baseAttack - 1);
```

#### BSP2 — SPEED buff allows 2 actions per round

Verified by BA4.

#### BSP3 — SPEED buff: First action doesn't exhaust unit

```java
applyBuff(unit, BuffType.SPEED);
applyAction(move);
assertThat(unit.getRemainingActions()).isEqualTo(1);
```

#### BSP4 — SPEED buff: Second action exhausts unit

```java
applyBuff(unit, BuffType.SPEED);
applyAction(move);
applyAction(attack);
assertThat(unit.getRemainingActions()).isEqualTo(0);
assertThat(unit.hasActed()).isTrue();
```

#### BSP5 — SPEED buff: Actions can include MOVE_AND_ATTACK

```java
applyBuff(unit, BuffType.SPEED);
applyAction(moveAndAttack);  // Counts as 1 action
applyAction(move);            // Second action
// Both valid
```

#### BSP6 — SPEED buff: Actions reset each round

```java
applyBuff(unit, BuffType.SPEED);
applyAction(move);
applyAction(attack);
processRoundEnd();
processRoundStart();
assertThat(unit.getRemainingActions()).isEqualTo(2);  // Reset
```

#### BSP7 — SPEED buff expiration: Next round only 1 action

```java
applyBuff(unit, BuffType.SPEED);  // duration=2
processRoundEnd();
processRoundEnd();  // SPEED expired
assertThat(unit.getRemainingActions()).isEqualTo(1);  // Back to normal
```

#### BSP8 — SPEED buff: Cannot accumulate more than 2 actions

```java
applyBuff(unit, BuffType.SPEED);
applyBuff(unit, BuffType.SPEED);  // Second SPEED (refresh, not stack)
assertThat(unit.getRemainingActions()).isEqualTo(2);  // Still 2, not 3
```

#### BSP9 — SPEED + SLOW: Net effect is normal (1 action, no delay)

Verified by BV19.

#### BSP10 — SPEED buff: Both actions tracked independently

```java
applyBuff(unit, BuffType.SPEED);
GameState afterFirst = applyAction(move);
GameState afterSecond = applyAction(attack);
// Both states are distinct and valid
```

---

### BSL-Series — SLOW Buff Preparing State

#### BSL1 — SLOW buff: Declaring action enters preparing state

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);
assertThat(unit.getPreparingAction()).isEqualTo(attack);
assertThat(unit.isPreparing()).isTrue();
```

#### BSL2 — SLOW buff: Preparing action stored in unit state

```java
assertThat(unit.getPreparingAction().getType()).isEqualTo(ATTACK);
assertThat(unit.getPreparingAction().getTargetUnitId()).isEqualTo(targetId);
```

#### BSL3 — SLOW buff: Preparing action executes next round

Verified by BA5.

#### BSL4 — SLOW buff: Preparing unit can be damaged

Verified by BA6.

#### BSL5 — SLOW buff: Preparing unit death cancels action

Verified by BA7.

#### BSL6 — SLOW buff: Preparing persists across round

Verified by BA8.

#### BSL7 — SLOW buff: After execution, preparing state clears

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);  // preparing
processRoundEnd();
processRoundStart();  // attack executes
assertThat(unit.isPreparing()).isFalse();
assertThat(unit.getPreparingAction()).isNull();
```

#### BSL8 — SLOW buff: MOVE preparing state

```java
applyBuff(unit, BuffType.SLOW);
applyAction(move);  // preparing
processRoundEnd();
processRoundStart();  // move executes
assertThat(unit.getPosition()).isEqualTo(targetPosition);
```

#### BSL9 — SLOW buff: MOVE_AND_ATTACK preparing state

```java
applyBuff(unit, BuffType.SLOW);
applyAction(moveAndAttack);  // preparing
processRoundEnd();
processRoundStart();  // executes
assertThat(unit.getPosition()).isEqualTo(targetPosition);
assertThat(target.getHp()).isEqualTo(originalHp - damage);
```

#### BSL10 — SLOW buff: Target moves before execution

```java
applyBuff(unit, BuffType.SLOW);
applyAction(attack);  // preparing to attack target at position X
// Enemy moves target to position Y
applyAction(enemyMoveTarget);

processRoundEnd();
processRoundStart();
// Attack should fail or miss (target not at expected position)
assertThat(target.getHp()).isEqualTo(originalHp);  // No damage
```

#### BSL11 — SLOW buff: USE_SKILL preparing state

```java
applyBuff(hero, BuffType.SLOW);
applyAction(useSkill);  // preparing
// Skill executes next round
```

#### BSL12 — SLOW buff: Multiple units can be preparing

```java
applyBuff(unit1, BuffType.SLOW);
applyBuff(unit2, BuffType.SLOW);
applyAction(unit1Attack);
applyAction(unit2Attack);
assertThat(unit1.isPreparing()).isTrue();
assertThat(unit2.isPreparing()).isTrue();
```

---

### BBL-Series — BLEED Buff Damage Over Time

#### BBL1 — BLEED buff: 1 damage at round end

```java
applyBuff(unit, BuffType.BLEED);
int hpBefore = unit.getHp();
processRoundEnd();
assertThat(unit.getHp()).isEqualTo(hpBefore - 1);
```

#### BBL2 — BLEED buff: Damage applies before duration decrement

```java
applyBuff(unit, BuffType.BLEED);  // duration=2
unit.setHp(1);
processRoundEnd();
// BLEED damage should kill unit
assertThat(unit.isAlive()).isFalse();
```

#### BBL3 — BLEED buff: Can kill unit

Verified by BBL2.

#### BBL4 — BLEED buff: Dead unit takes no further BLEED damage

```java
unit.setHp(1);
applyBuff(unit, BuffType.BLEED);
processRoundEnd();  // Unit dies
processRoundEnd();  // No additional damage processing
assertThat(unit.getHp()).isLessThanOrEqualTo(0);
```

#### BBL5 — BLEED buff: Stacks with minion decay

```java
// Minion with BLEED
applyBuff(minion, BuffType.BLEED);
int hpBefore = minion.getHp();
processRoundEnd();
// BLEED (-1) + Decay (-1) = -2 HP
assertThat(minion.getHp()).isEqualTo(hpBefore - 2);
```

#### BBL6 — BLEED buff: Multiple BLEED stack damage

```java
applyBuff(unit, BuffType.BLEED);
applyBuff(unit, BuffType.BLEED);  // If stackable
int hpBefore = unit.getHp();
processRoundEnd();
assertThat(unit.getHp()).isEqualTo(hpBefore - 2);  // 2 BLEED = 2 damage
```

#### BBL7 — BLEED buff: Processed in unit ID order

```java
applyBuff(unit1, BuffType.BLEED);  // id = "p1_hero"
applyBuff(unit2, BuffType.BLEED);  // id = "p2_hero"
// Process order: p1_hero first, then p2_hero
```

#### BBL8 — BLEED buff: Duration tracked separately from damage

```java
applyBuff(unit, BuffType.BLEED);  // duration=2
processRoundEnd();  // damage applied, duration=1
processRoundEnd();  // damage applied, duration=0, buff removed
processRoundEnd();  // NO damage (buff gone)
```

---

### BST-Series — Buff Stacking Rules

#### BST1 — Same buff type: Duration refreshes (no stack)

```java
applyBuff(unit, BuffType.POWER);  // duration=2
processRoundEnd();                // duration=1
applyBuff(unit, BuffType.POWER);  // duration resets to 2
assertThat(getBuffDuration(unit, BuffType.POWER)).isEqualTo(2);
assertThat(countBuffs(unit, BuffType.POWER)).isEqualTo(1);  // Only 1 instance
```

#### BST2 — Different buff types: Both active

```java
applyBuff(unit, BuffType.POWER);
applyBuff(unit, BuffType.SPEED);
assertThat(unit.getBuffs()).hasSize(2);
```

#### BST3 — Conflicting modifiers: Additive calculation

```java
applyBuff(unit, BuffType.POWER);     // +3 ATK
applyBuff(unit, BuffType.WEAKNESS);  // -2 ATK
applyBuff(unit, BuffType.SPEED);     // -1 ATK
// Net: +3 - 2 - 1 = 0
assertThat(getEffectiveAttack(unit)).isEqualTo(baseAttack + 0);
```

#### BST4 — Instant HP effects: All apply

```java
// If unit somehow gets LIFE and WEAKNESS at same time
// LIFE: +3 HP, WEAKNESS: -1 HP
// Net instant: +2 HP
```

#### BST5 — SPEED + SLOW: Cancel out (net = normal)

```java
applyBuff(unit, BuffType.SPEED);  // +1 action
applyBuff(unit, BuffType.SLOW);   // delay
// Net: 1 action, no delay
assertThat(unit.getRemainingActions()).isEqualTo(1);
assertThat(unit.isPreparing()).isFalse();
```

#### BST6 — All 6 buffs can coexist

```java
for (BuffType type : BuffType.values()) {
    applyBuff(unit, type);
}
assertThat(unit.getBuffs()).hasSize(6);
```

#### BST7 — Buff removal: Only specified buff removed

```java
applyBuff(unit, BuffType.POWER);
applyBuff(unit, BuffType.SPEED);
removeBuff(unit, BuffType.POWER);
assertThat(unit.hasBuff(BuffType.POWER)).isFalse();
assertThat(unit.hasBuff(BuffType.SPEED)).isTrue();
```

#### BST8 — Buff expiration order: All at duration=0 removed together

```java
applyBuff(unit, BuffType.POWER);   // duration=2
applyBuff(unit, BuffType.SPEED);   // duration=2
processRoundEnd();  // both duration=1
processRoundEnd();  // both duration=0, both removed
assertThat(unit.getBuffs()).isEmpty();
```

---

### BC-Series — Backward Compatibility

#### BC1 — No buffs: V1 MOVE rules unchanged

#### BC2 — No buffs: V1 ATTACK rules unchanged

#### BC3 — No buffs: V2 moveRange/attackRange rules unchanged

#### BC4 — No buffs: RuleEngine.validateAction behaves exactly as V2

```java
// Compare V2 validation results with V3 (no buffs)
// Error messages must match exactly
```

#### BC5 — No buffs: RuleEngine.applyAction behaves exactly as V2

#### BC6 — Empty unitBuffs: No null pointer exceptions

```java
GameState state = createStateWithNoBuffs();
ValidationResult result = ruleEngine.validateAction(state, action);
// Should not throw
```

---

### BD-Series — Deterministic Ordering

#### BD1 — Buff processing order: By unit ID ascending

```java
// Units: "p1_archer", "p1_hero", "p2_tank"
// Processing order: p1_archer → p1_hero → p2_tank
```

#### BD2 — BLEED damage order: By unit ID ascending

#### BD3 — Buff expiration order: By unit ID ascending

#### BD4 — BuffTile trigger order: Deterministic by position

#### BD5 — Replay: Same initial state + actions = identical final state

```java
GameState state1 = replayActions(initialState, actions);
GameState state2 = replayActions(initialState, actions);
assertThat(state1).isEqualTo(state2);
```

#### BD6 — Serialization roundtrip preserves exact state

```java
String json1 = serialize(state);
GameState reconstructed = deserialize(json1);
String json2 = serialize(reconstructed);
assertThat(json1).isEqualTo(json2);
```

---

## 5. Test Data Fixtures

### 5.1 Standard Test State

```java
GameState createTestState() {
    return GameState.builder()
        .board(Board.create(5, 5))
        .currentPlayer(PLAYER_1)
        .units(Arrays.asList(
            createHero("p1_hero", PLAYER_1, pos(2, 0), 5),
            createMinion("p1_tank", PLAYER_1, TANK, pos(0, 0), 5),
            createMinion("p1_archer", PLAYER_1, ARCHER, pos(4, 0), 3),
            createHero("p2_hero", PLAYER_2, pos(2, 4), 5),
            createMinion("p2_tank", PLAYER_2, TANK, pos(0, 4), 5),
            createMinion("p2_assassin", PLAYER_2, ASSASSIN, pos(4, 4), 2)
        ))
        .unitBuffs(new HashMap<>())
        .buffTiles(new ArrayList<>())
        .obstacles(new ArrayList<>())
        .currentRound(1)
        .build();
}
```

### 5.2 Buff Factory Helpers

```java
BuffInstance createPowerBuff() {
    return BuffInstance.builder()
        .buffId(UUID.randomUUID().toString())
        .type(BuffType.POWER)
        .duration(2)
        .modifier(new BuffModifier(3, 0, 0))  // +3 ATK
        .flags(BuffFlags.builder().powerBuff(true).build())
        .instantHpBonus(1)
        .build();
}
```

---

## 6. Error Message Standards

### 6.1 Validation Error Messages

| Scenario | Expected Message |
|----------|------------------|
| POWER blocks MOVE_AND_ATTACK | "Unit cannot use MOVE_AND_ATTACK with Power buff" |
| No POWER for DESTROY_OBSTACLE | "DESTROY_OBSTACLE requires Power buff" |
| SLOW unit tries new action | "Unit is preparing an action and cannot declare another" |
| SPEED exhausted | "Unit has already used both actions this round" |
| Normal exhausted | "Unit has already acted this round" |

### 6.2 Apply Result Messages

| Scenario | Expected Message |
|----------|------------------|
| BLEED damage | "Unit took 1 poison damage from BLEED" |
| Buff expired | "POWER buff expired on unit" |
| Preparing action executed | "Delayed ATTACK executed" |
| Preparing action cancelled | "Delayed action cancelled: unit died" |

---

## 7. Implementation Checklist

- [ ] BuffType enum with 6 types
- [ ] BuffInstance with all fields
- [ ] BuffModifier and BuffFlags classes
- [ ] BuffFactory for creating standard buffs
- [ ] BuffTile class with position, type, duration
- [ ] Obstacle class
- [ ] DeathChoice handling in GameState
- [ ] Unit.isPreparing() and getPreparingAction()
- [ ] Unit.getRemainingActions() for SPEED
- [ ] RuleEngine validation for POWER/SPEED/SLOW
- [ ] RuleEngine apply for all buff effects
- [ ] Round-end processing: BLEED, decay, duration, expiration
- [ ] Serializer updates for buffs, tiles, obstacles
- [ ] Deterministic ordering throughout

---

## 8. Summary

This test plan defines:

- **141 mandatory test cases** across 13 series
- Complete coverage of all 6 buff types
- Buff Tile mechanics (spawn, trigger, expire)
- POWER special behavior (block MOVE_AND_ATTACK, destroy obstacles)
- SPEED double action system
- SLOW preparing state system
- BLEED damage over time
- Stacking and conflict resolution
- Full backward compatibility with V1/V2
- Deterministic replay behavior

Upon completing this test plan, the V3 Buff System implementation will be fully test-driven and correct.

---

*End of BUFF_SYSTEM_V3_TESTPLAN.md*
