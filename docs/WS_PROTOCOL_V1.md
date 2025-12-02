# WS_PROTOCOL_V1 — 5x5 Tactics WebSocket Protocol (Version 1, Updated)

## 1. Scope

This document defines the **WebSocket communication protocol** for **5x5 Tactics (V1)**.

It covers:

- Connection flow
- Message types
- Client → Server action messages
- Server → Client state updates
- Error handling
- Serialization formats

This protocol must not contradict:

- GAME_RULES_V1
- ENGINE_SKELETON_V1
- TECH_ARCH

---

## 2. General WebSocket Rules

- Communication is **JSON-only**
- All messages have `"type"` + `"payload"`
- Server is authoritative
- Client must never simulate GameState

Base structure:

```json
{
  "type": "<MessageType>",
  "payload": { ... }
}
```

---

## 3. Message Types (Summary)

### Client → Server
| Type | Description |
|------|-------------|
| `join_match` | Join or create a match |
| `action` | Player issues an in-game action |
| `ping` | Heartbeat |

### Server → Client
| Type | Description |
|------|-------------|
| `match_joined` | Initial state after join |
| `state_update` | Full GameState after a valid action |
| `validation_error` | Action rejected |
| `game_over` | Match ended |
| `pong` | Heartbeat response |

---

## 4. Client → Server Messages

---

### 4.1 `join_match`

```json
{
  "type": "join_match",
  "payload": {
    "matchId": "<string>",
    "playerId": "<string>"
  }
}
```

---

### 4.2 `action` (Updated Target Rules)

This maps directly to Engine's `Action`.

```json
{
  "type": "action",
  "payload": {
    "playerId": "<string>",
    "action": {
      "type": "MOVE | ATTACK | MOVE_AND_ATTACK | END_TURN",
      "targetPosition": { "x": 1, "y": 2 },     // Required for MOVE, MOVE_AND_ATTACK, ATTACK
      "targetUnitId": "<string or null>"        // Required for ATTACK, MOVE_AND_ATTACK
    }
  }
}
```

### Required Rules

#### MOVE
- Requires `targetPosition`
- `targetUnitId` must be null

#### ATTACK
- Requires `targetPosition`
- Requires `targetUnitId`

#### MOVE_AND_ATTACK
- Requires `targetPosition` (movement)
- Requires `targetUnitId` (attack target)

#### END_TURN
- No targets required

---

### 4.3 `ping`

```json
{
  "type": "ping",
  "payload": {}
}
```

Server responds with `pong`.

---

## 5. Server → Client Messages

---

### 5.1 `match_joined`

```json
{
  "type": "match_joined",
  "payload": {
    "matchId": "<string>",
    "playerId": "<string>",
    "state": { ... GameState JSON ... }
  }
}
```

---

### 5.2 `state_update`

```json
{
  "type": "state_update",
  "payload": {
    "state": { ... GameState JSON ... }
  }
}
```

---

### 5.3 `validation_error`

```json
{
  "type": "validation_error",
  "payload": {
    "message": "<string>",
    "action": { ... Original Action JSON ... }
  }
}
```

---

### 5.4 `game_over`

```json
{
  "type": "game_over",
  "payload": {
    "winner": "<string>",
    "state": { ... GameState JSON ... }
  }
}
```

---

### 5.5 `pong`

```json
{
  "type": "pong",
  "payload": {}
}
```

---

## 6. GameState Serialization Format

The server always sends full state:

```
GameStateSerializer.toJsonMap(GameState)
```

Example:

```json
{
  "board": { "width": 5, "height": 5 },
  "units": [
    {
      "id": "u1",
      "owner": "P1",
      "hp": 10,
      "attack": 3,
      "alive": true,
      "position": { "x": 1, "y": 2 }
    }
  ],
  "currentPlayer": "P1",
  "isGameOver": false,
  "winner": null
}
```

---

## 7. Error Handling

### Validation Errors
- Sent via `validation_error`
- GameState is NOT modified

### Out-of-Turn Actions
Server returns:

```json
{
  "type": "validation_error",
  "payload": { "message": "Not your turn" }
}
```

---

## 8. State Update Ordering

Server must guarantee:

1. All clients receive updates in the same order
2. Updates are authoritative
3. Always send full GameState, never diffs

---

## 9. Future Extensions

V2 may include:

- Partial updates
- Action IDs for replay
- Player chat
- Timers
- Spectator mode

---

# End of WS_PROTOCOL_V1.md
