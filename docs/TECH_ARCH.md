# TECH_ARCH – 5x5 Tactics Engine & Server Architecture  
Version: 1.0  
Status: Approved for Implementation  
Author: (Your Name)  
Purpose: Provide a fully modular technical architecture for a reusable, standalone tactics engine that can integrate with other games such as SPD.

---

# 1. High-Level Goals

The project consists of **two main layers**:

1. **Tactics Engine (Core Library)**
   - Pure Java module
   - Contains all game rules, state, actions, resolution, serialization
   - 100% independent from networking, UI, and frameworks
   - Deterministic execution to support replay, simulation, and AI bots
   - Usable by:
     - Web PvP server
     - SPD (Shattered Pixel Dungeon) for cross-game integration
     - Offline testing tools
     - Any future client or platform

2. **Tactics Server (WebSocket)**
   - Spring Boot WebSocket service
   - Acts as an adapter between clients and the engine
   - Translates JSON ↔ Action ↔ GameState
   - Room/match management
   - Stateless except for in-memory matches

Future possible clients:
- Web browser (HTML/JS)
- Desktop/CLI simulation
- Mobile app
- SPD integration mode

---

# 2. Repository Structure
tactics5x5/
engine/ # Pure Java library (game logic)
src/main/java/com/tactics/engine/...
src/test/java/com/tactics/engine/...
server/ # Spring Boot WebSocket backend
src/main/java/com/tactics/server/...
client/ 

# Minimal HTML/JS reference client
index.html
game.js
style.css
docs/ 
# All specs (architecture, protocol, rules)
TECH_ARCH.md
GAME_RULES_V1.md
WS_PROTOCOL_V1.md
tools/ 

# Optional simulation / analysis tools

---

# 3. Module Responsibilities

## 3.1 Tactics Engine (Core Logic)

**Location:** `/engine/`  
**Package root:** `com.tactics.engine`

### Engine responsibilities:
- Define the **complete domain model**:
  - `GameState`
  - `Board`
  - `Unit`
  - `Action`
  - `ActionType`
  - `PlayerId`
- Implement all **game rules** inside `RuleEngine`
  - Movement rules
  - Attack rules
  - Target selection
  - Turn rotation
  - Win/loss condition
- Provide deterministic behavior:
  - All randomness (if added later) must come from injected RNG providers
- Provide core utilities:
  - Deep-copy of state (`GameState.clone()`)
  - Serialization (JSON-friendly representation)
  - Replay support (sequence of `Action` objects)

### Engine restrictions:
- Must **not** import:
  - Spring
  - WebSocket APIs
  - UI libraries
- Must be:
  - Deterministic
  - Pure logic
  - Serializable

### Engine future extension points:
- Hero classes (Warrior/Archer/Mage/Rogue)
- Minion roles (Tank/Assassin/Ranger)
- Skill system
- Buff system
- Terrain & obstacles
- Shrinking zone mechanic
- SPD cross-game integration

---

## 3.2 Tactics Server (WebSocket Server)
**Location:** `/server/`  
**Package root:** `com.tactics.server`

### Responsibilities:
- WebSocket connection handling
- Assign players into GameRooms (2 players per match)
- Translate incoming JSON → `Action`
- Call Engine:
  - `validateAction()`
  - `applyAction()`
- Broadcast:
  - Updated `GameState`
  - Error messages
  - Game over messages

### Server restrictions:
- Server does **not** contain game rules
- All rule decisions come strictly from Engine
- Server must be stateless outside GameRoom objects

A GameRoom contains:
```java
class GameRoom {
    String roomId;
    Session player1;
    Session player2;
    GameState state;
}

3.3 Client (Reference Web UI)
Location: /client/
Responsibilities:
Connect to WebSocket server
Render 5×5 board
Handle click interactions:
Select unit
Select target tile
Build JSON action request
Receive game_state update and re-render

Restrictions:
No rule computation
No game logic beyond UI behaviors

4. Engine Architecture Details
4.1 Recommended Package Layout
com.tactics.engine/
  model/          # Data structures only (Unit, GameState, Board)
  rules/          # RuleEngine, validation logic
  action/         # Action, validators, transformers
  util/           # Serialization, deep-copy, RNG providers
  sim/            # Optional: bots, search, playout tools

4.2 Core Classes (V1)

GameState
Immutable preferred (return new state after each action)
Contains:
Board dimensions
List of Units
Whose turn it is
Game over flag and winner
Round counter (optional but recommended)

Must be serializable (JSON mapping friendly)

Unit
Contains:
id
owner
hp
attack
position
alive
Action

Represents player intention:
MOVE
ATTACK
MOVE_AND_ATTACK
END_TURN
RuleEngine
Core validating and state-transition logic

Required methods:
ValidationResult validateAction(GameState state, Action action);
GameState applyAction(GameState state, Action action);

5. Determinism Requirements
To support simulations, replays, and SPD integration:
Engine execution must be deterministic given:
Initial GameState
Sequence of Action objects
All randomness (future features):
Must use injected RandomGenerator
Never use new Random() inside Engine

This allows:
Replay system
Turn-by-turn logging
Predictable AI simulations
SPD → Tactics battle → SPD result round trip

6. Serialization & Replay
Engine should optionally provide:
GameStateSerializer
JSON encode/decode
Useful for:
Saving match logs
Debugging
SPD integration
ReplayRecord

Contains:
Initial state snapshot
Sequence<Action>

Usage:
Server logs actions
Client can request replay
SPD can embed a Tactics match and replay it later

7. Integration With Other Games (SPD Example)
The Engine must allow external games to interact by providing:
Adapter pattern workflow:
SPD converts its combatants into Tactics Units
SPD creates a custom initial GameState
SPD executes:
RuleEngine.applyAction(...)
Engine returns results (winner, HP changes, logs)
SPD converts results back into its own domain (loot, XP, HP changes)
This requires:
No dependency on SPD for Engine
Clean and stable public API
Determinism guaranteed

8. Data Flow Diagram
[Browser Client] → JSON Action → [Server] → Action → [Engine]
                                   ↓                   ↑
                                   JSON GameState ←─────

9. Testing Strategy
Engine tests:
100% deterministic unit tests
Test:
Move validation
Attack validation
Turn switching
Win condition
Illegal action handling
Server tests:
WebSocket connection tests
Room assignment tests
Client tests:
Manual/UI only (prototype stage)

10. Future Extensions
The architecture supports gradual expansion:
Gameplay:
Hero classes
Minion classes
Buffs & obstacles
Cooldowns
Cards/abilities
Fog of war
Shrinking battlefield
PvP matchmaking
Ranking MMR
Technical:
Database persistence
Cloud deployment
Play-by-play replay UI
AI agents

11. Summary
This architecture ensures:
Engine is completely independent and reusable
Server is thin and stateless
Clients are replaceable
Future games (e.g., SPD) can easily integrate via adapters
Development can proceed via vibecoding workflows
This document defines the foundation for immediate implementation.