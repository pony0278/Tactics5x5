# BUFF_SYSTEM_V3.md — BUFF Definitions (Version 3)

## Overview

V3 BUFF system features:
- 6 BUFF types (3 positive, 3 negative)
- Fixed duration: **2 rounds** for all BUFFs
- Can apply to both Heroes and Minions
- Acquired via BUFF tiles (spawned on minion death)

---

## 1. BUFF Categories

| Category | Count | Types |
|----------|-------|-------|
| Positive | 3 | POWER, LIFE, SPEED |
| Negative | 3 | WEAKNESS, BLEED, SLOW |

---

## 2. Positive BUFFs

### 2.1 POWER (力量)
| Property | Value |
|----------|-------|
| Attack Bonus | +3 |
| HP Bonus | +1 (on acquire) |
| Duration | 2 rounds |

**Special Effects:**
- **Cannot attack after moving** — MOVE_AND_ATTACK disabled
- **Can destroy obstacles** — Unit may target obstacle tiles to remove them

```
Implementation Notes:
- flags.powerBuff = true
- modifier.bonusAttack = +3
- On acquire: unit.hp += 1 (one-time)
- Validation: If hasPowerBuff, reject MOVE_AND_ATTACK
- New action: DESTROY_OBSTACLE (target adjacent obstacle)
```

---

### 2.2 LIFE (生命)
| Property | Value |
|----------|-------|
| HP Bonus | +3 (on acquire) |
| Duration | 2 rounds |

**Special Effects:**
- None (pure stat boost)

```
Implementation Notes:
- flags.lifeBuff = true
- On acquire: unit.hp += 3 (one-time, can exceed maxHp)
- No ongoing effects
- Duration still tracked for consistency
```

---

### 2.3 SPEED (速度)
| Property | Value |
|----------|-------|
| Attack Penalty | -1 |
| Extra Action | +1 action per round |
| Duration | 2 rounds |

**Special Effects:**
- Unit may act **twice** per round instead of once
- Attack power reduced by 1 (minimum 0)

```
Implementation Notes:
- flags.speedBuff = true
- modifier.bonusAttack = -1
- Unit gets 2 action slots per round
- Both actions can be any valid action type
```

---

## 3. Negative BUFFs (Debuffs)

### 3.1 WEAKNESS (無力)
| Property | Value |
|----------|-------|
| Attack Penalty | -2 |
| HP Penalty | -1 (on acquire) |
| Duration | 2 rounds |

**Special Effects:**
- Immediate HP loss on application

```
Implementation Notes:
- flags.weaknessBuff = true
- modifier.bonusAttack = -2
- On acquire: unit.hp -= 1
- If hp <= 0: unit dies immediately
```

---

### 3.2 BLEED (失血)
| Property | Value |
|----------|-------|
| HP Loss | -1 per round |
| Duration | 2 rounds |

**Special Effects:**
- Damage over time (DoT)
- Applied at end of each round

```
Implementation Notes:
- flags.bleedBuff = true (same as flags.poison conceptually)
- At round end: unit.hp -= 1
- Stacks with minion decay (minions lose 2 HP/round with BLEED)
```

---

### 3.3 SLOW (慢速)
| Property | Value |
|----------|-------|
| Action Delay | All actions require +1 round |
| Duration | 2 rounds |

**Special Effects:**
- Any action takes **2 rounds** to complete
- Round 1: Declare action (unit is "preparing")
- Round 2: Action executes

```
Implementation Notes:
- flags.slowBuff = true
- Unit enters "preparing" state when action chosen
- Action resolves next round
- If unit dies while preparing: action cancelled
- Opposite of SPEED buff
```

---

## 4. BUFF Stat Summary

| BUFF | ATK | HP (instant) | HP/round | Special |
|------|-----|--------------|----------|---------|
| POWER | +3 | +1 | — | No MOVE_AND_ATTACK, can destroy obstacles |
| LIFE | — | +3 | — | None |
| SPEED | -1 | — | — | Double action |
| WEAKNESS | -2 | -1 | — | None |
| BLEED | — | — | -1 | DoT |
| SLOW | — | — | — | Actions delayed 1 round |

---

## 5. BUFF Acquisition

### 5.1 BUFF Tiles
- Created when friendly minion dies (player choice)
- Contains **random** BUFF (50% positive, 50% negative)
- Duration: **2 rounds** on the map
- Trigger: **Once** — first unit to step on it

### 5.2 Trigger Mechanics
```
1. Unit moves onto BUFF tile
2. Random BUFF selected (or pre-determined at spawn)
3. BUFF applied to unit
4. BUFF tile removed from map
```

### 5.3 Randomization
```java
// Equal probability distribution
BUFF_POOL = [POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW]
selectedBuff = BUFF_POOL[random(0, 5)]
```

Or weighted:
```java
// 50% positive, 50% negative
if (random() < 0.5) {
    selectedBuff = randomFrom([POWER, LIFE, SPEED]);
} else {
    selectedBuff = randomFrom([WEAKNESS, BLEED, SLOW]);
}
```

---

## 6. BUFF Duration & Expiration

### 6.1 Duration Rules
- All BUFFs last exactly **2 rounds**
- Duration decrements at **end of round**
- BUFF removed when duration reaches 0

### 6.2 Duration Processing
```
Round N (BUFF acquired, duration = 2):
  - BUFF effects active
  
Round N end:
  - duration = duration - 1  // now 1
  
Round N+1:
  - BUFF effects still active
  
Round N+1 end:
  - duration = duration - 1  // now 0
  - BUFF removed
```

