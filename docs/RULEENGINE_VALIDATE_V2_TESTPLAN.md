# RULEENGINE_VALIDATE_V2_TESTPLAN.md — RuleEngine.validateAction V2 Test Plan

## 1. Scope

This document defines the **Version 2 validation test plan** for:

- `com.tactics.engine.rules.RuleEngine.validateAction(GameState, Action)`

V2 focuses on:

- Using **unit.moveRange** for MOVE distance validation
- Using **unit.attackRange** for ATTACK and MOVE_AND_ATTACK distance validation
- Enabling **ranged attacks** (e.g., ARCHER attackRange = 2)
- Preserving all V1 behaviors when `moveRange = 1` and `attackRange = 1`

This test plan **extends** `RULEENGINE_TESTPLAN_V1`.
Existing V1 tests **must continue to pass**.

---

## 2. Reference Documents

- `GAME_RULES_V1.md`
- `GAME_RULES_V2.md`
- `UNIT_TYPES_V1.md`
- `RULEENGINE_TESTPLAN_V1.md`
- `RuleEngine.java`
- Engine models: `GameState`, `Unit`, `Board`, `Position`, `PlayerId`, `Action`, `ActionType`

---

## 3. Distance & Geometry Rules (V2 Recap)

- **Distance** is measured using **Manhattan distance**:

  ```text
  distance = abs(x1 - x2) + abs(y1 - y2)
  ```

- Movement and attacks must remain **orthogonal**:
  - No diagonal movement
  - No diagonal attacks

- **V2 MOVE**:
  - `1 <= distance <= unit.moveRange`

- **V2 ATTACK**:
  - `1 <= distance <= unit.attackRange`

- If a unit has `moveRange = 1` and `attackRange = 1`, then:
  - It behaves exactly like in V1.
  - All V1 tests remain valid.

---

## 4. Test Series Overview

| Series ID     | Area                    | Description                                      |
|---------------|-------------------------|--------------------------------------------------|
| G2-Series     | General                 | V2-specific general checks & backward compatibility |
| MV2-Series    | MOVE (V2)               | MOVE with moveRange (short, max, out-of-range)   |
| AV2-Series    | ATTACK (V2)             | ATTACK with attackRange (1, 2, out-of-range)     |
| MAV2-Series   | MOVE_AND_ATTACK (V2)    | Combined moveRange + attackRange                 |
| COMPAT-Series | Backward compatibility  | V1 behaviors preserved for range=1 units         |
| MSG2-Series   | Error messages          | Specific messages for V2 distance failures       |

> This document assumes all V1 test IDs remain valid and are not duplicated here.
> V2 tests should use new IDs (e.g., MV2-01, AV2-03).

---

## 5. G2-Series — General V2 Checks

### G2-01 — V2: Null action type still invalid

- **Given**: `action.type = null`
- **When**: `validateAction(state, action)`
- **Then**:
  - `isValid == false`
  - `errorMessage = "Invalid action type"` (same as V1)
- **Purpose**: Ensure V2 changes do not affect basic general validation.

### G2-02 — V2: Game over still blocks all actions

- **Given**:
  - `state.isGameOver = true`
  - Any valid-looking MOVE/ATTACK/MOVE_AND_ATTACK/END_TURN action
- **Then**:
  - `isValid == false`
  - `errorMessage = "Game is already over"`

### G2-03 — V2: Wrong currentPlayer still invalid

- **Given**:
  - `state.currentPlayer = P1`
  - `action.playerId = P2`
- **When**: `validateAction(state, action)`
- **Then**:
  - `isValid == false`
  - Error message consistent with V1 (e.g., `"Not your turn"`)

---

## 6. MV2-Series — MOVE with moveRange

These tests validate that MOVE now respects `unit.moveRange` instead of fixed `distance=1`.

### MV2-01 — MOVE within range (distance 1, moveRange=2) is valid

- **Given**:
  - Unit U (owner=P1) at (1,1), `moveRange = 2`
  - `state.currentPlayer = P1`
  - Target position (2,1) → distance=1
- **When**: MOVE to (2,1)
- **Then**:
  - `isValid == true`

### MV2-02 — MOVE at max range (distance = moveRange) is valid

- **Given**:
  - Unit U at (1,1), `moveRange = 2`
  - Target position (3,1) → distance=2
- **When**: MOVE to (3,1)
- **Then**:
  - `isValid == true`

### MV2-03 — MOVE beyond moveRange is invalid

- **Given**:
  - Unit U at (1,1), `moveRange = 2`
  - Target (4,1) → distance=3
- **When**: MOVE to (4,1)
- **Then**:
  - `isValid == false`
  - `errorMessage` indicates movement out of range (see MSG2-Series)

### MV2-04 — MOVE with distance = 0 (no-op) is invalid

