# WS_PROTOCOL_V1 — 5x5 Tactics WebSocket Protocol (Version 1, V3 Rules)

## 1. Scope

This document defines the **WebSocket communication protocol** for **5x5 Tactics**.

It covers:

- Connection flow
- Message types (including Draft phase)
- Client → Server action messages
- Server → Client state updates
- Error handling
- Serialization formats

This protocol aligns with:

- GAME_RULES_MASTER_SPEC_V3
- SKILL_SYSTEM_V3
- BUFF_SYSTEM_V3

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
| `select_team` | Submit draft selection (hero + minions) |
| `action` | Player issues an in-game action |
| `ping` | Heartbeat |

### Server → Client
| Type | Description |
|------|-------------|
| `match_joined` | Initial state after join |
| `state_update` | Full GameState after a valid action |
| `validation_error` | Action rejected |
| `game_over` | Match ended |
| `draft_ready` | Player submitted draft selection |
| `game_ready` | Game phase started |
| `your_turn` | Notification for unit's turn (with timer) |
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

### 4.2 `select_team` (Draft Phase)

Submit hero and minion selection during draft phase.

```json
{
  "type": "select_team",
  "payload": {
    "matchId": "<string>",
    "playerId": "<string>",
    "heroClass": "WARRIOR | MAGE | ROGUE | CLERIC | HUNTRESS | DUELIST",
    "minions": ["TANK", "ARCHER"]
  }
}
```

**Notes:**
- `heroClass`: One of 6 hero classes
- `minions`: Array of exactly 2 minion types (TANK, ARCHER, ASSASSIN)
- Server responds with `draft_ready` when received
- When both players submit, server sends `state_update` with phase="BATTLE"

---

### 4.3 `action`

This maps directly to Engine's `Action`.

```json
{
  "type": "action",
  "payload": {
    "matchId": "<string>",
    "playerId": "<string>",
    "action": {
      "type": "MOVE | ATTACK | MOVE_AND_ATTACK | END_TURN | USE_SKILL | DEATH_CHOICE",
      "targetX": 1,
      "targetY": 2,
      "targetUnitId": "<string or null>",
      "actingUnitId": "<string or null>",
      "skillId": "<string or null>",
      "buffType": "<string or null>"
    }
  }
}
```

### Action Type Requirements

#### MOVE
- Requires `targetX`, `targetY`
- `targetUnitId` must be null

```json
{
  "type": "action",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1",
    "action": {
      "type": "MOVE",
      "targetX": 2,
      "targetY": 3
    }
  }
}
```

#### ATTACK
- Requires `targetX`, `targetY`
- Requires `targetUnitId`

```json
{
  "type": "action",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1",
    "action": {
      "type": "ATTACK",
      "targetX": 2,
      "targetY": 3,
      "targetUnitId": "p2_hero"
    }
  }
}
```

#### MOVE_AND_ATTACK
- Requires `targetX`, `targetY` (movement destination)
- Requires `targetUnitId` (attack target)

```json
{
  "type": "action",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1",
    "action": {
      "type": "MOVE_AND_ATTACK",
      "targetX": 2,
      "targetY": 2,
      "targetUnitId": "p2_minion_1"
    }
  }
}
```

#### END_TURN
- No targets required

```json
{
  "type": "action",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1",
    "action": {
      "type": "END_TURN"
    }
  }
}
```

#### USE_SKILL
- Requires `skillId`
- Optional `targetX`, `targetY` (for targeted skills)
- Optional `targetUnitId` (for unit-targeted skills)

```json
{
  "type": "action",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1",
    "action": {
      "type": "USE_SKILL",
      "skillId": "warrior_heroic_leap",
      "targetX": 3,
      "targetY": 2
    }
  }
}
```

#### DEATH_CHOICE
- Requires `buffType` (one of 6 buff types)
- Sent when player's minion dies in combat

```json
{
  "type": "action",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1",
    "action": {
      "type": "DEATH_CHOICE",
      "buffType": "POWER | LIFE | SPEED | WEAKNESS | BLEED | SLOW"
    }
  }
}
```

**Death Choice Notes:**
- Player chooses buff type for tile spawned at death location
- 5 second timer; defaults to OBSTACLE if timeout
- Only for combat deaths (not system deaths from decay)

