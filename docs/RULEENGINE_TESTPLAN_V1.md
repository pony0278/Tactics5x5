# RULEENGINE_TESTPLAN_V1 — 5x5 Tactics RuleEngine Validation Test Plan

## 1. Scope

This document defines the **test plan for `RuleEngine.validateAction`** in 5x5 Tactics (V1).

It focuses purely on **validation**:

- Determining whether an `Action` is legal in a given `GameState`
- Producing `ValidationResult(isValid, errorMessage)`

`applyAction` is **out of scope** for this document (separate test plan).

The goal:

- Ensure complete coverage of GAME_RULES_V1 (especially §4–§10)
- Ensure deterministic, predictable validation outcomes
- Ensure no hidden rules beyond the documentation

---

## 2. Conventions and Notation

### 2.1 Players

- `P1`, `P2` — player IDs (string, wrapped in `PlayerId` in engine)

### 2.2 Units

Example unit IDs:

- `u1_p1`, `u2_p1` — units owned by `P1`
- `u1_p2`, `u2_p2` — units owned by `P2`

Unless otherwise specified:

- Default `hp = 10`
- Default `attack = 3`
- `alive = true` if `hp > 0`

### 2.3 Positions

Board is 5×5:

- Valid: `x ∈ {0..4}`, `y ∈ {0..4}`

We use sample coordinates like:

- `(0,0)`, `(1,0)`, `(2,0)`, `(1,1)`, `(4,4)` etc.

### 2.4 Action Types

- `MOVE`
- `ATTACK`
- `MOVE_AND_ATTACK`
- `END_TURN`

### 2.5 ValidationResult Expectations

For each test case, we specify:

- `isValid`: `true` or `false`
- `expectedError`: null or a string (e.g., `"Game is already over"`, `"Ambiguous attacker"`)

Error messages are indicative; implementation may use slightly different wording, but:

- Distinct error conditions must be distinguishable in tests
- Messages should be stable enough to assert on

---

## 3. General Validation (G-Series)

These tests apply to **all action types**.

### G1 — Null Action Type

- **Given**:
  - Any valid `GameState` (game not over, `currentPlayer = P1`)
- **Action**:
  - `type = null`
  - `playerId = "P1"`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates missing or unknown action type (e.g., `"Invalid action type"`)

---

### G2 — Wrong Player Turn

- **Given**:
  - `currentPlayer = P1`
- **Action**:
  - Any valid `MOVE` that P2 could perform
  - `playerId = "P2"`
- **Expected**:
  - `isValid = false`
  - `expectedError = "Not your turn"` (or equivalent)

---

### G3 — Game Already Over

- **Given**:
  - `isGameOver = true`
  - `winner` is non-null
- **Action**:
  - Any action (MOVE/ATTACK/MOVE_AND_ATTACK/END_TURN) from any player
- **Expected**:
  - `isValid = false`
  - `expectedError = "Game is already over"`

---

### G4 — Unknown Action Type String (if ever exposed)

- **Given**:
  - Valid `GameState`, `currentPlayer = P1`
- **Action**:
  - `type = <unsupported enum or mapping error>`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates invalid action type

(Implementation detail: this may be caught before RuleEngine; include here for robustness.)

---

## 4. MOVE Validation (M-Series)

Assume:

- `currentPlayer = P1`
- Unit `u1_p1` at `(1,1)` alive
- Board is empty except where specified

### M1 — Valid MOVE, Inside Board, Empty Destination

- **Given**:
  - `u1_p1` at `(1,1)`
  - Destination `(1,2)` is empty and inside board
- **Action**:
  - `type = MOVE`
  - `playerId = "P1"`
  - `targetPosition = (1,2)`
  - `targetUnitId = null`
- **Expected**:
  - `isValid = true`
  - `expectedError = null`

---

### M2 — MOVE Out of Board (Negative Coordinate)

- **Given**:
  - `u1_p1` at `(0,0)`
- **Action**:
  - `type = MOVE`
  - `playerId = "P1"`
  - `targetPosition = (-1,0)` (outside board)
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates out-of-bounds movement

---

### M3 — MOVE Out of Board (Beyond Max Coordinate)

- **Given**:
  - `u1_p1` at `(4,4)`
- **Action**:
  - `type = MOVE`
  - `playerId = "P1"`
  - `targetPosition = (5,4)` (x = 5)
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates out-of-bounds

---

### M4 — MOVE More Than One Tile

- **Given**:
  - `u1_p1` at `(1,1)`
- **Action**:
  - `targetPosition = (1,3)` (2 tiles away)
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates invalid move distance (must be exactly 1 orthogonal step)

---

### M5 — MOVE Diagonally

- **Given**:
  - `u1_p1` at `(1,1)`
- **Action**:
  - `targetPosition = (2,2)` (diagonal)
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates movement must be orthogonal

---

### M6 — MOVE Into Occupied Tile

