# RULEENGINE_APPLY_TESTPLAN_V1 — 5x5 Tactics RuleEngine ApplyAction Test Plan

## 1. Scope

This document defines the **test plan for `RuleEngine.applyAction`** in 5x5 Tactics (V1).

Assumptions:

- `applyAction` is only called with actions that have already passed
  `validateAction(state, action)` (i.e., validation is not repeated).
- All tests focus on **state transitions**:
  - Unit positions
  - HP changes
  - Alive/dead flags
  - Current player switching
  - Game over and winner detection

Validation error cases are covered separately in `RULEENGINE_TESTPLAN_V1`.

---

## 2. Conventions

Same notation as RULEENGINE_TESTPLAN_V1:

- Players: `P1`, `P2`
- Units: `u1_p1`, `u2_p1`, `u1_p2`, etc.
- Board: 5×5, positions `(x,y)` with `0 ≤ x,y ≤ 4`
- Default stats:
  - `hp = 10`
  - `attack = 3`
  - `alive = true` if `hp > 0`

`GameState` is immutable by design:

- Each call to `applyAction` must return a **new** GameState instance.

---

## 3. MOVE Apply Tests (AM-Series)

These tests assume MOVE has already been validated.

### AM1 — Basic MOVE Updates Position

- **Given**:
  - `currentPlayer = P1`
  - `u1_p1` at `(1,1)`, alive
  - Destination `(1,2)` is empty
- **Action**:
  - `type = MOVE`
  - `targetPosition = (1,2)`
- **Expected**:
  - Returned `GameState` has `u1_p1.position = (1,2)`
  - All other units unchanged
  - `currentPlayer` unchanged (`P1`)
  - `isGameOver = false`, `winner = null`

---

### AM2 — MOVE Does Not Mutate Original State

- **Given**:
  - Same as AM1
  - Keep a reference to original GameState
- **Action**:
  - Same as AM1
- **Expected**:
  - Original `GameState` still has `u1_p1.position = (1,1)`
  - New `GameState` has `u1_p1.position = (1,2)`
  - Out-of-place mutation (same object) must **not** occur

---

### AM3 — MOVE with Multiple Units present (Only One Moves)

- **Given**:
  - `u1_p1` at `(1,1)`
  - `u2_p1` at `(3,3)`
  - No other units
- **Action**:
  - Move `u1_p1` from `(1,1)` to `(1,2)`
- **Expected**:
  - `u1_p1.position = (1,2)`
  - `u2_p1.position` remains `(3,3)`
  - No unintended changes to other units

---

## 4. ATTACK Apply Tests (AA-Series)

These tests assume ATTACK is valid (adjacent, enemy, etc.).

### AA1 — Basic ATTACK Reduces Target HP

- **Given**:
  - `currentPlayer = P1`
  - `u1_p1` at `(1,1)`, `attack = 3`
  - `u1_p2` at `(1,2)`, `hp = 10`, `alive = true`
- **Action**:
  - `type = ATTACK`
  - `targetUnitId = "u1_p2"`
  - `targetPosition = (1,2)`
- **Expected**:
  - Target `u1_p2.hp = 7`
  - `u1_p2.alive = true`
  - Attacker `u1_p1` unchanged
  - `currentPlayer` unchanged (`P1`)
  - `isGameOver = false`, `winner = null`

---

### AA2 — ATTACK Kills Target (hp Drops to 0 or Below)

- **Given**:
  - `u1_p1` at `(1,1)`, `attack = 5`
  - `u1_p2` at `(1,2)`, `hp = 4`, `alive = true`
- **Action**:
  - ATTACK `u1_p2`
- **Expected**:
  - `u1_p2.hp = -1` (or any `<= 0` value)
  - `u1_p2.alive = false`
  - Other units unchanged
  - `currentPlayer` unchanged

---

### AA3 — ATTACK That Kills Last Enemy Triggers Game Over

- **Given**:
  - `currentPlayer = P1`
  - Enemy side (`P2`) has exactly one alive unit: `u1_p2` at `(1,2)`, `hp = 3`
  - `u1_p1` at `(1,1)`, `attack = 3`
  - No other P2 units or all others already dead
- **Action**:
  - ATTACK `u1_p2`
- **Expected**:
  - `u1_p2.alive = false`
  - Returned `GameState.isGameOver = true`
  - `GameState.winner = PlayerId("P1")`
  - `currentPlayer` may stay as `P1` after this call
    - (Turn switching due to game over is optional; but game cannot accept new actions)

---

### AA4 — ATTACK That Damages but Does Not Kill Does Not End Game

- **Given**:
  - `u1_p2.hp` remains above 0 after damage
- **Action**:
  - ATTACK
- **Expected**:
  - `isGameOver = false`
  - `winner = null`

---

### AA5 — ATTACK Does Not Mutate Original GameState

- Same pattern as AM2:
  - Original GameState remains with full HP
  - New GameState applies damage

---

## 5. MOVE_AND_ATTACK Apply Tests (AMA-Series)

MOVE_AND_ATTACK = MOVE step + ATTACK step + turn switch.

These tests assume the action is valid.

### AMA1 — MOVE_AND_ATTACK Performs Move and Attack

