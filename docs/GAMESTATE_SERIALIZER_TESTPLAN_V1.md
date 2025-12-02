# GAMESTATE_SERIALIZER_TESTPLAN_V1 — 5x5 Tactics GameState Serializer Test Plan

## 1. Scope

This document defines the **test plan for GameStateSerializer** in 5x5 Tactics V1.

Covered class:

- `com.tactics.engine.util.GameStateSerializer`

Focus:

- `toJsonMap`: Converting `GameState` to `Map<String, Object>` for JSON serialization
- `fromJsonMap`: Reconstructing `GameState` from `Map<String, Object>`
- Roundtrip consistency: `GameState` → Map → `GameState`
- Edge cases: empty units, game over states, null winner

Not covered:

- Actual JSON string serialization (handled by `JsonHelper`)
- WebSocket message handling
- Client-side rendering

---

## 2. Conventions

### 2.1 Expected JSON Structure

Per `CLIENT_WIREFRAME_V1`, the serialized `GameState` must produce this JSON-compatible map structure:

```json
{
  "board": { "width": 5, "height": 5 },
  "units": [
    {
      "id": "u1_p1",
      "owner": "P1",
      "hp": 10,
      "attack": 3,
      "position": { "x": 1, "y": 1 },
      "alive": true
    }
  ],
  "currentPlayer": "P1",
  "gameOver": false,
  "winner": null
}
```

### 2.2 Field Mappings

| GameState Field | Map Key | Value Type |
|-----------------|---------|------------|
| `board` | `"board"` | `Map<String, Object>` with `width`, `height` |
| `units` | `"units"` | `List<Map<String, Object>>` |
| `currentPlayer` | `"currentPlayer"` | `String` (PlayerId value) |
| `isGameOver` | `"gameOver"` | `Boolean` |
| `winner` | `"winner"` | `String` or `null` |

### 2.3 Unit Field Mappings

| Unit Field | Map Key | Value Type |
|------------|---------|------------|
| `id` | `"id"` | `String` |
| `owner` | `"owner"` | `String` (PlayerId value) |
| `hp` | `"hp"` | `Integer` |
| `attack` | `"attack"` | `Integer` |
| `position` | `"position"` | `Map<String, Object>` with `x`, `y` |
| `alive` | `"alive"` | `Boolean` |

### 2.4 Test Data

Standard test data:

- Board: `Board(5, 5)`
- Unit example: `Unit("u1", PlayerId("P1"), 10, 3, Position(2, 2), true)`
- Current player: `PlayerId("P1")` or `PlayerId("P2")`

### 2.5 Conceptual Test Mappings

| Concept ID | Description |
|------------|-------------|
| GS-SIMPLE | A minimal valid GameState: 5x5 board, empty units, P1 current player, not game over, no winner |
| GS-WITH-UNIT | GameState with at least one unit present |
| GS-GAME-OVER | GameState with `isGameOver = true` and a winner set |
| GS-DRAW | GameState with `isGameOver = true` but `winner = null` |
| GS-ROUNDTRIP | `original → toJsonMap → fromJsonMap → reconstructed` preserves all fields |
| MAP-VALID | A complete map with all required keys: `board`, `units`, `currentPlayer`, `gameOver`, `winner` |
| MAP-INVALID | A map missing one or more required keys, or containing invalid values |

---

## 3. toJsonMap Tests (TJM-Series)

### TJM1 — Board Serialization

- **Given**:
  - `GameState` with `Board(5, 5)`, empty units, `currentPlayer = P1`, `isGameOver = false`, `winner = null`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - Result contains key `"board"`
  - `result.get("board")` is a `Map` with:
    - `"width"` = 5
    - `"height"` = 5

---

### TJM2 — CurrentPlayer Serialization

- **Given**:
  - `GameState` with `currentPlayer = PlayerId("P1")`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("currentPlayer")` equals `"P1"`

---

### TJM3 — CurrentPlayer P2 Serialization

- **Given**:
  - `GameState` with `currentPlayer = PlayerId("P2")`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("currentPlayer")` equals `"P2"`

---

### TJM4 — GameOver False Serialization

- **Given**:
  - `GameState` with `isGameOver = false`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("gameOver")` equals `false`

---

### TJM5 — GameOver True Serialization

- **Given**:
  - `GameState` with `isGameOver = true`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("gameOver")` equals `true`

---

### TJM6 — Winner Null Serialization

- **Given**:
  - `GameState` with `winner = null`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("winner")` is `null`

---

### TJM7 — Winner P1 Serialization

- **Given**:
  - `GameState` with `isGameOver = true`, `winner = PlayerId("P1")`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("winner")` equals `"P1"`

---

### TJM8 — Empty Units List Serialization

