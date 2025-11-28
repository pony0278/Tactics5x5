# WS_PROTOCOL_V1 — 5x5 Tactics WebSocket Protocol (Version 1)

## 1. Scope

This document defines the **WebSocket communication protocol** for **5x5 Tactics (V1)**.

It covers:

- Connection flow  
- Message types  
- Client → Server action messages  
- Server → Client state updates  
- Error handling  
- Serialization formats  

This protocol must be followed by:

- WebSocket server implementation  
- Clients (UI/Front-end)  
- Any debugging or automation tools  

This protocol must **not** contradict:

- Game rules (GAME_RULES_V1)  
- Engine structure (ENGINE_SKELETON_V1)  
- Architecture rules (TECH_ARCH)  

---

## 2. General WebSocket Rules

- Communication is **JSON-only**.  
- All messages contain a field: `"type"`  
- Server is **authoritative**; the client **never simulates GameState**.  
- All state changes come from the server.

All messages follow this base structure:

```json
{
  "type": "<MessageType>",
  "payload": { ... }
}
```

---

## 3. Message Types (Summary)

### Client → Server
| Message Type            | Description |
|------------------------|-------------|
| `join_match`           | Join or create a match |
| `action`               | Player requests an in-game action |
| `ping`                 | Client heartbeat (optional) |

### Server → Client
| Message Type             | Description |
|-------------------------|-------------|
| `match_joined`          | Confirmation and initial state |
| `state_update`          | Full GameState after any applied action |
| `validation_error`      | Action rejected |
| `game_over`             | Match ended |
| `pong`                  | Server heartbeat response |

---

## 4. Client → Server Messages

---

### 4.1 `join_match`

Sent when client connects or wants to enter a match.

```json
{
  "type": "join_match",
  "payload": {
    "matchId": "<string>",
    "playerId": "<string>"
  }
}
```

#### Rules
- `playerId` must match the player's assigned identity.  
- Server responds with `match_joined`.

---

### 4.2 `action`

Client requests an in-game action.  
This maps **1:1** to the Engine's `Action` structure.

```json
{
  "type": "action",
  "payload": {
    "playerId": "<string>",
    "action": {
      "type": "MOVE | ATTACK | MOVE_AND_ATTACK | END_TURN",
      "targetPosition": { "x": 1, "y": 2 },     // nullable
      "targetUnitId": "<string or null>"        // nullable
    }
  }
}
```

#### Rules
- `playerId` must match the user sending the action.
- `targetPosition` must be included for MOVE / MOVE_AND_ATTACK.
- `targetUnitId` must be included for ATTACK.
- Server will:
  - Validate the action via `RuleEngine.validateAction`
  - Apply action if valid  
  - Respond with either `state_update` or `validation_error`

---

### 4.3 `ping` (optional)

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

Sent once after the client joins a match.

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

Sent when the state changes due to a valid action.

```json
{
  "type": "state_update",
  "payload": {
    "state": { ... GameState JSON ... }
  }
}
```

GameState is encoded via:

```
GameStateSerializer.toJsonMap(GameState)
```

This is the **only** source of truth for clients.

---

### 5.3 `validation_error`

Sent when an invalid action was attempted.

```json
{
  "type": "validation_error",
  "payload": {
    "message": "<string>",
    "action": {
      "type": "...",
      "targetPosition": { "x": 0, "y": 0 },
      "targetUnitId": "unit_01"
    }
  }
}
```

#### Notes
- The client should display the message but not modify GameState.  
- Errors originate from `ValidationResult`.

---

### 5.4 `game_over`

Sent when a winner is determined.

```json
{
  "type": "game_over",
  "payload": {
    "winner": "<string>",
    "state": { ... GameState JSON ... }
  }
}
```

Clients should:

- Display winner  
- Stop sending actions  

---

### 5.5 `pong`

```json
{
  "type": "pong",
  "payload": {}
}
```

Used for heartbeat maintenance.

---

## 6. GameState Serialization Format

GameState is always sent as:

```
GameStateSerializer.toJsonMap(GameState)
```

Required JSON fields:

```json
{
  "board": {
    "width": 5,
    "height": 5
  },
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

Must match ENGINE_SKELETON_V1 exactly.

---

## 7. Error Handling & Enforcement

### 7.1 Validation Errors
- Always sent as `validation_error`  
- Server does not modify GameState  

### 7.2 Illegal Messages
If the client sends malformed JSON or unknown fields:

- Server may ignore  
- Or respond with `validation_error`  

### 7.3 Out-of-Turn Actions
Must return:

```json
{
  "type": "validation_error",
  "payload": { "message": "Not your turn" }
}
```

---

## 8. State Update Ordering Guarantees

Server must ensure:

1. All clients receive state updates in **the same order**.  
2. State updates are **authoritative**.  
3. No incremental patches; always send the full GameState.  

---

## 9. Notes for Future Protocol Versions

Future versions may include:

- Partial diff updates  
- Player chat  
- Spectator mode  
- Match metadata  
- Turn timers  
- Action IDs for replay synchronization  

V1 intentionally keeps everything **minimal and robust**.

---

# End of WS_PROTOCOL_V1.md
