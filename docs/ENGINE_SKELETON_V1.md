# ENGINE_SKELETON_V1.md
Engine Class & Package Skeleton (Version 1)

This document provides the mid-level specification for the 5x5 Tactics engine.
All implementation work must strictly follow this skeleton and must not introduce
any new classes, new fields, or new methods beyond what is defined here.

The engine must be deterministic, side-effect free, and fully serializable.

---

# 1. Package Structure

The engine is organized into the following packages:

com.tactics.engine
├─ model/ # GameState, Board, Unit, PlayerId
├─ action/ # Action, ActionType
├─ rules/ # RuleEngine, ValidationResult
└─ util/ # Serialization, RNG provider

---

# 2. model/

## 2.1 GameState
**Responsibility:**  
Represents the complete state of an ongoing match.

**Fields (exactly these):**
- Board board
- List<Unit> units
- PlayerId currentPlayer
- boolean isGameOver
- PlayerId winner  (nullable)

**Methods:**
- getters only  
- no mutation  
- state transitions occur only via RuleEngine.applyAction

---

## 2.2 Board
**Responsibility:**  
Defines the 5×5 grid.

**Fields:**
- int width
- int height

(Initially fixed to 5×5 but still stored as fields.)

---

## 2.3 Unit
**Responsibility:**  
Represents a unit on the board.

**Fields (no more, no less):**
- String id
- PlayerId owner
- int hp
- int attack
- Position position
- boolean alive

( `Position` is a value object with x, y. )

---

## 2.4 PlayerId
**Responsibility:**  
Strong typing wrapper for player identity.

**Fields:**
- String value

---

# 3. action/

## 3.1 ActionType
Enum values:
- MOVE
- ATTACK
- MOVE_AND_ATTACK
- END_TURN

(No other actions allowed.)

---

## 3.2 Action
Represents a player-issued command.

**Fields:**
- ActionType type
- PlayerId playerId
- Position targetPosition   (nullable, depends on action)
- String targetUnitId       (nullable)

(These fields map 1-to-1 to WS_PROTOCOL message definitions.)

---

# 4. rules/

## 4.1 RuleEngine
**Responsibility:**  
Validates and applies actions.

**Methods:**
- ValidationResult validateAction(GameState state, Action action)
- GameState applyAction(GameState state, Action action)

No helpers, no additional methods.

---

## 4.2 ValidationResult
**Fields:**
- boolean isValid
- String errorMessage

---

# 5. util/

## 5.1 GameStateSerializer
**Responsibility:**  
Convert GameState to/from a JSON-friendly map structure.

**Methods:**
- Map<String, Object> toJsonMap(GameState state)
- GameState fromJsonMap(Map<String, Object> map)

---

## 5.2 RngProvider
**Responsibility:**  
Provide deterministic randomness for future features.

**Methods:**
- int nextInt(int bound)

(Unused in V1, but required for determinism.)

---

# End of ENGINE_SKELETON_V1.md