---

### 4.4 `ping`

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
    "winner": "P1 | P2",
    "state": { ... GameState JSON ... }
  }
}
```

---

### 5.5 `draft_ready`

Sent when a player submits their draft selection.

```json
{
  "type": "draft_ready",
  "payload": {
    "playerId": "<string>",
    "heroClass": "<string>",
    "draftComplete": true | false
  }
}
```

**Notes:**
- `draftComplete`: true when both players have submitted

---

### 5.6 `game_ready`

Sent when game transitions to a new phase.

```json
{
  "type": "game_ready",
  "payload": {
    "phase": "DRAFT | BATTLE"
  }
}
```

---

### 5.7 `your_turn`

Sent to indicate which unit should act (unit-by-unit turn system).

```json
{
  "type": "your_turn",
  "payload": {
    "unitId": "<string>",
    "actionStartTime": 1234567890,
    "timeoutMs": 10000,
    "timerType": "ACTION | DEATH_CHOICE | DRAFT"
  }
}
```

**Timer Values:**
- ACTION: 10,000ms (10 seconds)
- DEATH_CHOICE: 5,000ms (5 seconds)
- DRAFT: 60,000ms (60 seconds)

---

### 5.8 `pong`

```json
{
  "type": "pong",
  "payload": {}
}
```

---

## 6. GameState Serialization Format

The server always sends full state via `GameStateSerializer.toJsonMap(GameState)`.

Example (V3 format):

```json
{
  "board": { "width": 5, "height": 5 },
  "units": [
    {
      "id": "p1_hero",
      "owner": "P1",
      "hp": 5,
      "maxHp": 5,
      "attack": 1,
      "moveRange": 2,
      "attackRange": 1,
      "alive": true,
      "position": { "x": 2, "y": 0 },
      "category": "HERO",
      "heroClass": "WARRIOR",
      "selectedSkillId": "warrior_endure",
      "skillCooldown": 0,
      "actionsUsed": 0
    },
    {
      "id": "p1_minion_1",
      "owner": "P1",
      "hp": 3,
      "maxHp": 3,
      "attack": 1,
      "moveRange": 2,
      "attackRange": 1,
      "alive": true,
      "position": { "x": 1, "y": 0 },
      "category": "MINION",
      "minionType": "TANK"
    }
  ],
  "currentPlayer": "P1",
  "currentRound": 1,
  "phase": "BATTLE",
  "isGameOver": false,
  "winner": null,
  "obstacles": [
    { "id": "obs_1", "position": { "x": 2, "y": 2 } }
  ],
  "buffTiles": [
    { "id": "tile_1", "position": { "x": 3, "y": 3 }, "buffType": "POWER", "duration": 2, "triggered": false }
  ],
  "unitBuffs": {
    "p1_hero": [
      { "buffId": "speed_1", "type": "SPEED", "duration": 2 }
    ]
  },
  "pendingDeathChoice": null
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

### Invalid Action
Server returns:

```json
{
  "type": "validation_error",
  "payload": { "message": "Invalid action: <reason>" }
}
```

---

## 8. State Update Ordering

Server must guarantee:

1. All clients receive updates in the same order
2. Updates are authoritative
3. Always send full GameState, never diffs

---

## 9. Timer System

### Timer Types
| Type | Duration | Timeout Behavior |
|------|----------|------------------|
| DRAFT | 60s | Random selection |
| ACTION | 10s | Hero -1 HP, auto END_TURN |
| DEATH_CHOICE | 5s | Default OBSTACLE |

### Timer Flow
1. Server starts timer, sends `your_turn` with timer info
2. Client displays countdown
3. If timeout: server applies penalty and advances game
4. Server sends `state_update` with new state

---

## 10. Game Phases

### DRAFT Phase
1. Both players receive `match_joined`
2. Players send `select_team` messages
3. Server sends `draft_ready` on each submission
4. When both ready: server sends `state_update` with phase="BATTLE"

### BATTLE Phase
1. Server sends `your_turn` for active unit
2. Player sends `action`
3. Server validates, applies, sends `state_update`
4. On death: `pendingDeathChoice` set, timer starts
5. Continue until hero dies → `game_over`

---

# End of WS_PROTOCOL_V1.md