- **Given**:
  - `u1_p1` at `(1,1)`
  - `u2_p1` (or `u1_p2`) at `(1,2)`
- **Action**:
  - `targetPosition = (1,2)`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates tile is occupied

---

### M7 — MOVE with Dead Unit

- **Given**:
  - `u1_p1` at `(1,1)` with `hp <= 0`, `alive = false`
- **Action**:
  - `targetPosition = (1,2)`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates unit is dead / cannot move

---

### M8 — MOVE with targetUnitId Non-null (Protocol Misuse)

- **Given**:
  - `u1_p1` at `(1,1)`
- **Action**:
  - `type = MOVE`
  - `targetPosition = (1,2)`
  - `targetUnitId = "uX"`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates invalid field combination for MOVE

---

## 5. ATTACK Validation (A-Series)

Assume:

- `currentPlayer = P1`
- `u1_p1` at `(1,1)`, alive
- `u1_p2` at `(1,2)`, alive (adjacent enemy)

ATTACK requires:

- `targetUnitId` (enemy)
- `targetPosition` (enemy position)
- Attacker inferred as unique adjacent friendly

### A1 — Valid ATTACK (Single Adjacent Enemy)

- **Given**:
  - `u1_p1` at `(1,1)` (P1, alive)
  - `u1_p2` at `(1,2)` (P2, alive)
- **Action**:
  - `type = ATTACK`
  - `playerId = "P1"`
  - `targetUnitId = "u1_p2"`
  - `targetPosition = (1,2)`
- **Expected**:
  - `isValid = true`
  - `expectedError = null`

---

### A2 — ATTACK with Target Out of Range (More Than 1 Tile)

- **Given**:
  - `u1_p1` at `(1,1)`
  - `u1_p2` at `(1,3)` (distance 2)
- **Action**:
  - `targetUnitId = "u1_p2"`
  - `targetPosition = (1,3)`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates target not adjacent

---

### A3 — ATTACK with Diagonal Target

- **Given**:
  - `u1_p1` at `(1,1)`
  - `u1_p2` at `(2,2)` (diagonal)
- **Action**:
  - `targetUnitId = "u1_p2"`
  - `targetPosition = (2,2)`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates target not adjacent (orthogonal only)

---

### A4 — ATTACK Self / Friendly Unit

- **Given**:
  - `u1_p1` at `(1,1)`
  - `u2_p1` at `(1,2)` (friendly, alive)
- **Action**:
  - `targetUnitId = "u2_p1"`
  - `targetPosition = (1,2)`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates target must be an enemy

---

### A5 — ATTACK with Dead Attacker

- **Given**:
  - `u1_p1` at `(1,1)`, `alive = false` (hp <= 0)
  - `u1_p2` at `(1,2)`, alive
- **Action**:
  - `targetUnitId = "u1_p2"`
  - `targetPosition = (1,2)`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates attacker is dead

---

### A6 — ATTACK with Dead Target

- **Given**:
  - `u1_p1` at `(1,1)`, alive
  - `u1_p2` at `(1,2)`, `alive = false`, `hp <= 0`
- **Action**:
  - `targetUnitId = "u1_p2"`
  - `targetPosition = (1,2)`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates target is dead / cannot be attacked

---

### A7 — ATTACK with Missing targetUnitId

- **Given**:
  - Same as A1
- **Action**:
  - `type = ATTACK`
  - `targetUnitId = null`
  - `targetPosition = (1,2)`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates missing targetUnitId

---

### A8 — ATTACK with Missing targetPosition

- **Given**:
  - Same as A1
- **Action**:
  - `type = ATTACK`
  - `targetUnitId = "u1_p2"`
  - `targetPosition = null`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates missing targetPosition

---

### A9 — ATTACK Ambiguous Attacker (Multiple Friendlies Adjacent)

- **Given**:
  - `u1_p1` at `(1,1)` (P1, alive)
  - `u2_p1` at `(1,3)` (P1, alive)
  - `u1_p2` at `(1,2)` (P2, alive) — adjacent to both `u1_p1` and `u2_p1`
- **Action**:
  - `type = ATTACK`
  - `playerId = "P1"`
  - `targetUnitId = "u1_p2"`
  - `targetPosition = (1,2)`
- **Expected**:
  - `isValid = false`
  - `expectedError = "Ambiguous attacker"` (or equivalent message indicating ambiguity)

---

## 6. MOVE_AND_ATTACK Validation (MA-Series)

Assume:

- `currentPlayer = P1`
- `u1_p1` at `(1,1)`
- `u1_p2` at `(2,1)` initially, etc.

MOVE_AND_ATTACK requires:

- `targetPosition` (destination tile)
- `targetUnitId` (enemy to attack after movement)

### MA1 — Valid MOVE_AND_ATTACK

- **Given**:
  - `u1_p1` at `(1,1)` (P1, alive)
  - `u1_p2` at `(2,1)` (P2, alive)
