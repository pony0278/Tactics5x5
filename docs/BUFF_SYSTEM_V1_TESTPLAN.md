# BUFF_SYSTEM_V1_TESTPLAN.md — 5x5 Tactics Buff System Test Plan (Version 1)

## 1. Scope

This document defines the test cases required for the Buff System V1.

It covers:

- Buff model correctness
- Buff serialization & deserialization
- Buff lifecycle behavior
- Buff effects on movement, attack, and action restrictions
- Buff duration reduction
- Poison tick
- Interaction with RuleEngine validation
- Interaction with RuleEngine apply
- Backward compatibility with GAME_RULES_V1 & V2
- Deterministic ordering

Not covered:

- Terrain interactions
- Skill-triggered buffs (future)
- Buff UI
- Multi-source buff merging
- Aura buffs
- Item-based buffs

## 2. Test Series Overview

| Series | Purpose |
|--------|---------|
| BM-Series | Buff model correctness |
| BS-Series | Buff serialization |
| BL-Series | Buff lifecycle (duration, removal) |
| BV-Series | Buff effects on validation |
| BA-Series | Buff effects on applyAction |
| BP-Series | Poison & end-of-turn behavior |
| BC-Series | Backward compatibility with V1/V2 rules |
| BD-Series | Deterministic ordering & stable behavior |

## 3. Test Series (Detailed)

### BM-Series — Buff Model Tests

#### BM1 — BuffInstance stores all fields correctly

Given a constructed BuffInstance,
assert that all fields (buffId, sourceUnitId, duration, stackable, modifiers, flags)
are preserved via getters.

#### BM2 — BuffModifier correctly stores numeric modifiers

Test bonusHp / bonusAttack / bonusMoveRange / bonusAttackRange.

#### BM3 — BuffFlags correctly stores behavioral flags

Test stunned, rooted, poison.

#### BM4 — Non-stackable buffs replace existing instance

If stackable = false, applying a second buff of same buffId replaces the first one.

#### BM5 — Stackable buffs coexist

If stackable = true, applying multiple instances keeps both.

---

### BS-Series — Buff Serialization Tests

#### BS1 — GameStateSerializer.toJsonMap includes unitBuffs

Serialized Map contains a "unitBuffs" key.

#### BS2 — Unit buffs serialize all fields

Each BuffInstance serializes:

- buffId
- sourceUnitId
- duration
- stackable
- modifiers{}
- flags{}

#### BS3 — fromJsonMap reconstructs buff list exactly

Roundtrip:
GameState → Map → GameState
buffs must match instance-by-instance.

#### BS4 — Empty buff list serializes & deserializes correctly

#### BS5 — Unknown buff fields in JSON are ignored (forward compatibility)

---

### BL-Series — Buff Lifecycle Tests

#### BL1 — duration reduces by 1 at end of each turn

After a player completes a turn (END_TURN or MOVE_AND_ATTACK),
all buffs duration--.

#### BL2 — Buff with duration=1 is removed after turn end

#### BL3 — Multiple buffs decrease durations together

#### BL4 — OnApply: bonusHp applies immediately

If buff.modifiers.bonusHp = +5,
target.hp increases instantly unless the unit is dead.

#### BL5 — OnApply bonusHp does not resurrect a dead unit

#### BL6 — OnTurnStart poison ticks (optional positioning depending on design)

If poison flag active → unit hp-- at turn start or turn end (based on spec; V1 uses end-of-turn).

#### BL7 — Removing buffs does not affect other buffs on the unit

---

### BV-Series — RuleEngine validateAction() + Buff Effects

These tests verify how buffs modify legal actions.

#### BV1 — stunned units cannot MOVE

validateAction must return invalid.

#### BV2 — stunned units cannot ATTACK

validateAction must return invalid.

#### BV3 — stunned units cannot MOVE_AND_ATTACK

Same as above.

#### BV4 — stunned units can END_TURN

validateAction must return valid.

#### BV5 — rooted units cannot MOVE

