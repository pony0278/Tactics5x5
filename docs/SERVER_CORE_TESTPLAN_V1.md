# SERVER_CORE_TESTPLAN_V1 — 5x5 Tactics Server Core Test Plan

## 1. Scope

This document defines the **test plan for the server core layer** in 5x5 Tactics V1.

Covered classes:

- `com.tactics.server.core.MatchRegistry`
- `com.tactics.server.core.MatchService`

Focus:

- In-memory match management
- GameState immutability
- Basic action application via `RuleEngine`
- Error handling for invalid actions

Not covered:

- WebSocket integration (`MatchWebSocketHandler`)
- JSON serialization / deserialization
- Frontend behavior

---

## 2. Conventions

### 2.1 Players and Match

- Players: `"P1"`, `"P2"` (wrapped in `PlayerId`)
- Match ID example: `"match-1"`

### 2.2 Engine Elements

- `GameState` created with:
  - `Board(5, 5)`
  - `List<Unit>` (can be empty or customized for tests)
  - `currentPlayer` as `PlayerId("P1")` or `PlayerId("P2")`
  - `isGameOver` and `winner` as appropriate

### 2.3 RuleEngine Usage in Tests

For `MatchService` tests, two approaches are possible:

- Use the real `RuleEngine` with simple states and actions, **or**
- Use a fake/minimal `RuleEngine` implementation that:
  - Accepts a known action and returns a predictable `GameState`
  - Rejects invalid actions

The test plan assumes either approach as long as:

- You can trigger both valid and invalid cases
- You can assert that state is updated correctly

---

## 3. MatchRegistry Tests (MR-Series)

### MR1 — getMatch on Empty Registry Returns Null

- **Given**:
  - `MatchRegistry` created with empty map
- **When**:
  - `getMatch("match-1")`
- **Then**:
  - Returns `null`

---

### MR2 — createMatch Adds New Match

- **Given**:
  - Empty `MatchRegistry`
  - Initial `GameState` with:
    - `Board(5, 5)`
    - empty units
    - `currentPlayer = P1`
- **When**:
  - `createMatch("match-1", initialState)`
- **Then**:
  - `getMatch("match-1")` returns non-null `Match`
  - `match.getMatchId().getValue().equals("match-1")`
  - `match.getState()` equals `initialState`
  - `match.getConnections()` is empty (or as created by implementation)

---

### MR3 — createMatch Overwrites Existing Match (If Implemented As Such)

- **Given**:
  - Registry with existing `match-1`
- **When**:
  - `createMatch("match-1", newState)`
- **Then**:
  - `getMatch("match-1").getState()` equals `newState`

(If the chosen behavior is to **not** overwrite, adjust expected result accordingly. The test should assert the chosen behavior.)

---

### MR4 — updateMatchState Replaces Match State (Immutability)

- **Given**:
  - `MatchRegistry` with `match-1 → Match(oldState)`
  - `newState` is a different `GameState` instance
- **When**:
  - `updateMatchState("match-1", newState)`
- **Then**:
  - `getMatch("match-1").getState()` equals `newState`
  - The reference to the `Match` object may change or remain the same, but:
    - The internal `GameState` reference must be updated
    - No in-place mutation of `oldState` is allowed (GameState is immutable)

---

### MR5 — listMatches Returns All Active Matches

- **Given**:
  - Multiple matches created:
    - `"match-1"`, `"match-2"`
- **When**:
  - `listMatches()`
- **Then**:
  - Collection size is 2
  - Contains both match IDs (via match.getMatchId().getValue())

---

## 4. MatchService Tests (MS-Series)

Assume a `MatchService` constructed with:

- A `MatchRegistry` instance
- A `RuleEngine` instance
- A `GameStateSerializer` instance (not heavily used in this test plan)

### MS1 — getOrCreateMatch Creates New Match When Absent

- **Given**:
  - Empty `MatchRegistry`
- **When**:
  - `getOrCreateMatch("match-1")`
- **Then**:
  - Returns non-null `Match`
  - `match.getMatchId().getValue().equals("match-1")`
  - `match.getState()` is a default initial state, e.g.:
    - `Board(5, 5)`
    - empty units
    - `currentPlayer = P1`
    - `isGameOver = false`

---

### MS2 — getOrCreateMatch Returns Existing Match on Second Call

- **Given**:
  - First call to `getOrCreateMatch("match-1")` already executed
- **When**:
  - Call `getOrCreateMatch("match-1")` again
- **Then**:
  - Both returned `Match` instances refer to the **same underlying match**
    (or at least the same `matchId` and state)
  - Registry size remains 1

---

### MS3 — findMatch Returns Null If Match Does Not Exist

- **Given**:
  - `MatchRegistry` has no `"unknown-match"`
- **When**:
  - `findMatch("unknown-match")`
- **Then**:
  - Returns `null`

---

### MS4 — findMatch Returns Existing Match

