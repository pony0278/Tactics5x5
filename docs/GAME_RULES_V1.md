# GAME_RULES_V1 — 5x5 Tactics Game Rules (Version 1, Updated)

## 1. Scope of This Document

This document defines the official gameplay rules for **5x5 Tactics, Version 1**.

It specifies:

- Turn structure
- Movement rules
- Attack rules
- Win conditions
- Action legality
- Positional constraints
- Interaction constraints

These rules must be strictly followed by:

- `RuleEngine.validateAction()`
- `RuleEngine.applyAction()`

This document must **not** define or contradict:

- Action formats (see **WS_PROTOCOL_V1**)
- Engine class structure (see **ENGINE_SKELETON_V1**)
- System architecture (see **TECH_ARCH**)

---

## 2. Board Rules

- The board is a **5 × 5** grid.
- Valid coordinates: **x = 0–4**, **y = 0–4**.
- Units may not move outside the board.

---

## 3. Unit Rules

Each unit has:

- `id`
- `owner`
- `hp`
- `attack`
- `position`
- `alive`

### 3.1 Alive/Dead Rules

A unit is alive if:

- `alive == true`, and
- `hp > 0`

If `hp <= 0`, then:

- `alive` must be set to **false**
- Dead units cannot move or attack
- Dead units remain in the GameState but are ignored by action rules

---

## 4. Turn Structure

- Only **one player acts at a time**.
- The active player is given by **GameState.currentPlayer**.

A player's turn ends only if:

1. They perform **END_TURN**, or
2. They perform **MOVE_AND_ATTACK** (which ends the turn automatically)

---

## 5. Action Rules

There are four allowed action types.

---

### 5.1 MOVE

The player moves one of their own units.

Movement rules:

- Distance: **exactly 1 tile (orthogonal only)**
- Allowed deltas:
  - `(0,1)`
  - `(0,-1)`
  - `(1,0)`
  - `(-1,0)`
- No diagonal movement

Target tile must be:

- Inside the board
- Not occupied by any unit

Additional constraints:

- The unit must be **alive**
- The unit must belong to the **acting player**

---

### 5.2 ATTACK

The player attacks an enemy unit.

Attack range:

- Distance: **exactly 1 tile orthogonal**
- Same delta rules as MOVE

Target unit must:

- Exist
- Be alive
- Belong to the opponent

Attacker must:

- Belong to the acting player
- Be alive
- Be adjacent to the target

---

### 5.2.1 Required Target Specification (Mandatory)

An ATTACK action must specify **both**:

- `targetUnitId` — the enemy being attacked
- `targetPosition` — the position of the enemy unit

These two must match (server must confirm consistency).

The attacker is inferred as:

> The **unique** alive unit owned by `playerId` that is adjacent to the targetUnit.

If multiple friendly units are adjacent to the target, the action is invalid
(**"Ambiguous attacker"**).

---

### 5.2.2 Damage Resolution

```
target.hp -= attacker.attack
```

If `target.hp <= 0`:

- Mark unit as dead (`alive = false`)

---

### 5.2.3 Turn Does NOT End Automatically

ATTACK does **not** end the player's turn.
Multiple attacks per turn are allowed if rules permit.

---

### 5.3 MOVE_AND_ATTACK

This action performs:

1. MOVE (exactly 1 tile, following MOVE rules)
2. ATTACK targetUnitId (following ATTACK rules)

This is a combined action and both movement and attack must be legal.

---

### 5.3.1 Required Target Specification (Mandatory)

MOVE_AND_ATTACK must specify:

- `targetPosition` — movement destination
- `targetUnitId` — the enemy to attack after movement

After moving:

- The acting unit must be adjacent to the target
- Only **one** attacking candidate may exist
  - If multiple exist → invalid ("Ambiguous attacker after movement")

---

### 5.3.2 Turn Ends Automatically

After a successful MOVE_AND_ATTACK:

- The turn ends
- `currentPlayer` switches to the opponent

---

### 5.4 END_TURN

- Ends the player's turn
- Switches `currentPlayer` to the opponent
- Always valid unless the game is already over

---

## 6. Unit Selection Rules

For MOVE, ATTACK, or MOVE_AND_ATTACK:

- The acting player must choose **one alive unit they own**.

Selection is implicit via Action fields:

- MOVE → inferred from which unit can legally move to `targetPosition`
- ATTACK → inferred from attackers adjacent to targetUnit
- MOVE_AND_ATTACK → inferred after movement

Ambiguity → **invalid action**

---

## 7. Targeting Rules

### 7.1 Position Targets

`targetPosition` must:

- Not be null when required
- Be inside board bounds
- Not be occupied during MOVE
- Be adjacent when required for ATTACK or MOVE_AND_ATTACK

### 7.2 Unit Targets

`targetUnitId` must:

- Not be null for ATTACK and MOVE_AND_ATTACK
- Refer to an existing and alive unit
- Belong to the opponent

---

## 8. Win Conditions

A player wins if:

- **All opponent units are dead**

When win is detected:

- `GameState.isGameOver = true`
- `GameState.winner = <PlayerId>`

After game over:

- All actions must be rejected
- `validateAction` must return: `"Game is already over"`

---

## 9. Validation Rules Summary

`ValidationResult.isValid = false` if any rule below is violated.

### General Validation

- Action type is null
- Acting player does not match `currentPlayer`
- Game is already over

### MOVE

- Unit does not belong to acting player
- Unit is dead
- Target tile outside board
- Target tile occupied
- Movement not exactly 1 tile orthogonal

### ATTACK

- Attacker not owned by acting player
- Attacker dead
- Target missing
- Target dead
- Target not adjacent
- Ambiguous attacker
- Missing targetUnitId or targetPosition

### MOVE_AND_ATTACK

- MOVE step invalid
- No enemy adjacent after movement
- Ambiguous attacker after movement
- Attack step invalid
- Missing targetUnitId

### END_TURN

- Always valid unless game already over

---

## 10. ApplyAction Rules Summary

### MOVE
- Update unit position
- Return new GameState

### ATTACK
- Apply damage
- Mark target dead if hp <= 0
- Turn does **not** end

### MOVE_AND_ATTACK
- Apply MOVE
- Apply ATTACK
- End turn (switch `currentPlayer`)

### END_TURN
- Switch `currentPlayer`

---

## 11. Notes for Future Versions

This V1 ruleset intentionally excludes:

- Multi-tile movement
- Ranged attacks
- Skills
- Buffs/Debuffs
- Terrain effects
- Line of sight
- Action points
- Special unit classes

These will be introduced in future expansions.

---

# End of GAME_RULES_V1.md