- **Given**:
  - `currentPlayer = P1`
  - `u1_p1` at `(1,1)`, `attack = 3`
  - `u1_p2` at `(2,1)`, `hp = 10`
  - `targetPosition = (1,2)` such that after moving, `u1_p1` is adjacent to `u1_p2`
- **Action**:
  - `type = MOVE_AND_ATTACK`
  - `targetPosition = (1,2)`
  - `targetUnitId = "u1_p2"`
- **Expected**:
  - Returned `GameState`:
    - `u1_p1.position = (1,2)`
    - `u1_p2.hp = 7`
    - `u1_p2.alive = true`
  - `currentPlayer` is now `P2` (turn switched)
  - `isGameOver = false`, `winner = null`

---

### AMA2 — MOVE_AND_ATTACK That Kills Target But Not Last Enemy

- **Given**:
  - `u1_p1` (P1) attacking `u1_p2` (P2)
  - P2 still has other alive units
- **Expected**:
  - `u1_p2.alive = false`
  - Other P2 units remain alive
  - `isGameOver = false`
  - `winner = null`
  - `currentPlayer` switched to P2

---

### AMA3 — MOVE_AND_ATTACK That Kills Last Enemy Triggers Game Over

- **Given**:
  - `u1_p1` is attacking P2's last alive unit
- **Expected**:
  - Target `alive = false`
  - `isGameOver = true`
  - `winner = P1`
  - Turn still switches to `P2` or stays on `P1`?
    - Decision: For V1, it is acceptable that `currentPlayer` switches as normal.
    - In any case, validation will prevent further actions post-game over.

---

### AMA4 — MOVE_AND_ATTACK Does Not Mutate Original GameState

- Validate:
  - Original `GameState` remains unchanged
  - New `GameState` has moved attacker + damaged target

---

## 6. END_TURN Apply Tests (AET-Series)

### AET1 — END_TURN Switches Current Player

- **Given**:
  - `currentPlayer = P1`
  - Game not over
- **Action**:
  - `type = END_TURN`
- **Expected**:
  - `currentPlayer = P2`
  - Units, HP, alive flags unchanged
  - `isGameOver` and `winner` unchanged

---

### AET2 — END_TURN Is No-Op for Units and Board

- Ensure:
  - No positions, HP, alive flags, or board dimensions change
  - Only `currentPlayer` changes

---

## 7. Game Over Consistency Tests (AGO-Series)

These tests ensure `applyAction` consistently sets `isGameOver` and `winner` based on unit states.

### AGO1 — No Winner When Both Sides Have Alive Units

- **Given**:
  - Each player has at least one `alive == true` unit
- **After any applyAction (MOVE/ATTACK/MOVE_AND_ATTACK/END_TURN)**:
  - `isGameOver = false`
  - `winner = null`

---

### AGO2 — Winner Detected When One Side Has No Alive Units

- **Given**:
  - After an ATTACK or MOVE_AND_ATTACK:
    - All P2 units `alive = false`
    - P1 has at least one alive unit
- **Expected**:
  - Returned `GameState.isGameOver = true`
  - `winner = P1`

---

### AGO3 — No Winner When Both Sides Have No Alive Units (Draw Not Supported in V1)

- **Given**:
  - After applying action, all units of **both** P1 and P2 are dead
- **Expected**:
  - For V1: either:
    - Treat as P1 victory, or
    - Treat as P2 victory, or
    - Disallow via validation
  - **NOTE**: V1 should choose one behavior and test for it.
  - Recommended for V1: this situation should not occur if validation is correct.
    - This case can be skipped or asserted as "undefined behavior" for now.

(If later you add draw rules, update this section.)

---

## 8. Immutability & Structural Integrity Tests (IM-Series)

These tests ensure no accidental mutable sharing.

### IM1 — New GameState Instance Returned

- For any action:
  - `newState != oldState` (different reference)

---

### IM2 — Units List Copied, Not Mutated In-Place

- **Given**:
  - Hold reference to `oldUnits = oldState.getUnits()`
- **After**:
  - `newUnits = newState.getUnits()`
- **Expected**:
  - `newUnits != oldUnits`
  - Shared Unit objects may or may not be re-used for unchanged units, but:
    - At least for changed units, new Unit instances must be created
    - No in-place modification of old Unit instances

(Implementation detail: whether to copy all Units or only modified ones is up to you, but tests should ensure oldState is unaffected.)

---

### IM3 — Board Is Immutable

- `Board` is typically shared between states (since width/height don't change).
- Tests should ensure:
  - Board dimensions are consistent before and after
  - No methods mutate Board (there are no setters in V1)

---

## 9. Suggested Test Implementation Strategy

- Create `RuleEngineApplyActionTest` (JUnit 5).
- Optionally mirror the structure of `RuleEngineValidateActionTest`
  - Use @Nested for MOVE / ATTACK / MOVE_AND_ATTACK / END_TURN / GameOver / Immutability
- For each test:
  - Build a small, explicit GameState
  - Create Action that is **already valid** under GAME_RULES_V1
  - Call `applyAction` directly
  - Assert on:
    - Positions
    - HP
    - alive
    - currentPlayer
    - isGameOver
    - winner

---

# End of RULEENGINE_APPLY_TESTPLAN_V1.md