- **Given**:
  - A match `"match-1"` created via `getOrCreateMatch`
- **When**:
  - `findMatch("match-1")`
- **Then**:
  - Returns the same `Match` as `getOrCreateMatch("match-1")`

---

### MS5 — getCurrentState Returns Match State

- **Given**:
  - `"match-1"` exists with a known `GameState`
- **When**:
  - `getCurrentState("match-1")`
- **Then**:
  - Returns the exact `GameState` stored in the match

---

### MS6 — getCurrentState Returns Null for Unknown Match

- **Given**:
  - No match with id `"unknown"`
- **When**:
  - `getCurrentState("unknown")`
- **Then**:
  - Returns `null` (or throws an exception, depending on chosen behavior)
  - Test must assert the behavior used in implementation.

---

## 5. MatchService.applyAction Tests (MSA-Series)

These tests verify that `applyAction` uses `RuleEngine` correctly and updates `MatchRegistry`.

For simplicity, you may:

- Use a minimal real GameState and a real RuleEngine, with a single unit.
- Or use a fake RuleEngine that returns a predictable new state for a given action.

### MSA1 — applyAction Applies Valid MOVE and Updates State

- **Given**:
  - `"match-1"` with initial `GameState` where:
    - `currentPlayer = P1`
    - At least one P1 unit exists at position `(1,1)`
  - A valid MOVE `Action` moving that unit to `(1,2)`
- **When**:
  - Call `applyAction("match-1", PlayerId("P1"), action)`
- **Then**:
  - Returned `GameState` reflects the MOVE
  - `MatchRegistry.getMatch("match-1").getState()` is the same new `GameState`
  - Original `GameState` instance is unchanged (immutability)

---

### MSA2 — applyAction Applies Valid ATTACK and Updates State

- **Given**:
  - `"match-1"` with a state where:
    - P1 unit adjacent to P2 unit
  - A valid ATTACK action
- **When**:
  - `applyAction("match-1", PlayerId("P1"), action)`
- **Then**:
  - New `GameState` shows decreased HP for target
  - Registry state updated to this new state
  - Original state remains unchanged

---

### MSA3 — applyAction Applies END_TURN and Switches currentPlayer

- **Given**:
  - `"match-1"` with `currentPlayer = P1`
- **When**:
  - `applyAction("match-1", PlayerId("P1"), END_TURN action)`
- **Then**:
  - New state has `currentPlayer = P2`
  - Registry updated
  - Original state unchanged

---

### MSA4 — applyAction Throws on Validation Failure

- **Given**:
  - `"match-1"` with `currentPlayer = P1`
  - An action from `playerId = P2` (wrong turn) or any invalid action
- **When**:
  - `applyAction("match-1", PlayerId("P2"), invalidAction)`
- **Then**:
  - Method throws `IllegalArgumentException` (as per current implementation)
  - Registry state is **not** changed
  - Test asserts:
    - exception type
    - exception message contains validation error text

---

### MSA5 — applyAction Creates Match if Not Existing (If Implemented)

- **Given**:
  - No `"match-1"` in registry
  - You call `applyAction("match-1", PlayerId("P1"), someAction)`
- **Then**:
  - Behavior depends on implementation:
    - If `applyAction` expects match to exist, it may throw.
    - If `applyAction` calls `getOrCreateMatch`, it may create a default match.
  - The test must assert whichever behavior is implemented.
  - This test exists mainly to **lock in that behavior**.

---

## 6. Immutability & Consistency Tests (MSC-Series)

### MSC1 — applyAction Does Not Mutate Old GameState

- **Given**:
  - Save reference to `oldState = match.getState()`
- **When**:
  - Call `applyAction(...)` and get `newState`
- **Then**:
  - `newState != oldState`
  - `match.getState() == newState` after update
  - All assertions performed on a copy; oldState fields remain unchanged

---

### MSC2 — MatchRegistry Does Not Leak Mutable Collections

- **Given**:
  - Get `matches = matchRegistry.getMatches()`
- **When**:
  - Attempt to modify `matches` (if possible)
- **Then**:
  - Behavior depends on implementation:
    - If unmodifiable, test expects `UnsupportedOperationException`
    - If modifiable, test ensures that only controlled APIs are used in production
  - This test is optional and depends on how defensive you want the registry to be.

---

## 7. Suggested Test Class Layout

Recommended JUnit 5 test classes:

- `MatchRegistryTest`
  - Tests MR1–MR5
- `MatchServiceTest`
  - Tests MS1–MS6, MSA1–MSA5, MSC1–MSC2

Structure:

- Use `@Nested` classes for grouping (e.g. `@Nested class RegistryBasics`, `@Nested class ApplyAction`).
- Use `@DisplayName` with IDs (e.g. `"MR1 - getMatch on empty registry returns null"`).

---

# End of SERVER_CORE_TESTPLAN_V1.md
