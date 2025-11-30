# UNIT_TYPES_TESTPLAN_V1 — Unit Types Test Plan (Version 1)

## 1. Scope

This document defines the **test plan for unit types and the default starting lineup** in 5x5 Tactics, Version 1.

Covered components:

- `UNIT_TYPES_V1.md` (specification document)
- `GameStateFactory` (or equivalent initial state builder, e.g. `createStandardGame()`)

Focus:

- Ensuring the default lineup matches `UNIT_TYPES_V1.md`
- Verifying HP / ATK stats per unit type in the initial `GameState`
- Verifying starting positions, owners, and basic game flags (`currentPlayer`, `isGameOver`, `winner`)
- Ensuring TANK is defined but not present in the default lineup

Not covered:

- RuleEngine behavior per unit type (still V1 rules)
- Ranged behavior using `attackRange` (planned for future GAME_RULES_V2)
- Buffs, terrain, or SPD-specific behaviors

---

## 2. Reference Specifications

### 2.1 UNIT_TYPES_V1 (summary)

From `UNIT_TYPES_V1.md`:

| Type       | maxHp | attack | moveRange | attackRange | Role               |
|------------|-------|--------|-----------|-------------|--------------------|
| SWORDSMAN  | 10    | 3      | 1         | 1           | Balanced melee     |
| ARCHER     | 8     | 3      | 1         | 2 (future)  | Ranged attacker    |
| TANK       | 16    | 2      | 1         | 1           | Durable frontline  |

> Note: `moveRange` and `attackRange` are **not enforced** in GAME_RULES_V1.
> They are future-facing fields for GAME_RULES_V2.

### 2.2 Default starting lineup

From `UNIT_TYPES_V1.md`:

- **Player 1**
  - `u1_p1` — SWORDSMAN at (1, 0)
  - `u2_p1` — ARCHER at (3, 0)

- **Player 2**
  - `u1_p2` — SWORDSMAN at (1, 4)
  - `u2_p2` — ARCHER at (3, 4)

---

## 3. Test Series Overview

| Series ID      | Area                          | Description                                      |
|----------------|-------------------------------|--------------------------------------------------|
| GSF-Series     | GameStateFactory              | Default game creation / lineup and stats         |
| UTDOC-Series   | Spec consistency (doc ↔ code) | Consistency between `UNIT_TYPES_V1.md` and code  |

---

## 4. GSF-Series — GameStateFactory Tests

These tests validate the behavior of the factory method that creates the standard initial `GameState`, e.g. `GameStateFactory.createStandardGame()`.

### GSF1 — createStandardGame returns non-null GameState

- **Given**
  - `GameStateFactory.createStandardGame()` is called.
- **When**
  - The method returns a `GameState` instance.
- **Then**
  - The result is not `null`.
  - `getBoard()` is not `null`.
  - `getUnits()` is not `null`.

---

### GSF2 — Initial GameState uses a 5x5 board

- **Given**
  - A `GameState` from `createStandardGame()`.
- **When**
  - Reading `state.getBoard().getWidth()` and `getHeight()`.
- **Then**
  - `width == 5`
  - `height == 5`

---

### GSF3 — Default lineup has exactly 4 units

- **Given**
  - A `GameState` from `createStandardGame()`.
- **When**
  - Reading `state.getUnits()`.
- **Then**
  - `units.size() == 4`.

---

### GSF4 — Default unit IDs and owners

- **Given**
  - A `GameState` from `createStandardGame()`.
- **When**
  - Units are inspected by `id`.
- **Then**
  - There are exactly these units:
    - `u1_p1` with `owner = "P1"`
    - `u2_p1` with `owner = "P1"`
    - `u1_p2` with `owner = "P2"`
    - `u2_p2` with `owner = "P2"`
  - No duplicate IDs.

---

### GSF5 — Default starting positions match UNIT_TYPES_V1

- **Given**
  - A `GameState` from `createStandardGame()`.
- **When**
  - Reading each unit's `position`.
- **Then**
  - `u1_p1.position == (1, 0)`
  - `u2_p1.position == (3, 0)`
  - `u1_p2.position == (1, 4)`
  - `u2_p2.position == (3, 4)`

---

### GSF6 — Default stats for SWORDSMAN units

- **Given**
  - A `GameState` from `createStandardGame()`.
- **When**
  - Inspecting units with IDs `u1_p1` and `u1_p2` (SWORDSMAN).
- **Then**
  - For both:
    - `hp == 10`
    - `attack == 3`
    - `alive == true`

---

### GSF7 — Default stats for ARCHER units

- **Given**
  - A `GameState` from `createStandardGame()`.
- **When**
  - Inspecting units with IDs `u2_p1` and `u2_p2` (ARCHER).
- **Then**
  - For both:
    - `hp == 8`
    - `attack == 3`
    - `alive == true`

---

### GSF8 — Initial flags: currentPlayer, isGameOver, winner

- **Given**
  - A `GameState` from `createStandardGame()`.
- **When**
  - Reading:
    - `state.getCurrentPlayer()`
    - `state.isGameOver()`
    - `state.getWinner()`
- **Then**
  - `currentPlayer.value == "P1"` (Player 1 starts)
  - `isGameOver == false`
  - `winner == null`

---

### GSF9 — No TANK units in default lineup

- **Given**
  - A `GameState` from `createStandardGame()`.
- **When**
  - Inspecting all units.
- **Then**
  - No unit in the default lineup uses TANK stats:
    - There is **no** unit with `(hp == 16 && attack == 2)`.

> Note: This test documents that TANK is defined in UNIT_TYPES_V1 but **not yet used** in the standard starting layout.

---

## 5. UTDOC-Series — Spec Consistency Tests

These tests are partially conceptual (doc ↔ code consistency) and may be implemented either as:

- JUnit tests referring to constants, or
- Manual verification supported by comments and code review.

### UTDOC1 — Default lineup in code matches UNIT_TYPES_V1

- **Given**
  - The content of `UNIT_TYPES_V1.md`
  - The implementation of `GameStateFactory.createStandardGame()`.
- **Then**
  - The default units (IDs, owners, positions) in code match the table in `UNIT_TYPES_V1.md` §4.
  - Any future change to the default lineup should update both:
    - `UNIT_TYPES_V1.md`
    - `GameStateFactory`

---

### UTDOC2 — Type stats in code match UNIT_TYPES_V1

- **Given**
  - Type definitions in `UNIT_TYPES_V1.md`:
    - SWORDSMAN: hp=10, attack=3
    - ARCHER:    hp=8,  attack=3
    - TANK:      hp=16, attack=2
  - Constants or literals used in `GameStateFactory`.
- **Then**
  - The HP / ATK values used for SWORDSMAN and ARCHER in the default lineup match the document.
  - TANK stats, if present as constants, also match the document, even if unused in `createStandardGame()`.

---

## 6. Recommended JUnit Test Structure

Recommended test class:

- `GameStateFactoryTest`

Suggested structure:

```java
class GameStateFactoryTest {

    @Nested
    @DisplayName("GSF-Series - Default GameState creation")
    class DefaultGameStateTests {
        // GSF1 - GSF9
    }

    // UTDOC-Series can be covered by additional tests or comments, if desired.
}
```

Each test should:

- Call `GameStateFactory.createStandardGame()`
- Assert on:
  - Board size
  - Units count
  - Unit IDs / owners / positions
  - HP / attack / alive
  - currentPlayer, isGameOver, winner
  - Absence of TANK in default lineup

---

# End of UNIT_TYPES_TESTPLAN_V1.md