validateAction invalid.

#### BV6 — rooted units cannot perform MOVE_AND_ATTACK movement step

Movement invalid even if attack would be valid.

#### BV7 — rooted units CAN still ATTACK if target in range

#### BV8 — bonusMoveRange increases allowed movement range

E.g., base=1, buff +1 → moveRange=2

#### BV9 — bonusAttackRange increases ATTACK range

#### BV10 — Poison does NOT prevent any actions

Poison only affects HP at turn end.

#### BV11 — Multiple bonusMoveRange buffs are additive

#### BV12 — Multiple bonusAttackRange buffs are additive

#### BV13 — Validation must reflect buff-modified stats without altering base stats

---

### BA-Series — RuleEngine applyAction() + Buff Effects

These tests verify buffs applied during applyAction, not validation.

#### BA1 — bonusAttack increases ATTACK damage

Damage = base + bonusAttack.

#### BA2 — RAGE buff (+2 ATK) applied correctly

Target hp reduces accordingly.

#### BA3 — bonusMoveRange does NOT modify actual position beyond allowed range

Behavior is same as validation, but double-check apply path.

#### BA4 — bonusAttackRange does NOT affect apply beyond validation

#### BA5 — OnTurnEnd poison deals damage

Unit hp reduces by poison amount.

#### BA6 — Poison damage can kill the unit

#### BA7 — Buff expiration happens AFTER poison damage in the same turn

Order:

1. Poison damage
2. duration--
3. Remove expired

#### BA8 — Stun does not interfere with END_TURN applyAction

---

### BP-Series — Poison & End-of-Turn Tests

#### BP1 — Single poison tick deals exactly 1 damage

#### BP2 — Multiple poison buffs stack damage

If stackable=true, two poison buffs → 2 damage per turn.

#### BP3 — Poison kills the unit at turn end

Verify hp <= 0 → alive=false.

#### BP4 — Poison damage does not occur for already-dead unit

#### BP5 — Poison duration reduces exactly like other buffs

---

### BC-Series — Backward Compatibility

These tests ensure Buff System does NOT break existing V1/V2 behavior unless buffs are present.

#### BC1 — No buffs: V1 MOVE rules unchanged

#### BC2 — No buffs: V1 ATTACK rules unchanged

#### BC3 — No buffs: V2 moveRange/attackRange rules unchanged

#### BC4 — No buffs: RuleEngine.validateAction behaves exactly the same as before Buff System

#### BC5 — No buffs: RuleEngine.applyAction behaves exactly the same as before Buff System

These must compare specific expected error messages from V1/V2.

---

### BD-Series — Deterministic Ordering

#### BD1 — Buffs resolve in deterministic order (unitId sorted ascending)

#### BD2 — Buff expiration order deterministic

#### BD3 — Poison ticks applied in deterministic order

#### BD4 — Replay: Given same initial GameState + action sequence, GameState hashes identical

## 4. Test Class Structure (JUnit 5 Recommendation)

Recommended file:

```
src/test/java/com/tactics/engine/buff/BuffSystemTest.java
```

Structure:

```java
@Nested class BM_ModelTests { ... }
@Nested class BS_SerializationTests { ... }
@Nested class BL_LifecycleTests { ... }
@Nested class BV_ValidationTests { ... }
@Nested class BA_ApplyTests { ... }
@Nested class BP_PoisonTests { ... }
@Nested class BC_CompatibilityTests { ... }
@Nested class BD_DeterminismTests { ... }
```

## 5. Summary

This test plan defines:

- ≥ 50 mandatory test cases
- Complete coverage of Buff System V1
- Exact hooks for RuleEngine
- Full lifecycle coverage
- Deterministic replay behavior
- Strict backward compatibility
- Serializer correctness
- Stackable & non-stackable logic

Upon completing this test plan, implementation in the Engine becomes straightforward, correct, and fully test-driven.

---

*End of BUFF_SYSTEM_V1_TESTPLAN.md*