### 6.3 Instant Effects
Some BUFFs have one-time effects on acquisition:
| BUFF | Instant Effect |
|------|----------------|
| POWER | +1 HP |
| LIFE | +3 HP |
| WEAKNESS | -1 HP |

These are applied immediately and persist even after BUFF expires.

---

## 7. BUFF Stacking

### 7.1 Same BUFF Type
- **Does NOT stack** — refresh duration instead
- If unit has POWER and steps on another POWER tile: duration resets to 2

### 7.2 Different BUFF Types
- **Stacks fully** — unit can have multiple BUFFs
- Effects are additive

### 7.3 Conflicting BUFFs
| Conflict | Resolution |
|----------|------------|
| SPEED + SLOW | Both active — net effect: normal action speed |
| POWER + WEAKNESS | Both active — ATK: +3 - 2 = +1 |

```
Implementation Notes:
- Aggregate all modifiers: sum bonusAttack from all BUFFs
- Aggregate all flags: OR all boolean flags
- Let conflicting effects cancel mathematically
```

---

## 8. BUFF Tiles on Map

### 8.1 Tile Properties
```java
class BuffTile {
    Position position;
    BuffType buffType;      // May be null if random-on-trigger
    int duration;           // Rounds remaining on map (starts at 2)
    boolean triggered;      // Once true, tile is removed
}
```

### 8.2 Tile Lifecycle
```
1. Minion dies, owner chooses SPAWN_BUFF_TILE
2. BuffTile created at death position
3. Tile persists for 2 rounds OR until triggered
4. If unit steps on tile: apply BUFF, remove tile
5. If 2 rounds pass: tile expires, removed
```

### 8.3 Tile Interaction
- Units can **move through** BUFF tiles
- BUFF triggers when unit **ends movement** on tile
- Cannot trigger BUFF tile mid-movement (only final position)

---

## 9. Implementation Data Structures

### 9.1 BuffType Enum
```java
enum BuffType {
    // Positive
    POWER,
    LIFE,
    SPEED,
    
    // Negative
    WEAKNESS,
    BLEED,
    SLOW
}
```

### 9.2 BuffInstance
```java
class BuffInstance {
    String id;
    BuffType type;
    int duration;           // Rounds remaining
    BuffModifier modifier;  // Stat changes
    BuffFlags flags;        // Boolean effects
}
```

### 9.3 BuffModifier
```java
class BuffModifier {
    int bonusAttack;        // Can be negative
    int bonusMoveRange;     // For future use
    int bonusAttackRange;   // For future use
}
```

### 9.4 BuffFlags
```java
class BuffFlags {
    boolean powerBuff;      // Blocks MOVE_AND_ATTACK, enables obstacle destroy
    boolean speedBuff;      // Grants extra action
    boolean slowBuff;       // Delays actions
    boolean bleedBuff;      // DoT at round end
    // stunned, rooted from V1 still available
}
```

---

## 10. Validation Impact

### 10.1 POWER BUFF
```
If unit has POWER buff:
  - MOVE_AND_ATTACK → INVALID ("Cannot attack after moving with Power buff")
  - DESTROY_OBSTACLE → VALID (new action type)
```

### 10.2 SLOW BUFF
```
If unit has SLOW buff:
  - All actions enter "preparing" state
  - Validation must check if unit is already preparing
  - If preparing: only valid action is WAIT or forced execution
```

### 10.3 Attack Calculation
```java
int effectiveAttack = unit.getAttack() + sumOf(buff.modifier.bonusAttack);
effectiveAttack = Math.max(0, effectiveAttack);  // Floor at 0
```

---

## 11. Apply Action Impact

### 11.1 Round End Processing
```java
void processRoundEnd(GameState state) {
    // 1. Apply BLEED damage
    for (Unit unit : state.getUnits()) {
        if (hasBleedBuff(unit)) {
            unit.hp -= 1;
            checkDeath(unit);
        }
    }
    
    // 2. Apply minion decay
    for (Unit unit : state.getUnits()) {
        if (unit.category == MINION) {
            unit.hp -= 1;
            checkDeath(unit);
        }
    }
    
    // 3. Decrement BUFF durations
    for (Unit unit : state.getUnits()) {
        for (BuffInstance buff : unit.buffs) {
            buff.duration -= 1;
        }
        unit.buffs.removeIf(b -> b.duration <= 0);
    }
    
    // 4. Decrement BUFF tile durations
    for (BuffTile tile : state.getBuffTiles()) {
        tile.duration -= 1;
    }
    state.buffTiles.removeIf(t -> t.duration <= 0);
}
```

---

## 12. Implementation Checklist

- [ ] BuffType enum with 6 types
- [ ] BuffInstance with duration tracking
- [ ] BuffModifier for stat changes
- [ ] BuffFlags for boolean effects
- [ ] BUFF tile spawning on minion death
- [ ] BUFF tile trigger on unit movement
- [ ] BUFF tile expiration after 2 rounds
- [ ] POWER: +3 ATK, +1 HP, block MOVE_AND_ATTACK, obstacle destroy
- [ ] LIFE: +3 HP instant
- [ ] SPEED: -1 ATK, double action
- [ ] WEAKNESS: -2 ATK, -1 HP instant
- [ ] BLEED: -1 HP per round
- [ ] SLOW: action delay system
- [ ] BUFF stacking rules
- [ ] Round-end BUFF processing

---

# End of BUFF_SYSTEM_V3.md