- **Given**:
  - `GameState` with empty `units` list
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("units")` is an empty `List`

---

### TJM9 — Single Unit Serialization

- **Given**:
  - `GameState` with one unit:
    - `Unit("u1_p1", PlayerId("P1"), 10, 3, Position(2, 2), true)`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("units")` is a `List` with size 1
  - First element is a `Map` with:
    - `"id"` = `"u1_p1"`
    - `"owner"` = `"P1"`
    - `"hp"` = 10
    - `"attack"` = 3
    - `"position"` is a `Map` with `"x"` = 2, `"y"` = 2
    - `"alive"` = `true`

---

### TJM10 — Multiple Units Serialization

- **Given**:
  - `GameState` with two units:
    - `Unit("u1", PlayerId("P1"), 10, 3, Position(0, 0), true)`
    - `Unit("u2", PlayerId("P2"), 8, 4, Position(4, 4), true)`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `result.get("units")` is a `List` with size 2
  - Both units are represented with correct field values

---

### TJM11 — Dead Unit Serialization

- **Given**:
  - `GameState` with a dead unit:
    - `Unit("u1", PlayerId("P1"), 0, 3, Position(1, 1), false)`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - Unit map contains `"alive"` = `false`
  - Unit map contains `"hp"` = 0

---

### TJM12 — Position Serialization

- **Given**:
  - Unit at `Position(3, 4)`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - Unit's `"position"` map contains:
    - `"x"` = 3
    - `"y"` = 4

---

## 4. fromJsonMap Tests (FJM-Series)

All tests in this section use **complete, valid maps** (MAP-VALID) containing all required keys with correctly typed values. Invalid inputs are covered in the Edge Case Tests (EC-Series).

### FJM1 — Board Deserialization

- **Given**:
  - Map with `"board"` = `{ "width": 5, "height": 5 }`
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned `GameState` has:
    - `board.getWidth()` = 5
    - `board.getHeight()` = 5

---

### FJM2 — CurrentPlayer Deserialization

- **Given**:
  - Map with `"currentPlayer"` = `"P1"`
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned `GameState` has:
    - `currentPlayer.getValue()` equals `"P1"`

---

### FJM3 — GameOver Deserialization

- **Given**:
  - Map with `"gameOver"` = `true`
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned `GameState` has `isGameOver()` = `true`

---

### FJM4 — Winner Deserialization

- **Given**:
  - Map with `"winner"` = `"P2"`
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned `GameState` has `winner.getValue()` = `"P2"`

---

### FJM5 — Winner Null Deserialization

- **Given**:
  - Map with `"winner"` = `null`
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned `GameState` has `getWinner()` = `null`

---

### FJM6 — Empty Units Deserialization

- **Given**:
  - Map with `"units"` = empty list
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned `GameState` has empty `units` list

---

### FJM7 — Single Unit Deserialization

- **Given**:
  - Map with one unit in `"units"`:
    ```
    {
      "id": "u1",
      "owner": "P1",
      "hp": 10,
      "attack": 3,
      "position": { "x": 2, "y": 3 },
      "alive": true
    }
    ```
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned `GameState` has 1 unit with:
    - `id` = `"u1"`
    - `owner.getValue()` = `"P1"`
    - `hp` = 10
    - `attack` = 3
    - `position.getX()` = 2
    - `position.getY()` = 3
    - `alive` = `true`

---

### FJM8 — Multiple Units Deserialization

- **Given**:
  - Map with two units in `"units"` list
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned `GameState` has 2 units
  - Each unit has correct field values

---

### FJM9 — Dead Unit Deserialization

- **Given**:
  - Map with unit where `"alive"` = `false`, `"hp"` = 0
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Returned unit has `isAlive()` = `false`
  - Returned unit has `getHp()` = 0

---

## 5. Roundtrip Tests (RT-Series)

### RT1 — Basic Roundtrip with Empty Units

- **Given**:
  - Original `GameState` with:
    - `Board(5, 5)`
    - empty units
    - `currentPlayer = P1`
    - `isGameOver = false`
    - `winner = null`
- **When**:
  - `map = toJsonMap(original)`
  - `reconstructed = fromJsonMap(map)`
- **Then**:
  - `reconstructed.getBoard().getWidth()` = 5
  - `reconstructed.getBoard().getHeight()` = 5
  - `reconstructed.getUnits()` is empty
  - `reconstructed.getCurrentPlayer().getValue()` = `"P1"`
  - `reconstructed.isGameOver()` = `false`
  - `reconstructed.getWinner()` = `null`

---

### RT2 — Roundtrip with Single Unit

- **Given**:
  - Original `GameState` with one unit:
    - `Unit("u1", PlayerId("P1"), 10, 3, Position(2, 2), true)`
- **When**:
  - `map = toJsonMap(original)`
  - `reconstructed = fromJsonMap(map)`
