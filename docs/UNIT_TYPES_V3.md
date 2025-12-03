# UNIT_TYPES_V3.md — Unit Definitions (Version 3)

## Overview

V3 introduces two unit categories:
- **Hero**: Player's main character (SPD-integrated)
- **Minion**: Support units (3 types, player drafts 2)

---

## 1. Minion Types

### 1.1 TANK
| Stat | Value |
|------|-------|
| HP | 5 |
| Attack | 1 |
| Move Range | 1 |
| Attack Range | 1 |

**Passive Ability: Guardian**
- If a friendly unit within **1 tile** (orthogonal) is attacked, TANK intercepts the damage
- TANK takes the damage instead of the original target
- Only triggers if TANK is alive
- Does not trigger if TANK itself is the target

```
Implementation Notes:
- On ATTACK resolution, check for adjacent friendly TANK
- If found, redirect damage to TANK
- Priority: If multiple TANKs adjacent, use deterministic order (lowest unit ID)
```

---

### 1.2 ARCHER
| Stat | Value |
|------|-------|
| HP | 3 |
| Attack | 1 |
| Move Range | 1 |
| Attack Range | 3 |

**Passive Ability: Long Shot**
- Base attack range is **3 tiles** (vs standard 1)
- Can attack without being adjacent
- Still orthogonal only (no diagonal)

```
Implementation Notes:
- attackRange = 3
- No minimum range (can attack adjacent enemies)
- Line-of-sight blocking by obstacles: TBD (V3.1?)
```

---

### 1.3 ASSASSIN
| Stat | Value |
|------|-------|
| HP | 2 |
| Attack | 2 |
| Move Range | 4 |
| Attack Range | 1 |

**Passive Ability: Swift**
- Move range is **4 tiles** per action
- High mobility, low survivability
- Can reposition dramatically each turn

```
Implementation Notes:
- moveRange = 4
- Glass cannon profile (high damage, low HP)
- Benefits most from positioning and BUFF tiles
```

---

## 2. Hero Type

### 2.1 Base Stats (All Heroes)
| Stat | Value |
|------|-------|
| HP | 5 |
| Attack | 1 |
| Move Range | 1 |
| Attack Range | 1 |

### 2.2 SPD Integration
- Hero appearance and identity based on player's **SPD character**
- Skill tree determined by SPD character class/type
- Player selects **1 skill** from their skill tree before match

### 2.3 Skill System
| Property | Value |
|----------|-------|
| Skills per match | 1 (selected pre-match) |
| Uses per round | 1 |
| Cooldown after use | 2 rounds |

```
Implementation Notes:
- Hero has reference to SPD character ID
- Skill list loaded from SPD skill tree
- Player picks 1 skill during draft phase
- Skill cooldown tracked per-hero
```

---

## 3. Stat Comparison Table

| Unit | HP | ATK | Move | Attack Range | Special |
|------|----|----|------|--------------|---------|
| Hero | 5 | 1 | 1 | 1 | 1 Skill (from SPD) |
| Tank | 5 | 1 | 1 | 1 | Guardian (intercept) |
| Archer | 3 | 1 | 1 | 3 | Long Shot |
| Assassin | 2 | 2 | 4 | 1 | Swift |

---

## 4. Unit Flags

### 4.1 Unit Category
```java
enum UnitCategory {
    HERO,    // Player's main character
    MINION   // Support units (TANK, ARCHER, ASSASSIN)
}
```

### 4.2 Minion Type
```java
enum MinionType {
    TANK,
    ARCHER,
    ASSASSIN
}
```

### 4.3 Unit Properties
```java
class Unit {
    String id;
    UnitCategory category;      // HERO or MINION
    MinionType minionType;      // null if HERO
    String spdCharacterId;      // null if MINION
    
    int hp;
    int maxHp;
    int attack;
    int moveRange;
    int attackRange;
    
    boolean alive;
    Position position;
    
    // Hero only
    String selectedSkillId;
    int skillCooldown;
}
```

---

## 5. Death Behavior

### 5.1 Hero Death
- **Game ends** — opponent wins
- No obstacle/BUFF tile choice

### 5.2 Minion Death
Owner chooses one:
| Choice | Result |
|--------|--------|
| SPAWN_OBSTACLE | Impassable terrain at death position |
| SPAWN_BUFF_TILE | Random BUFF tile (2 rounds, one-use) |

---

## 6. Attrition (Minions Only)

- At **end of each round**, all minions lose **1 HP**
- Heroes are exempt from decay
- This creates natural pressure to be aggressive

```
Round 1 end: Minions at HP - 1
Round 2 end: Minions at HP - 2
Round 3 end: Minions at HP - 3
...
TANK survives 5 rounds of pure decay
ARCHER survives 3 rounds
ASSASSIN survives 2 rounds
```

---

## 7. Passive Ability Details

### 7.1 Guardian (TANK)

**Trigger Condition:**
```
1. Enemy attacks a friendly unit
2. TANK is within 1 tile of the target (orthogonal)
3. TANK is alive
4. TANK is not the original target
```

**Effect:**
```
1. Damage redirected to TANK
2. Original target takes 0 damage
3. TANK takes full damage
```

**Edge Cases:**
- Multiple TANKs adjacent: Lowest ID intercepts
- TANK has STUN debuff: Cannot intercept (TBD)
- TANK at 1 HP: Still intercepts, dies

---

### 7.2 Long Shot (ARCHER)

**Implementation:**
```
attackRange = 3
// No special trigger, just extended range
```

**Validation:**
- Manhattan distance to target ≤ 3
- Orthogonal alignment required
- Line-of-sight: TBD (assume clear for V3.0)

---

### 7.3 Swift (ASSASSIN)

**Implementation:**
```
moveRange = 4
// No special trigger, just extended movement
```

**Path Validation:**
- Cannot pass through occupied tiles
- Cannot pass through obstacles
- Orthogonal movement only (no diagonal)

---

## 8. Implementation Checklist

- [ ] UnitCategory enum (HERO, MINION)
- [ ] MinionType enum (TANK, ARCHER, ASSASSIN)
- [ ] Hero with SPD character reference
- [ ] Skill selection and cooldown tracking
- [ ] TANK Guardian passive (damage redirect)
- [ ] ARCHER attackRange = 3
- [ ] ASSASSIN moveRange = 4
- [ ] Minion decay system
- [ ] Death choice handling

---

# End of UNIT_TYPES_V3.md
