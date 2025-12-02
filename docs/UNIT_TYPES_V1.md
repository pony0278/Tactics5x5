# UNIT_TYPES_V1.md — Unit Types Specification (Version 1)

## 1. Purpose

This document defines the **Version 1 unit type system** for the 5x5 Tactics engine.

Goals:

- Introduce distinct unit archetypes
- Standardize base stats for each type
- Provide a forward-compatible foundation for future mechanics:
  - Buffs / debuffs
  - Special abilities
  - Terrain effects
  - SPD system integration
  - Ranged attacks (future ruleset)

Version 1 introduces **three base unit types**:

- **SWORDSMAN**
- **ARCHER**
- **TANK**

Unit types affect initial stats only.
**GAME_RULES_V1 does not change.**

---

## 2. Data Model

A `UnitType` defines the following attributes:

| Field | Description | Notes |
|-------|-------------|-------|
| `id` | Unique type identifier (e.g., `"SWORDSMAN"`) | String |
| `maxHp` | Maximum HP for units of this type | numeric |
| `attack` | Base attack damage | numeric |
| `moveRange` | Number of tiles this unit can move | For V1: always = 1 |
| `attackRange` | Number of tiles this unit can attack | For V1: always = 1 |

### 2.1 Compatibility Notes

- **GAME_RULES_V1** enforces:
  - Move distance = 1
  - Attack distance = 1
→ Fields `moveRange` and `attackRange` are **inactive for V1**, but included for V2 expansion.

- No engine change is required in V1.
- Optional: Unit may store `unitTypeId` if added to ENGINE_SKELETON_V1.

---

## 3. Base Unit Types (Version 1)

### 3.1 SWORDSMAN

```
id: "SWORDSMAN"
maxHp: 10
attack: 3
moveRange: 1
attackRange: 1
role: Balanced melee fighter
description: Standard frontline soldier with reliable stats.
```

### 3.2 ARCHER

```
id: "ARCHER"
maxHp: 8
attack: 3
moveRange: 1
attackRange: 2  // Future expansion (inactive in V1)
role: Ranged attacker
description: Fragile but excels at long-range pressure.
```

### 3.3 TANK

```
id: "TANK"
maxHp: 16
attack: 2
moveRange: 1
attackRange: 1
role: Durable frontline unit
description: High HP and strong survivability, low damage output.
```

---

## 4. Default Starting Lineup

The following default units are recommended for initial gameplay:

### Player 1

| Unit ID | Type | Position |
|---------|------|----------|
| `u1_p1` | SWORDSMAN | (1, 0) |
| `u2_p1` | ARCHER | (3, 0) |

### Player 2

| Unit ID | Type | Position |
|---------|------|----------|
| `u1_p2` | SWORDSMAN | (1, 4) |
| `u2_p2` | ARCHER | (3, 4) |

### Notes

- This layout ensures symmetry and tactical diversity.
- TANK is defined but intentionally **not used** in the default setup.
- The lineup is intended for early prototyping and V1 gameplay balance.

---

## 5. Integration with the Engine (V1)

### 5.1 No changes required to GAME_RULES_V1

- Units behave exactly as defined in GAME_RULES_V1.
- moveRange and attackRange **do not override** rules until GAME_RULES_V2.

### 5.2 Optional extension to the Unit model

If desired, ENGINE_SKELETON_V1 may be updated with:

```
unitTypeId: String
maxHp: int
```

This allows:

- Serialization of unit types
- Frontend to display type-specific visuals
- Buff / skill systems to reference type rules
- SPD compatibility (unit class system)

### 5.3 Initialization

Unit types are applied when constructing the initial `GameState`:

- `hp = maxHp`
- `attack = attack (from UnitType)`
- Movement / attack ranges ignored until V2

No further logic needed.

---

## 6. Future Expansion Roadmap

### 6.1 GAME_RULES_V2 (Ranged & movement rules)

- Use `moveRange` and `attackRange`
- Archer can attack from 2 tiles away
- Tank may have minimum-range vulnerability

### 6.2 BUFF_SYSTEM_V1

- Temporary attack buffs
- Poison / fire DoT
- Defense bonus effects
- Duration + source tracking

### 6.3 TERRAIN_SYSTEM_V1

- High ground → +1 attack
- Forest → block ranged attacks
- Traps → damage when stepped on

### 6.4 SPD / Tactics Shared Core

These fields become shared between SPD and Tactics:

- Unit type id
- Stats
- Buffs
- Effects
- Actions

---

## 7. Summary

UNIT_TYPES_V1 defines:

- Three initial unit types with clean base stats
- A forward-compatible structure ready for:
  - Ranged rules
  - Buff systems
  - Terrain
  - SPD integration
- No breaking changes to the V1 engine or rules

This document serves as the foundation for **UNIT_TYPES_V2 and GAME_RULES_V2**.

---

# End of UNIT_TYPES_V1.md
