# GAME_RULES_V2.md — 5x5 Tactics Game Rules (Version 2)

## Enhancements: Ranged Attacks & Movement Range

---

## 1. Purpose

This document defines the **Version 2 gameplay rules** for 5x5 Tactics.

V2 extends the V1 ruleset with:

- Unit-based movement distance (`moveRange`)
- Unit-based attack distance (`attackRange`)
- Full support for ranged attacks
- Updated validation logic for MOVE, ATTACK, MOVE_AND_ATTACK

The following V1 rules remain unchanged unless explicitly overridden:

- Turn structure
- Win conditions
- Unit alive/dead rules
- Action types
- Action field requirements
- No diagonal movement

---

## 2. Compatibility Policy

V2 is **fully backward-compatible** with V1.

- If a unit has `moveRange = 1` and `attackRange = 1`, its behavior is exactly the same as V1.
- Only units with `attackRange > 1` (e.g., ARCHER) gain new capabilities.

---

## 3. New Fields (Defined in UNIT_TYPES_V1.md)

Each unit type now defines:

| Field | Meaning |
|-------|---------|
| `moveRange` | Maximum tiles a unit may move per MOVE action |
| `attackRange` | Maximum tiles a unit may attack from (Manhattan distance) |

These values come from `/docs/UNIT_TYPES_V1.md`.

Example (Archer):

```
moveRange = 1
attackRange = 2
```

---

## 4. Board Geometry & Distance

### 4.1 Manhattan Distance

All distance rules use **Manhattan distance**:

```
distance = abs(x1 - x2) + abs(y1 - y2)
```

### 4.2 Orthogonal Only

Units cannot:

- Move diagonally
- Attack diagonally

A move or attack is valid only if the displacement occurs purely horizontally or vertically (same rule as V1).

---

## 5. MOVE Rules (Updated for V2)

MOVE now uses the unit's `moveRange`.

### 5.1 MOVE Distance (New)

```
distance(position → targetPosition) <= unit.moveRange
```

But movement must be **orthogonal**:

- Horizontal moves: `(dx > 0 && dy == 0)`
- Vertical moves: `(dx == 0 && dy > 0)`
- No diagonal or mixed-axis movement

### 5.2 MOVE Summary (Updated)

A MOVE is valid if:

- Acting unit belongs to acting player
- Unit is alive
- Target tile is inside board
- Target tile is unoccupied
- Movement is orthogonal
- Manhattan distance ≤ `unit.moveRange`

This replaces V1's "exactly 1 tile" rule.

---

## 6. ATTACK Rules (Updated for V2)

ATTACK now uses the unit's `attackRange` for targeting.

### 6.1 Attack Distance (New)

```
distance(attacker → targetUnitPosition) <= unit.attackRange
```

But still **orthogonal only** — distance measured along a straight line.

### 6.2 ATTACK Target Requirements (Same as V1)

Target unit must:

- Exist
- Be alive
- Belong to the opponent
- Match `targetUnitId`
- Match `targetPosition` exactly

### 6.3 Attacker Inference (Same as V1)

The attacker is inferred as the **unique** alive friendly unit satisfying:

```
distance(attacker.position → target.position) <= attacker.attackRange
```

- If 0 attackers exist → invalid
- If 2+ attackers exist → invalid ("Ambiguous attacker")

---

## 7. MOVE_AND_ATTACK Rules (Updated for V2)

- MOVE step uses `moveRange`
- ATTACK step uses `attackRange`

### 7.1 Combined Rules

A MOVE_AND_ATTACK is valid if:

- MOVE step valid using `moveRange`
- After movement, attacker is orthogonally aligned with the target
- `distance <= attackRange`
- Target exists, alive, enemy
- Attacker is uniquely identifiable
- All ATTACK V2 rules apply

### 7.2 Turn Switching (Same as V1)

After a successful MOVE_AND_ATTACK:

- Turn ends immediately
- `currentPlayer` switches to opponent

---

## 8. Action Field Requirements (Same as V1)

| Action | targetPosition | targetUnitId |
|--------|----------------|--------------|
| MOVE | required | null |
| ATTACK | required | required |
| MOVE_AND_ATTACK | required | required |
| END_TURN | null | null |

Both `targetPosition` and `targetUnitId` must be consistent for ATTACK and MOVE_AND_ATTACK.

---

## 9. Full Validation Summary (V2)

### General

- Null action type → invalid
- Wrong player → invalid
- Game is already over → invalid

### MOVE (V2)

Invalid if:

- Unit not owned by player
- Unit dead
- Target out of board
- Target occupied
- Movement diagonal
- `distance > moveRange`

### ATTACK (V2)

Invalid if:

- Target missing
- Target dead
- Target is friendly
- No adjacent attacker within `attackRange`
- Multiple possible attackers
- Distance diagonal
- `distance > attackRange`
- Missing required fields

### MOVE_AND_ATTACK (V2)

Invalid if:

- MOVE step invalid (`moveRange`)
- ATTACK step invalid (`attackRange`)
- No adjacent attacker after moving
- Ambiguous attacker
- Missing fields

### END_TURN

Same as V1.

---

## 10. ApplyAction Summary (Unchanged)

### MOVE

- Update position only

### ATTACK

- Deal damage
- Kill if `hp ≤ 0`

### MOVE_AND_ATTACK

- Move
- Attack
- Switch turn

### END_TURN

- Switch turn

---

## 11. Notes for Future V3 / V4

V2 focuses solely on enabling ranged attacks and scalable movement.

Future expansions may include:

- Diagonal ranged targeting
- Obstacles / terrain blocking line-of-sight
- Movement costs per tile
- Archer minimum range constraint
- True projectile simulation (SPD-compatible)

---

# End of GAME_RULES_V2.md
