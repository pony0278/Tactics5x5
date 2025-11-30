# BUFF_SYSTEM_V1.md — 5x5 Tactics Buff System (Version 1)

## 1. Scope of This Document

This document defines the Buff System V1 for 5x5 Tactics.
It specifies:

- Buff data model
- Buff lifecycle
- Buff stacking rules
- Buff application timing
- How buffs affect game rules (movement, attack, damage, restrictions)
- Interaction with GameState, Unit, RuleEngine, and Serializer
- Deterministic behavior and ordering

This specification must be strictly followed by:

- Buff model implementation
- Buff serialization/deserialization
- RuleEngine buff hooks
- GAME_RULES future versions requiring buffs
- Validation and apply pipelines

This document must not contradict:

- TECH_ARCH
- ENGINE_SKELETON
- UNIT_TYPES_V1
- GAME_RULES_V1/V2

## 2. Overview

A Buff is a temporary effect applied to a Unit.
Buffs can:

- Modify stats (HP, attack, moveRange, attackRange)
- Restrict actions (e.g., stun)
- Cause effects each turn (e.g., poison)
- Expire after a certain number of turns

Buffs are deterministic and stored entirely in GameState, ensuring:

- Replay safety
- Server-authoritative consistency
- Pure functional transitions

## 3. Buff Model (Core Data Structure)

A buff instance is represented as:

```
BuffInstance {
    String buffId;           // e.g., "RAGE", "HASTE", "POISON"
    String sourceUnitId;     // Optional: unit that applied it (nullable)
    int duration;            // Number of turns remaining (>= 1)
    boolean stackable;       // Whether multiple instances can coexist
    BuffModifier modifiers;  // Numeric stat changes (see below)
    BuffFlags flags;         // Behavioral effects (see below)
}
```

### 3.1 BuffModifier

Pure numeric modifiers applied on top of base stats:

```
BuffModifier {
    int bonusHp;          // Applied immediately if > 0, ignored if target is dead
    int bonusAttack;
    int bonusMoveRange;
    int bonusAttackRange;
}
```

### 3.2 BuffFlags

Binary behavioral effects:

```
BuffFlags {
    boolean stunned;      // Cannot MOVE / ATTACK / MOVE_AND_ATTACK
    boolean rooted;       // Cannot MOVE (but can ATTACK)
    boolean silenced;     // Cannot use skills (future extension)
    boolean taunted;      // Must attack a specific unit if possible (future)
    boolean poison;       // Takes damage at turn end
}
```

> V1 only requires stunned, rooted, and poison.

## 4. Buff Storage (GameState Integration)

A new field must be added to GameState:

```
Map<String /*unitId*/, List<BuffInstance>> unitBuffs
```

Rules:

- Key = unitId
- Value = list of active buff instances
- Empty list = no buffs
- Units that die still keep buffs but they may not take effect unless specified
- Buff lists are immutable as part of GameState's immutable structure.

## 5. Buff Lifecycle

Buff lifecycle consists of:

1. OnApply
2. OnTurnStart
3. During Validation
4. During ApplyAction
5. OnTurnEnd
6. Expiration

### 5.1 OnApply

When a buff is added:

- A new BuffInstance is created
- If stackable = false, any existing instance with the same buffId is replaced
- If bonusHp > 0, apply immediately as healing (cannot resurrect)

### 5.2 OnTurnStart

Triggered only for the active player's units.

Effects:

- Poison: take poison damage (cannot reduce below 0)
- Stun: remains active (prevents action)
- Root: remains active

### 5.3 Validation Phase Influence

The following affects RuleEngine.validateAction():

| Flag | Effect |
|------|--------|
| stunned | All actions except END_TURN invalid |
| rooted | MOVE / MOVE_AND_ATTACK MOVE-step invalid |
| bonusMoveRange | increases movement range |
| bonusAttackRange | increases attack range |

### 5.4 ApplyAction Phase Influence

Buffs may modify:

