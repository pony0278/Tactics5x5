# SERVER_TECH_ARCH_V1 — 5x5 Tactics Server Architecture Specification

## 1. Scope

This document defines the **server-layer architecture** for 5x5 Tactics (V1).
It specifies:

- Server responsibilities
- Match lifecycle
- State management
- WebSocket message handling
- Player action validation & application flow
- Error handling rules
- In-memory data structures
- Determinism & engine boundaries
- Future-proofing for expansion

The server must strictly follow:

- `TECH_ARCH.md`
- `ENGINE_SKELETON_V1.md`
- `GAME_RULES_V1.md`
- `WS_PROTOCOL_V1.md`

The server must **not** implement gameplay logic.
All gameplay rules must be delegated to the Engine (`RuleEngine`).

---

## 2. Server Responsibilities

### The server **must**:

1. Host WebSocket endpoints.
2. Manage matches in memory (`matchId → Match`).
3. Maintain the authoritative `GameState` per match.
4. Translate WS protocol messages into `Action` objects.
5. Validate actions using `RuleEngine.validateAction()`.
6. Apply actions using `RuleEngine.applyAction()`.
7. Broadcast state updates to all connected clients in the match.
8. Enforce turn order and reject invalid actions.
9. Detect game over and broadcast `game_over`.
10. Ensure deterministic behavior.

### The server **must not**:

- Implement any gameplay rules (movement, attack, turn logic).
- Modify `GameState` directly.
- Modify `Unit` directly.
- Predict future actions.
- Use randomness outside the Engine.
- Use any non-deterministic behavior in processing actions.

---

## 3. High-Level Architecture

```
       WebSocket Layer
              │
              ▼
        Match Controller
              │
              ▼
        RuleEngine (Engine Layer)
              │
              ▼
     GameState (immutable)
```

The server only orchestrates and delegates;
**the Engine is the single source of game logic**.

---

## 4. Match Data Model

Each active match is represented by:

```
Match {
    matchId: String
    players: { P1: WebSocketSession?, P2: WebSocketSession? }
    state: GameState
}
```

Requirements:

- Each match supports exactly **two players**: P1 and P2.
- Players may join in any order.
- If a player reconnects, they replace their previous WebSocket session.
- `GameState` is always immutable.
- State must only change via `RuleEngine.applyAction`.

---

## 5. Match Lifecycle (V1)

### 5.1 Creation

A match is created when the first client sends:

```
type = "join_match"
payload.matchId = "abc123"
payload.playerId = "P1" or "P2"
```

If `matchId` does not exist:

- Create new `Match`
- Initialize GameState:
  - 5×5 board
  - Predefined unit placements (if needed for prototype)
  - `currentPlayer = P1`
  - `isGameOver = false`
  - `winner = null`

Server responds to client:

```
type = "match_joined"
payload.state = <GameState JSON>
```

---

### 5.2 Joining Existing Match

If `matchId` already exists:

- Add or replace the WebSocket session for that player.
- Send the current GameState via `match_joined`.

Both players can join in any order.

---

### 5.3 Action Processing Loop

When server receives:

```
{
  type: "action",
  payload: { playerId, action }
}
```

Server must perform:

1. Lookup match via matchId.
2. Ensure `playerId` matches sender.
3. Validate via `RuleEngine.validateAction`.
   - If invalid → send `validation_error`.
4. Apply via `RuleEngine.applyAction`.
5. Update match.state = newState.
6. Broadcast:
   - If not game over → `state_update`
   - If game over → `game_over`
7. Do not send incremental diffs—send **full GameState**.

This loop is deterministic:
same inputs → same outputs.

---

### 5.4 Game Over

A match is over when:

- `GameState.isGameOver == true`

Server must send:

```
type = "game_over"
payload.winner = state.winner
payload.state = <final GameState>
```

After that:

- Server rejects all further actions.
- Match remains stored until garbage collected.

---

## 6. WebSocket Message Flow (V1)

### 6.1 Client → Server

| Type          | Description |
|---------------|-------------|
| `join_match`  | Join or create match |
| `action`      | Player action request |
| `ping`        | Heartbeat |

### 6.2 Server → Client

