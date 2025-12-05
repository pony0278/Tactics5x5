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
- **Alternating unit actions**: Player A Unit 1 → Player B Unit 1 → Player A Unit 2 → ...
- Each unit may act once per round

### 2.2 Exhaustion Rule
When one player runs out of units to move but the opponent still has unused units:
- The opponent takes **consecutive turns** for their remaining units
- A Round ends only when **ALL living units** on the board have acted once

```
Example:
- P1 has 2 units, P2 has 3 units
- P1 Unit1 → P2 Unit1 → P1 Unit2 → P2 Unit2 → P2 Unit3
- Round ends (all 5 units have acted)
```

### 2.3 Time Limit
- **10 seconds** per action decision
- Timeout penalty: Player's **HERO** loses **1 HP**

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
- Starting from **Round 8**, **all units** (Heroes + Minions) lose **1 HP per round**
- This stacks with Minion Decay (minions lose 2 HP/round total after R8)

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
| Property | Value |
|----------|-------|
| HP | 3 |
| Destructible | Yes — any unit can ATTACK an obstacle |
| POWER Crush | Units with POWER BUFF destroy obstacles in 1 hit |
| Blocking | Blocks movement and line-of-sight while active |

```
Destruction methods:
1. Normal attack: Obstacle takes damage, destroyed when HP ≤ 0
2. POWER buff: Instant destruction (ignores HP)
```

---

## 5. Victory Condition

**Kill the enemy Hero to win.**

- Minion deaths do not end the game

### 5.1 Simultaneous Death Resolution
If an action causes both Heroes to die at the same time (e.g., via Reflection damage or AoE):
- The **Active Player** (the one who initiated the attack) **WINS**
- Rationale: Rewards aggression over defense

```
Example:
- P1 attacks with Wild Magic, kills both heroes
- P1 WINS (P1 is the active player)
```

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
- No valid target at position (unit or obstacle)
- Target is friendly unit
- Target is dead
- Distance > unit's `attackRange`
- Attacker is dead

**Note**: Obstacles are valid attack targets. Attacks reduce obstacle HP.

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
**Against Unit:**
- Calculate damage: `attacker.attack + buffBonuses`
- Apply to target: `target.hp -= damage`
- If `target.hp <= 0`: Mark dead, trigger death mechanics

**Against Obstacle:**
- If attacker has POWER buff: Obstacle destroyed instantly
- Else: `obstacle.hp -= attacker.attack`
- If `obstacle.hp <= 0`: Remove obstacle from board

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
- [ ] 10-second timer with timeout penalty (Hero -1 HP)
- [ ] Minion decay (1 HP per round)
- [ ] Death choice: obstacle vs BUFF tile
- [ ] BUFF tile system (2 rounds, one-time trigger)
- [ ] Obstacle system (3 HP, attackable, POWER instant destroy)
- [ ] Round 8+ pressure system (all units -1 HP)
- [ ] Victory condition: hero death only
- [ ] Simultaneous death: active player wins
- [ ] Exhaustion rule: consecutive turns when opponent runs out

---

# End of GAME_RULES_V3.md
