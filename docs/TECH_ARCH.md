# TECH_ARCH — 5x5 Tactics Architecture Specification

## 1. Overview

5x5 Tactics is a deterministic, server-authoritative, turn-based tactics game.
The system is divided into three cleanly separated layers:

- **Engine** — Pure simulation and game logic  
- **Server** — Authoritative match controller  
- **Client** — UI only; renders state from the server  

Each layer has strict boundaries and may not depend on layers above it.

---

## 2. High-Level Architecture

```
Client → Server → Engine
```

### Engine
- Pure logic, deterministic, immutable  
- No I/O  
- No networking  
- No randomness except via injected RNG  
- Fully serializable  
- No global state  

### Server
- Hosts WebSocket endpoint  
- Maintains GameState  
- Validates actions  
- Applies actions via RuleEngine  
- Broadcasts updates to all clients  

### Client
- Renders UI  
- Sends user actions  
- Receives authoritative state updates  
- No duplicated rules  
- No prediction logic  

---

## 3. Determinism Requirements

Determinism is a core requirement.

Given:
- Initial GameState  
- RNG seed (if used)  
- Sequence of player actions  

The entire match must always produce the same output.

Therefore, the Engine must:
- Be pure  
- Avoid side effects  
- Return new GameState for every change  
- Avoid system time  
- Avoid non-deterministic operations  

This enables:
- Replay  
- Debugging  
- Server-side resimulation  
- Future bot training  
- Competitive fairness  

---

## 4. Engine Responsibilities

The Engine includes:

### Core Models
- GameState  
- Unit  
- Board  
- Position  
- PlayerId  

### Action System
- Action  
- ActionType  

### Rules
- RuleEngine  
- ValidationResult  

### Utilities
- Serialization  
- Deterministic RNG wrapper  

Engine must not:
- Perform I/O  
- Use WebSocket logic  
- Use configuration files  
- Use reflection or system-level operations  

---

## 5. Server Responsibilities

Server is the authoritative orchestrator.

Server must:
- Maintain one GameState per active match  
- Accept player actions  
- Call `validateAction`  
- Call `applyAction`  
- Reject illegal commands  
- Send updated GameState to clients  

Server must not:
- Implement movement rules  
- Implement attack rules  
- Calculate damage  
- Determine turn order  

All gameplay logic resides in the Engine.

---

## 6. Client Responsibilities

Client is a thin rendering layer.

Client must:
- Display the game board  
- Display units & status  
- Allow user input  
- Send WebSocket messages  
- Receive state updates  
- Render GameState  

Client must not:
- Decide legal moves  
- Apply damage  
- Run simulation  
- Predict turns  

Client is always fully dependent on the Server.

---

## 7. Core Engine Data Models

### GameState
Represents full match state:
- Board  
- Units list  
- Current player  
- Game-over flag  
- Winner  

GameState is immutable.

### Board
Represents a 5×5 grid.

### Unit
Contains:
- id  
- owner  
- hp  
- attack  
- position  
- alive  

No additional fields allowed unless added to the design documents.

### PlayerId
Strongly typed identifier for players.

---

## 8. Action System

### ActionType
Allowed values:
- MOVE  
- ATTACK  
- MOVE_AND_ATTACK  
- END_TURN  

### Action
Fields:
- type  
- playerId  
- targetPosition (nullable)  
- targetUnitId (nullable)  

Action only expresses intent.  
No logic inside Action classes.

---

## 9. RuleEngine Specification

`RuleEngine` is the only way to mutate GameState.

Methods:
```
ValidationResult validateAction(GameState state, Action action)
GameState applyAction(GameState state, Action action)
```

Engine must:
- Never modify GameState in-place  
- Apply rules strictly based on GAME_RULES_V1  
- Keep validation consistent with the WebSocket Protocol  

---

## 10. Serialization Requirements

`GameStateSerializer` must:
- Convert GameState → JSON-safe Map  
- Convert Map → GameState  
- Produce deterministic output  
- Be stable across versions  
- Contain no logic outside serialization  

Used for:
- WebSocket transmission  
- Replays  
- Debugging tools  
- Testing  

---

## 11. Replay System Requirements

A match must be fully reproducible from:

- Initial GameState  
- RNG seed  
- Ordered list of Actions  

This enables:
- Tournament logs  
- Spectator mode  
- Server resimulation  
- AI training  
- Anti-cheat comparison  

---

## 12. Security Model

- The Server is always authoritative  
- Client input is never trusted  
- All validation occurs server-side  
- Engine must assume inputs are hostile  
- Replay logs are the single source of truth  

---

## 13. Extensibility Strategy

Architecture must support future modules:

- New unit types  
- Item & equipment system  
- Buff & debuff system  
- Terrain system  
- Fog-of-war  
- SPD bridging / shared arena  
- Bots & AI players  

All must integrate **without breaking determinism**.

ENGINE should remain stable and unchanged unless strictly necessary.

---

## 14. Development Principles

- Engine first, server second, client last  
- No rules outside Engine  
- No state mutation outside RuleEngine  
- Determinism over convenience  
- Strong modular boundaries  
- No duplication of logic  
- Prefer composition over inheritance  
- Everything must be serializable  
- Avoid magic numbers  
- Never violate Action or GameState formats  

---

## 15. Summary

This TECH_ARCH defines:
- The responsibilities of Engine, Server, and Client  
- Determinism and replay requirements  
- The allowed data models  
- The action system design  
- The role of RuleEngine  
- The serialization contract  
- Long-term extensibility strategy  

All other documents (ENGINE_SKELETON_V1, GAME_RULES_V1, WS_PROTOCOL_V1) must conform to this architecture.

---

# End of TECH_ARCH.md
