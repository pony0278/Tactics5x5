# GAME_RULES_V3.md — 5×5 Tactics Game Rules (Version 3)

## Overview

V3 is a major gameplay revision focusing on:
- Hero-centric victory condition
- Minion selection phase (draft 2 from 3)
- Death mechanics (obstacle or BUFF tile spawning)
- Time pressure and attrition systems
- SPD character integration

---

## 1. Core Game Setup

### 1.1 Board
- 5×5 grid
- No map shrinking (board is already compact)

### 1.2 Team Composition
Each player fields:
| Unit Type | Count | Notes |
|-----------|-------|-------|
| Hero | 1 | Based on player's SPD character |
| Minion | 2 | Selected from 3 options before match |

### 1.3 Pre-Match Draft Phase
Before battle begins:
1. Each player is presented with 3 minion types: **ARCHER**, **TANK**, **ASSASSIN**
2. Player selects **2 of 3** to deploy
3. Selection is hidden from opponent until match starts

---

## 2. Turn Structure

### 2.1 Turn Order
- **Alternating unit actions**: Player A moves one unit → Player B moves one unit → repeat
- Each unit may act once per round

### 2.2 Time Limit
- **10 seconds** per action decision
- Timeout penalty: Acting player loses **1 HP** (applied to hero? or acting unit? — clarify in implementation)

### 2.3 Action Options
Each unit per turn may perform ONE of:
| Action | Description |
|--------|-------------|
| MOVE | Move up to unit's `moveRange` |
| ATTACK | Attack enemy within `attackRange` |
| MOVE_AND_ATTACK | Move, then attack if adjacent (distance ≤ 1) |
| MOVE_AND_SKILL | Move, then use skill (heroes only) |
| USE_SKILL | Use skill without moving (heroes only) |
| END_TURN | Pass action |

### 2.4 MOVE_AND_ATTACK Rule
- After moving, if an enemy is within **1 tile** (orthogonal), the unit may immediately attack
- This counts as a single action

---

## 3. Attrition Mechanics

### 3.1 Minion Decay
- At the **end of each round**, all minions lose **1 HP**
- This applies to both players' minions
- Heroes are NOT affected by decay

### 3.2 Late-Game Pressure
- Starting from **Round 8**, both players lose **1 HP per round**
- Applied to: Hero? All units? (clarify in implementation)

---

## 4. Death Mechanics

### 4.1 Friendly Minion Death
When a **friendly minion** dies, the owning player may choose:

| Option | Effect |
|--------|--------|
| Spawn Obstacle | Creates impassable terrain at death location |
| Spawn BUFF Tile | Creates a BUFF tile (random, may be positive or negative) |

### 4.2 BUFF Tile Properties
- Duration: **2 rounds**
- Trigger: **Once only** — removed after first unit steps on it
- Effect: Applies random BUFF (see BUFF_SYSTEM_V3.md)

### 4.3 Obstacle Properties
- Blocks movement (units cannot pass through)
- Duration: Permanent until destroyed
- Destruction: Only units with **POWER BUFF** can destroy obstacles

---

## 5. Victory Condition

**Kill the enemy Hero to win.**

- Minion deaths do not end the game
- If both heroes die simultaneously: Draw (or first-to-die loses — clarify)

---

## 6. Distance & Movement Rules

### 6.1 Distance Metric
All distances use **Manhattan distance**:
```
distance = abs(x1 - x2) + abs(y1 - y2)
```

### 6.2 Movement Constraints
- **Orthogonal only** — no diagonal movement
- Cannot pass through occupied tiles
- Cannot pass through obstacles

### 6.3 Attack Constraints
- **Orthogonal only** — no diagonal attacks
- Must have line-of-sight (no obstacles blocking — if implemented)

---

## 7. Validation Rules Summary

### MOVE
Invalid if:
- Unit not owned by acting player
- Unit is dead
- Target tile out of bounds
- Target tile occupied (by unit or obstacle)
- Distance > unit's `moveRange`
- Path blocked by obstacle (if pathfinding implemented)

### ATTACK
Invalid if:
- No valid target at position
- Target is friendly
- Target is dead
- Distance > unit's `attackRange`
- Attacker is dead

### MOVE_AND_ATTACK
Invalid if:
- MOVE step invalid
- After move, no enemy within distance 1
- ATTACK step invalid

### USE_SKILL
Invalid if:
- Unit is not a hero
- Skill is on cooldown
- Skill conditions not met (see SKILL_SYSTEM_V3.md)

---

## 8. Apply Action Effects

### MOVE
- Update unit position

### ATTACK
- Calculate damage: `attacker.attack + buffBonuses`
- Apply to target: `target.hp -= damage`
- If `target.hp <= 0`: Mark dead, trigger death mechanics

### MOVE_AND_ATTACK
- Execute MOVE
- Execute ATTACK
- End unit's turn

### END_TURN
- Pass to next unit/player

---

## 9. Round End Processing

At the end of each full round (after all units have acted):

1. **Minion Decay**: All minions lose 1 HP
2. **Poison Damage**: Apply poison from BUFFs
3. **BUFF Duration**: Decrease all BUFF durations by 1
4. **Remove Expired**: Remove BUFFs with duration ≤ 0
5. **Remove BUFF Tiles**: Decrease tile durations, remove expired
6. **Late-Game Check**: If round ≥ 8, apply HP loss

---

## 10. Compatibility Notes

### Breaking Changes from V2
| Feature | V2 | V3 |
|---------|----|----|
| Victory condition | Eliminate all enemies | Kill enemy hero |
| Team size | Configurable | Fixed: 1 hero + 2 minions |
| Minion selection | None | Draft 2 from 3 |
| Death mechanics | Unit removed | Obstacle or BUFF tile |
| Time limit | None | 10 seconds |
| Attrition | None | Minion decay + round 8 pressure |

### Migration
- V3 requires new `Hero` unit type
- V3 requires `SkillSystem` integration
- V3 requires `DraftPhase` before match
- V3 requires `Timer` system

---

## 11. Implementation Checklist

- [ ] Hero unit type with SPD integration
- [ ] Draft phase (select 2 of 3 minions)
- [ ] 10-second timer with timeout penalty
- [ ] Minion decay (1 HP per round)
- [ ] Death choice: obstacle vs BUFF tile
- [ ] BUFF tile system (2 rounds, one-time trigger)
- [ ] Obstacle spawning and destruction
- [ ] Round 8+ pressure system
- [ ] Victory condition: hero death only

---

# End of GAME_RULES_V3.md