- **Given**:
  - Unit U at (1,1), any `moveRange >= 1`
  - Target (1,1) → distance=0
- **When**: MOVE to same tile
- **Then**:
  - `isValid == false`
  - Error treated as invalid movement (e.g., `"Movement must change position"`)

### MV2-05 — Diagonal MOVE still invalid even if within moveRange

- **Given**:
  - Unit U at (1,1), `moveRange = 2`
  - Target (2,2) → Manhattan distance=2 but diagonal
- **When**: MOVE to (2,2)
- **Then**:
  - `isValid == false`
  - Error message indicates diagonal / invalid movement shape

### MV2-06 — MOVE with moveRange=1 behaves like V1

- **Given**:
  - Unit U at (1,1), `moveRange = 1`
  - Target (2,1) → distance=1
- **When**: MOVE to (2,1)
- **Then**:
  - `isValid == true` (same as V1)

### MV2-07 — MOVE blocked by occupied tile (within range) is invalid

- **Given**:
  - Unit U at (1,1), `moveRange = 2`
  - Another unit occupies (3,1)
  - Target position (3,1)
- **When**: MOVE to (3,1)
- **Then**:
  - `isValid == false`
  - Error matches V1 `"Target tile occupied"` behavior

> **Note**: This does not test pathfinding; it only validates the target tile.

---

## 7. AV2-Series — ATTACK with attackRange

These tests validate ranged and melee attacks based on `attackRange`.

### AV2-01 — Melee ATTACK (attackRange=1) at distance 1 is valid

- **Given**:
  - Attacker SWORDSMAN at (1,1), `attackRange=1`
  - Target enemy at (1,2) → distance=1
- **When**: ATTACK with `targetPosition = (1,2)` and correct `targetUnitId`
- **Then**:
  - `isValid == true`

### AV2-02 — Melee ATTACK (attackRange=1) at distance 2 is invalid

- **Given**:
  - Attacker SWORDSMAN at (1,1), `attackRange=1`
  - Target enemy at (1,3) → distance=2
- **When**: ATTACK
- **Then**:
  - `isValid == false`
  - Error indicates `"Target out of attack range"`

### AV2-03 — Ranged ATTACK (attackRange=2) at distance 2 is valid

- **Given**:
  - Attacker ARCHER at (1,1), `attackRange=2`
  - Target enemy at (1,3) → distance=2
- **When**: ATTACK
- **Then**:
  - `isValid == true`

### AV2-04 — Ranged ATTACK beyond attackRange is invalid

- **Given**:
  - ARCHER at (1,1), `attackRange=2`
  - Target enemy at (1,4) → distance=3
- **When**: ATTACK
- **Then**:
  - `isValid == false`
  - Error indicates out-of-range

### AV2-05 — Diagonal ATTACK within Manhattan range still invalid

- **Given**:
  - ARCHER at (1,1), `attackRange=2`
  - Target enemy at (2,2) → distance=2 (diagonal)
- **When**: ATTACK
- **Then**:
  - `isValid == false`
  - Error indicates invalid attack shape / requires orthogonal

### AV2-06 — Ambiguous ranged attacker (two friendlies in range) is invalid

- **Given**:
  - Friendly ARCHER A at (1,1), `attackRange=2`
  - Friendly ARCHER B at (3,1), `attackRange=2`
  - Enemy target at (2,1) → distance=1 from both A and B
- **When**: ATTACK targeting that enemy
- **Then**:
  - `isValid == false`
  - `errorMessage = "Ambiguous attacker"` (same concept as V1)

### AV2-07 — Unique ranged attacker is valid

- **Given**:
  - ARCHER A at (1,1), `attackRange=2`
  - Friendly SWORDSMAN B at (4,1), `attackRange=1`
  - Enemy at (3,1) → distance=2 from A, distance=1 from B
- **When**: ATTACK targeting enemy at (3,1)
- **Then**:
  - Only A qualifies if rules say:
    - B cannot attack at distance=2 (`attackRange=1`)
  - `isValid == true`
  - *(If your V2 design decides something else, adjust accordingly.)*

### AV2-08 — ATTACK with correct targetUnitId but mismatched position is invalid

- **Given**:
  - Enemy unit U at (1,3)
  - `action.targetUnitId = U.id`
  - `action.targetPosition = (1,2)`
- **When**: ATTACK
- **Then**:
  - `isValid == false`
  - Error indicates mismatch between `targetUnitId` and `targetPosition`

---

## 8. MAV2-Series — MOVE_AND_ATTACK with ranges

### MAV2-01 — MOVE_AND_ATTACK with MOVE in range and ATTACK in range is valid

- **Given**:
  - ARCHER at (1,1), `moveRange=1`, `attackRange=2`
  - Enemy at (3,1)