- **Action**:
  - `type = MOVE_AND_ATTACK`
  - `playerId = "P1"`
  - `targetPosition = (1,2)` (legal move)
  - `targetUnitId = "u1_p2"`
- **Assumption**:
  - After moving to `(1,2)`, `u1_p1` is adjacent (orthogonal) to `u1_p2` at `(2,1)`
- **Expected**:
  - `isValid = true`
  - `expectedError = null`

---

### MA2 — MOVE_AND_ATTACK with Illegal MOVE Step

- **Given**:
  - `u1_p1` at `(1,1)`
- **Action**:
  - `targetPosition = (1,3)` (distance 2)
  - `targetUnitId = "u1_p2"`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates illegal move part (same as MOVE checks)

---

### MA3 — MOVE_AND_ATTACK with No Adjacent Enemy After Move

- **Given**:
  - `u1_p1` at `(1,1)`
  - `u1_p2` at `(4,4)`
- **Action**:
  - `targetPosition = (1,2)` (legal move)
  - `targetUnitId = "u1_p2"`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates no valid adjacent target after movement

---

### MA4 — MOVE_AND_ATTACK with Missing targetUnitId

- **Given**:
  - `u1_p1` at `(1,1)`
  - `u1_p2` adjacent after move
- **Action**:
  - `type = MOVE_AND_ATTACK`
  - `targetPosition = (1,2)`
  - `targetUnitId = null`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates missing targetUnitId

---

### MA5 — MOVE_AND_ATTACK with Missing targetPosition

- **Given**:
  - `u1_p1` at `(1,1)`
  - `u1_p2` at some adjacent tile to a possible destination
- **Action**:
  - `type = MOVE_AND_ATTACK`
  - `targetPosition = null`
  - `targetUnitId = "u1_p2"`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates missing targetPosition

---

### MA6 — MOVE_AND_ATTACK Ambiguous Attacker After Move

- **Given**:
  - `u1_p1` at `(1,1)` (P1)
  - `u2_p1` at `(1,3)` (P1)
  - `u1_p2` at `(1,2)` (P2)
- **Action**:
  - Movement such that after moving, both `u1_p1` and `u2_p1` are adjacent to `u1_p2`
  - `type = MOVE_AND_ATTACK`
  - `targetPosition` = some tile
  - `targetUnitId = "u1_p2"`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates ambiguous attacker after movement

(Exact coordinates can be chosen to construct the ambiguity.)

---

### MA7 — MOVE_AND_ATTACK with Dead Attacker

- **Given**:
  - `u1_p1` at `(1,1)`, `alive = false`
  - `u1_p2` at `(2,1)`, alive
- **Action**:
  - `type = MOVE_AND_ATTACK`
  - `targetPosition = (1,2)`
  - `targetUnitId = "u1_p2"`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates attacker dead

---

### MA8 — MOVE_AND_ATTACK with Dead Target

- **Given**:
  - `u1_p1` at `(1,1)`, alive
  - `u1_p2` at `(2,1)`, `alive = false`
- **Action**:
  - `type = MOVE_AND_ATTACK`
  - `targetPosition` = tile adjacent to `(2,1)`
  - `targetUnitId = "u1_p2"`
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates target dead

---

## 7. END_TURN Validation (ET-Series)

### ET1 — Valid END_TURN

- **Given**:
  - `currentPlayer = P1`
  - Game is not over
- **Action**:
  - `type = END_TURN`
  - `playerId = "P1"`
- **Expected**:
  - `isValid = true`
  - `expectedError = null`

---

### ET2 — END_TURN When Game Over

- **Given**:
  - `isGameOver = true`
- **Action**:
  - `type = END_TURN`
  - `playerId = "P1"`
- **Expected**:
  - `isValid = false`
  - `expectedError = "Game is already over"`

---

## 8. Additional Edge Cases (E-Series)

### E1 — No Units on Board

- **Given**:
  - `units = []`
  - `isGameOver = false`
- **Action**:
  - Any MOVE / ATTACK / MOVE_AND_ATTACK
- **Expected**:
  - `isValid = false`
  - `expectedError` indicates no valid acting unit / invalid action

---

### E2 — All Opponent Units Dead (Game Should Be Over in ApplyAction)

This is more relevant for `applyAction`, but for validation:

- **Given**:
  - All units of opponent have `alive = false` and `hp <= 0`
  - `isGameOver = true` already updated
- **Action**:
  - Any action
- **Expected**:
  - Same as G3 — game already over

---

## 9. Implementation Notes

- All tests should be implemented as pure unit tests for `RuleEngine.validateAction`.
- Any randomness must not be used in validation.
- Error messages should be **consistent and explicit** enough to be asserted.
- New rules must not be added beyond what is described in GAME_RULES_V1.

---

# End of RULEENGINE_TESTPLAN_V1.md