- Actual attack damage
- Movement distance
- Post-action HP changes
- Trigger on-kill effects (future extension)

### 5.5 OnTurnEnd

After the acting player's turn fully completes:

- Reduce duration of all buffs on all units by 1
- Remove buffs with duration = 0
- Apply end-of-turn poison damage
- If poison damage kills the unit → mark dead

### 5.6 Expiration

When duration reaches 0:

- Buff instance is removed
- No "OnRemove" effects in V1 (reserved for V2)

## 6. Serialization Requirements

Buffs must be included in JSON output.

### 6.1 GameState → Map

```json
"unitBuffs": {
    "u1_p1": [
        {
            "buffId": "RAGE",
            "sourceUnitId": "u2_p1",
            "duration": 1,
            "stackable": false,
            "modifiers": {
                "bonusHp": 0,
                "bonusAttack": 2,
                "bonusMoveRange": 0,
                "bonusAttackRange": 0
            },
            "flags": {
                "stunned": false,
                "rooted": false,
                "silenced": false,
                "taunted": false,
                "poison": false
            }
        }
    ]
}
```

### 6.2 Map → GameState

- All fields must be restored exactly.
- If unknown fields appear → ignore (forward compatibility).

## 7. RuleEngine Integration

The RuleEngine must support buffs at the following points:

### 7.1 Validation Modifications (V2 + Buff Extensions)

- MOVE range = baseMoveRange + sum(bonusMoveRange)
- ATTACK range = baseAttackRange + sum(bonusAttackRange)
- If stunned → any action except END_TURN returns invalid
- If rooted → MOVE portion invalid

### 7.2 ApplyAction Modifications

- Damage = (baseAttack + bonusAttack)
- If poison → apply hp reduction at OnTurnEnd
- Buff duration reduces after applyAction turn-end
- Remove expired buffs

### 7.3 Determinism

Buffs always resolve in deterministic order:

- For each unitId sorted lexicographically
- For each buff instance in insertion order

## 8. Buff Definitions (V1 Library)

V1 includes five predefined buff types.

### 8.1 RAGE

- **Effect:** +2 attack
- **Duration:** 1 turn
- **Stackable:** false
- **Flags:** none

### 8.2 HASTE

- **Effect:** +1 moveRange
- **Duration:** 1 turn
- **Stackable:** false
- **Flags:** none

### 8.3 MARKED

- **Effect:** Target takes +1 damage when attacked (future use)
- **Duration:** 1 turn
- **Stackable:** false
- **Flags:** none

### 8.4 POISON

- **Effect:** -1 HP at end of each turn
- **Duration:** 2 turns
- **Flags:** poison = true
- **Stackable:** true

### 8.5 STUN

- **Effect:** Cannot MOVE / ATTACK / MOVE_AND_ATTACK
- **Duration:** 1 turn
- **Flags:** stunned = true
- **Stackable:** false

## 9. Timing Summary

| Phase | Buff Effects |
|-------|--------------|
| OnApply | Immediate stat changes (bonusHp) |
| Turn Start | Poison tick (optional V1), Restriction flags apply |
| Validation | stunned/rooted & range modifiers |
| ApplyAction | bonusAttack, bonusMoveRange, bonusAttackRange |
| Turn End | Duration--, Poison damage, expiration |

## 10. Forward Compatibility

V2+ may add:

- OnHit effects
- OnKill effects
- OnReceiveDamage effects
- Damage-over-time with stacking rules
- Aura buffs
- Zone buffs (synergy with TERRAIN_SYSTEM_V1)
- Trigger conditions

This V1 spec is intentionally small and safe.

## 11. Summary

BUFF_SYSTEM_V1 introduces:

- Fully deterministic buff model
- Unit-attached buff list
- Lifecycle phases
- Stat modifiers & behavioral restrictions
- Integration points for RuleEngine
- Serialization contract
- Five core buffs

This provides the foundation for expanding the tactical depth of 5x5 Tactics and future integrations with SPD mechanics.

---

*End of BUFF_SYSTEM_V1.md*