- **When**:
  - MOVE_AND_ATTACK:
    - `targetPosition = (2,1)` (MOVE step: distance=1)
    - `targetUnitId = enemy id`, enemy at (3,1) (ATTACK step: distance=1)
- **Then**:
  - `isValid == true`

### MAV2-02 — MOVE step in range, ATTACK step out of attackRange is invalid

- **Given**:
  - ARCHER at (1,1), `attackRange=2`
  - Enemy at (4,1)
- **When**:
  - MOVE_AND_ATTACK:
    - MOVE to (2,1) → distance=1
    - Enemy at (4,1) → post-move distance=2 (valid) or 3 (if different setup)
  - Design two cases:
    - **Case A**: distance=3 → invalid
    - **Case B**: distance=2 → valid
- **Then**:
  - Case A: `isValid == false`, out-of-range
  - Case B: `isValid == true`

### MAV2-03 — MOVE beyond moveRange, even if ATTACK would be in range, is invalid

- **Given**:
  - `moveRange = 1`
  - Trying to MOVE 2 tiles then ATTACK in range
- **When**: MOVE_AND_ATTACK
- **Then**:
  - `isValid == false`
  - Error corresponds to MOVE violation (not ATTACK)

### MAV2-04 — Ambiguous attacker after movement is invalid (ranged case)

- **Given**:
  - Two friendly ARCHERs, both can move then be in range of the same target, depending on inference rules.
- **When**:
  - MOVE_AND_ATTACK where final board state after movement yields 2 possible attackers in attackRange
- **Then**:
  - `isValid == false`
  - Error indicates ambiguous attacker after movement

---

## 9. COMPAT-Series — Backward Compatibility for Range=1 Units

### COMPAT-01 — SWORDSMAN MOVE (moveRange=1) remains V1 behavior

- **Given**:
  - SWORDSMAN with `moveRange=1`
  - Classic V1 case: move from (1,1) → (2,1)
- **Then**:
  - V1 MOVE test still passes under V2 logic.

### COMPAT-02 — SWORDSMAN ATTACK (attackRange=1) remains V1 behavior

- **Given**:
  - SWORDSMAN with `attackRange=1`
  - Classic V1 melee attack case
- **Then**:
  - V1 ATTACK tests still pass.

### COMPAT-03 — All existing V1 tests pass unchanged

- **Given**:
  - Full test suite from `RULEENGINE_TESTPLAN_V1`
- **Then**:
  - No behavior changes for existing V1 scenarios.

> *(This is a conceptual requirement; can be enforced by running the entire V1 test suite.)*

---

## 10. MSG2-Series — Error Message Expectations

These tests assert that error messages are specific enough to distinguish range issues from other failures.

### MSG2-01 — MOVE out-of-range uses a clear message

- **Scenario**: MOVE with distance > moveRange
- **Then**:
  - `errorMessage` contains something like `"Move out of range"` or `"Movement distance exceeds move range"`.

### MSG2-02 — ATTACK out-of-range uses a clear message

- **Scenario**: ATTACK with distance > attackRange
- **Then**:
  - `errorMessage` contains `"Attack out of range"` or equivalent.

### MSG2-03 — Diagonal MOVE produces shape-specific error

- **Scenario**: Diagonal MOVE, even within distance
- **Then**:
  - Message distinguishes diagonal from range (e.g., `"Movement must be orthogonal"`).

### MSG2-04 — Diagonal ATTACK produces shape-specific error

- **Scenario**: Diagonal ATTACK, even within attackRange
- **Then**:
  - Message clarifies invalid geometry rather than distance.

---

## 11. Recommended JUnit Structure

Suggested new test class (or extension of existing one):

**`RuleEngineValidateActionV2Test`**

Structure:

```java
@Nested
@DisplayName("MV2-Series - MOVE with moveRange")
class MoveV2Tests { ... }

@Nested
@DisplayName("AV2-Series - ATTACK with attackRange")
class AttackV2Tests { ... }

@Nested
@DisplayName("MAV2-Series - MOVE_AND_ATTACK with ranges")
class MoveAndAttackV2Tests { ... }

@Nested
@DisplayName("COMPAT-Series - Backward compatibility with V1")
class BackwardCompatibilityTests { ... }

@Nested
@DisplayName("MSG2-Series - V2 error messages")
class ErrorMessageV2Tests { ... }
```

Each test:

1. Builds minimal `GameState` with:
   - `Board(5,5)`
   - Units with desired `moveRange` / `attackRange` (for now via conventions or helper)
2. Creates a single `Action`
3. Calls `validateAction(state, action)`
4. Asserts:
   - `isValid`
   - `errorMessage` (where applicable)

---

**End of RULEENGINE_VALIDATE_V2_TESTPLAN.md**