- **Then**:
  - Reconstructed state has 1 unit with matching fields

---

### RT3 — Roundtrip with Multiple Units

- **Given**:
  - Original `GameState` with 3 units (P1 x2, P2 x1)
- **When**:
  - Roundtrip serialization
- **Then**:
  - All 3 units present with correct fields

---

### RT4 — Roundtrip with Game Over State

- **Given**:
  - Original `GameState` with:
    - `isGameOver = true`
    - `winner = PlayerId("P1")`
- **When**:
  - Roundtrip serialization
- **Then**:
  - `reconstructed.isGameOver()` = `true`
  - `reconstructed.getWinner().getValue()` = `"P1"`

---

### RT5 — Roundtrip with Dead Unit

- **Given**:
  - Original `GameState` with one dead unit:
    - `Unit("u1", PlayerId("P2"), 0, 3, Position(1, 1), false)`
- **When**:
  - Roundtrip serialization
- **Then**:
  - Reconstructed unit has `isAlive()` = `false`
  - Reconstructed unit has `getHp()` = 0

---

### RT6 — Roundtrip with P2 as Current Player

- **Given**:
  - Original `GameState` with `currentPlayer = P2`
- **When**:
  - Roundtrip serialization
- **Then**:
  - `reconstructed.getCurrentPlayer().getValue()` = `"P2"`

---

### RT7 — Roundtrip Preserves Unit Order

- **Given**:
  - Original `GameState` with units in specific order: `["u1", "u2", "u3"]`
- **When**:
  - Roundtrip serialization
- **Then**:
  - Reconstructed units are in the same order by ID

---

## 6. Edge Case Tests (EC-Series)

This section tests **invalid inputs** (MAP-INVALID) and null handling. The serializer must fail fast with clear exceptions rather than returning null or silently ignoring errors.

### EC1 — Null GameState to toJsonMap

- **Given**:
  - `state = null`
- **When**:
  - `toJsonMap(null)`
- **Then**:
  - Throws `IllegalArgumentException` with message `"GameState cannot be null"`

---

### EC2 — Null Map to fromJsonMap

- **Given**:
  - `map = null`
- **When**:
  - `fromJsonMap(null)`
- **Then**:
  - Throws `IllegalArgumentException` with message `"Map cannot be null"`

---

### EC3 — Map Missing Required Fields

- **Given**:
  - Map missing `"board"` key
- **When**:
  - `fromJsonMap(map)`
- **Then**:
  - Throws `IllegalArgumentException` with message indicating the missing field

---

### EC4 — Board with Non-Standard Dimensions

- **Given**:
  - `GameState` with `Board(10, 10)` (non-5x5)
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `"board"` map contains `"width"` = 10, `"height"` = 10
  - Serializer should not assume 5x5

---

### EC5 — Unit at Edge Position (0,0)

- **Given**:
  - Unit at `Position(0, 0)`
- **When**:
  - Roundtrip serialization
- **Then**:
  - Position preserved as `x=0`, `y=0`

---

### EC6 — Unit at Edge Position (4,4)

- **Given**:
  - Unit at `Position(4, 4)`
- **When**:
  - Roundtrip serialization
- **Then**:
  - Position preserved as `x=4`, `y=4`

---

### EC7 — Unit with Zero Attack

- **Given**:
  - `Unit("u1", PlayerId("P1"), 10, 0, Position(1, 1), true)`
- **When**:
  - Roundtrip serialization
- **Then**:
  - Unit `attack` preserved as 0

---

### EC8 — Game Over with No Winner (Draw)

- **Given**:
  - `GameState` with `isGameOver = true`, `winner = null`
- **When**:
  - `toJsonMap(state)`
- **Then**:
  - `"gameOver"` = `true`
  - `"winner"` = `null`

---

## 7. Suggested Test Class Layout

Recommended JUnit 5 test class:

- `GameStateSerializerTest`

Structure:

```java
@Nested
@DisplayName("toJsonMap Tests (TJM-Series)")
class ToJsonMapTests {
    // TJM1 - TJM12
}

@Nested
@DisplayName("fromJsonMap Tests (FJM-Series)")
class FromJsonMapTests {
    // FJM1 - FJM9
}

@Nested
@DisplayName("Roundtrip Tests (RT-Series)")
class RoundtripTests {
    // RT1 - RT7
}

@Nested
@DisplayName("Edge Case Tests (EC-Series)")
class EdgeCaseTests {
    // EC1 - EC8
}
```

Use `@DisplayName` with IDs, e.g.:
- `"TJM1 - Board serialization"`
- `"FJM7 - Single unit deserialization"`
- `"RT4 - Roundtrip with game over state"`

---

# End of GAMESTATE_SERIALIZER_TESTPLAN_V1.md