| Type             | Description |
|------------------|-------------|
| `match_joined`   | Initial state after join |
| `state_update`   | Broadcast updated GameState |
| `validation_error` | Action rejected |
| `game_over`      | Match ended |
| `pong`           | Heartbeat reply |

Message structure must match `WS_PROTOCOL_V1.md`.

---

## 7. Action Handling Pipeline (Server-Side)

### 7.1 Overview

```
Client Action → WS Handler → Match Controller → RuleEngine.validate
                                              → RuleEngine.apply
                                              → Broadcast
```

### 7.2 Detailed Steps

#### Step 1 — Receive WS action message

- Validate JSON structure (protocol-level).
- Extract `playerId` & `action`.

#### Step 2 — Match Lookup

- If match not found → send validation_error: `"Unknown match"`.

#### Step 3 — Player Authorization

- The WebSocket session must correspond to the claimed `playerId`.

#### Step 4 — Call `validateAction()`

If invalid:

```
type = "validation_error"
payload.message = <errorMessage>
payload.action = <original action>
```

#### Step 5 — Call `applyAction()`

- ALWAYS returns a new immutable `GameState`.
- Must update match.state.

#### Step 6 — Game Over Check

If `state.isGameOver == true`:

- Immediately broadcast `game_over`.

Else:

- Broadcast `state_update`.

---

## 8. Determinism Requirements

Server must **not** introduce nondeterminism:

- No timestamps
- No randomness
- No external ordering based on thread scheduling

The order of state updates must strictly follow the order of received actions.

Clients must always see:

- identical state,
- in the same order,
- across reconnects or multiple observers.

---

## 9. Broadcasting Rules

### 9.1 Who receives updates?

All connected clients in the match (both P1 and P2).

### 9.2 What is broadcast?

Always broadcast **full GameState**, serialized via:

```
GameStateSerializer.toJsonMap()
```

Never send:

- partial diffs
- per-unit patches
- incremental updates

This keeps V1 simple, deterministic, and stateless for clients.

---

## 10. Error Handling Philosophy

### 10.1 Invalid Action → validation_error

- All invalid actions return `validation_error`.
- GameState never changes.

### 10.2 Malformed WS Message

If JSON missing required fields:

- Respond with a protocol-level error (validation_error)

### 10.3 Unknown matchId

- Respond with `"Unknown match"`

### 10.4 Unauthorized action from wrong playerId

- Respond with `"Not your turn"` or `"Incorrect player"` depending on context.

### 10.5 After Game Over

All actions return:

```
validation_error: "Game is already over"
```

---

## 11. In-Memory Storage Model

Simple in-memory dictionary:

```
Map<String, Match> matches
```

This is fully sufficient for V1 (no database, no persistence).
Future versions may add persistence once the system matures.

---

## 12. Concurrency Model (V1)

To preserve **determinism & safety**:

- Each match must be processed **sequentially**.
- One action at a time per match.
- No concurrent mutation of GameState.

Recommended approach:

- One lightweight lock per match (`synchronized` block or similar).
- WebSocket callbacks enqueue actions into per-match queue (optional).

---

## 13. Implementation Recommendations (Non-Normative)

Although not required by this document, these are recommended:

- Create `MatchService` class for managing matches.
- Create `WebSocketMatchHandler` class for WS logic.
- Encapsulate player session references inside Match object.
- Provide utility methods:
  - `broadcast(match, message)`
  - `send(playerSession, message)`

These keep the main logic easy to maintain.

---

## 14. Future Extensions (Not in V1)

Server architecture must support future:

- Multiple matches per player
- Matchmaking / lobbies
- Spectator mode
- Reconnection with state diffing
- Persistent match logs
- Replays
- Ranking & MMR
- Bot players
- Multi-unit selection
- Time-limited turns
- Multi-player matches (3+ players)

None of these are included in V1.

---

## 15. Summary

The Server layer in 5x5 Tactics V1:

- Is **authoritative**
- Delegates all game logic to **RuleEngine**
- Manages matches & connections
- Applies player actions → updates state → broadcasts updates
- Ensures deterministic results
- Maintains simple, clear boundaries between layers

This architecture is stable and extensible, minimizing future refactors.

---

# End of SERVER_TECH_ARCH_V1.md
