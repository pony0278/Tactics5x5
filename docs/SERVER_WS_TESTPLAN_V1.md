# SERVER_WS_TESTPLAN_V1 — 5x5 Tactics WebSocket Layer Test Plan

## 1. Scope

This document defines the **test plan for the WebSocket layer** in 5x5 Tactics V1.

Covered classes:

- `com.tactics.server.ws.MatchWebSocketHandler`
- `com.tactics.server.ws.ConnectionRegistry`
- Use of DTOs in `com.tactics.server.dto.*`

Focus:

- Handling WebSocket events (`onOpen`, `onClose`, `onMessage`)
- Mapping JSON text to DTOs and back (at least structurally)
- Integration with `MatchService` (mocked/fake)
- Broadcasting semantics to players in a match
- Error handling and message types (`validation_error`, `game_over`, etc.)

Not covered:

- Engine logic (RuleEngine)
- Server core details (MatchRegistry, MatchService behavior) — those are tested in `SERVER_CORE_TESTPLAN_V1`
- Any real network / actual WebSocket server framework

Tests will use **fake/mock implementations** of:

- `ClientConnection`
- `MatchService`
- JSON parsing/serialization (if needed, can be simplified or mocked)

---

## 2. Conventions

### 2.1 Test Doubles

- **FakeClientConnection**:
  - Implements `ClientConnection`
  - Captures all outgoing messages in a list of `String` (JSON/serialized form).

- **FakeMatchService**:
  - Provides deterministic responses for:
    - `getOrCreateMatch`
    - `applyAction`
    - `getCurrentState`
  - Does not require real Engine or GameState logic; we only need to know:
    - When it is called
    - With what parameters
    - Whether it returns a specific dummy state or throws `IllegalArgumentException`.

### 2.2 Messages

- Incoming messages passed to `onMessage` will be JSON-like strings.
  - For test purposes, you may:
    - Use a real JSON library, **or**
    - Use a simple fake parser that recognizes a small subset of JSON, **or**
    - Provide DTOs and bypass full JSON parsing by calling internal helpers (if designed that way).

- Outgoing messages will be captured as serialized strings.
  - Tests assert:
    - The message type (e.g. `"match_joined"`, `"state_update"`, `"validation_error"`, `"game_over"`)
    - Key payload fields (e.g. `matchId`, `playerId`, etc.)

---

## 3. ConnectionRegistry Tests (CR-Series)

### CR1 — register Adds Connection

- **Given**:
  - An empty `ConnectionRegistry`
  - A FakeClientConnection `conn` with id `"c1"`
- **When**:
  - `register(conn)`
- **Then**:
  - `findById("c1")` returns `conn`

---

### CR2 — unregister Removes Connection

- **Given**:
  - `ConnectionRegistry` with `conn` registered under `"c1"`
- **When**:
  - `unregister(conn)`
- **Then**:
  - `findById("c1")` returns `null` (or equivalent "not found" behavior)

---

### CR3 — findById Returns Null for Unknown Id

- **Given**:
  - No connection registered for `"unknown"`
- **When**:
  - `findById("unknown")`
- **Then**:
  - Returns `null`

---

## 4. MatchWebSocketHandler: Connection Events (WS-CONN-Series)

### WS-CONN1 — onOpen Registers Connection

- **Given**:
  - A new `MatchWebSocketHandler` with empty `ConnectionRegistry`
  - A FakeClientConnection `conn` with id `"c1"`
- **When**:
  - `onOpen(conn)`
- **Then**:
  - `ConnectionRegistry.findById("c1")` returns `conn`

---

### WS-CONN2 — onClose Unregisters Connection

- **Given**:
  - `onOpen(conn)` has been previously called
- **When**:
  - `onClose(conn)`
- **Then**:
  - `ConnectionRegistry.findById("c1")` returns `null`

---

## 5. MatchWebSocketHandler: join_match Flow (WS-JOIN-Series)

These tests focus on handling:

```json
{
  "type": "join_match",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1"
  }
}
```

### WS-JOIN1 — join_match Creates or Fetches Match and Responds with match_joined

- **Given**:
  - FakeMatchService where:
    - `getOrCreateMatch("match-1")` returns a Match with:
      - `matchId = "match-1"`
      - `state` = dummy GameState
  - A FakeClientConnection `conn` representing P1
- **When**:
  - `onMessage(conn, <join_match JSON for match-1, P1>)`
- **Then**:
  - FakeMatchService receives a call to `getOrCreateMatch("match-1")`
  - `conn` receives exactly one outbound message
  - Outgoing message (after parsing) has:
    - `type = "match_joined"`
    - `payload.matchId = "match-1"`
    - `payload.playerId = "P1"`
    - `payload.state` is a non-null map representing GameState

---

### WS-JOIN2 — join_match with Different PlayerId (P2) Still Returns match_joined

- **Given**:
  - FakeMatchService configured similarly as WS-JOIN1
  - A FakeClientConnection `connP2` representing P2
- **When**:
  - `onMessage(connP2, <join_match JSON for match-1, P2>)`
- **Then**:
  - `getOrCreateMatch("match-1")` is called again (idempotent behavior allowed)
  - Response to P2:
    - `type = "match_joined"`
    - `payload.matchId = "match-1"`
    - `payload.playerId = "P2"`
    - `payload.state` present

---

### WS-JOIN3 — Malformed join_match Payload → validation_error

- **Given**:
  - A join_match message missing `matchId` or `playerId`
- **When**:
  - `onMessage(conn, <malformed join_match JSON>)`
- **Then**:
  - FakeMatchService is **not** called
  - `conn` receives a `validation_error` message:
    - `type = "validation_error"`
    - `payload.message` indicates malformed or missing fields

---

## 6. MatchWebSocketHandler: action Flow (WS-ACT-Series)

These tests cover:

```json
{
  "type": "action",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1",
    "action": {
      "type": "MOVE",
      "targetX": 1,
      "targetY": 2,
      "targetUnitId": null
    }
  }
}
```

### WS-ACT1 — Valid Action Produces state_update Broadcast

- **Given**:
  - FakeMatchService where:
    - `applyAction("match-1", PlayerId("P1"), action)` returns a new GameState
    - New GameState has `isGameOver = false`
  - Match `"match-1"` has two connected clients: P1 (`connP1`), P2 (`connP2`)
    - Registered in ConnectionRegistry and attached to the Match (implementation-dependent)
- **When**:
  - `onMessage(connP1, <valid action JSON for match-1, P1>)`
- **Then**:
  - FakeMatchService receives:
    - `applyAction("match-1", PlayerId("P1"), <Action>)`
  - Both `connP1` and `connP2` receive one outbound message each:
    - `type = "state_update"`
    - `payload.state` contains serialized GameState

---

### WS-ACT2 — Valid Action That Ends Game Produces game_over Broadcast

- **Given**:
  - FakeMatchService `applyAction` returns a GameState where:
    - `isGameOver = true`
    - `winner = PlayerId("P1")`
- **When**:
  - `onMessage(connP1, <valid action JSON>)`
- **Then**:
  - All clients in match receive one outbound message:
    - `type = "game_over"`
    - `payload.winner = "P1"`
    - `payload.state` contains final GameState

---

### WS-ACT3 — Invalid Action → validation_error to Sender Only

- **Given**:
  - FakeMatchService configured so that:
    - `applyAction("match-1", PlayerId("P1"), action)` throws `IllegalArgumentException("Not your turn")`
- **When**:
  - `onMessage(connP1, <invalid action JSON>)`
- **Then**:
  - `connP1` receives:
    - `type = "validation_error"`
    - `payload.message` contains `"Not your turn"` (or appropriate text)
  - No `state_update` or `game_over` is broadcast to any client

---

### WS-ACT4 — Action for Unknown Match → validation_error

- **Given**:
  - FakeMatchService throws `IllegalArgumentException("Unknown match")` when `applyAction` is called with an unknown `matchId`
- **When**:
  - `onMessage(connP1, <action JSON with matchId = "unknown-match">)`
- **Then**:
  - `connP1` receives:
    - `type = "validation_error"`
    - `payload.message` contains `"Unknown match"`

---

### WS-ACT5 — Malformed Action Payload → validation_error

- **Given**:
  - An `action` message missing required fields (e.g., no `type` in action payload)
- **When**:
  - `onMessage(connP1, <malformed action JSON>)`
- **Then**:
  - FakeMatchService is not called
  - `connP1` receives:
    - `type = "validation_error"`
    - `payload.message` indicates malformed or missing fields

---

## 7. MatchWebSocketHandler: Generic Message Handling (WS-GEN-Series)

### WS-GEN1 — Unknown Message Type → validation_error or Ignored

- **Given**:
  - Incoming message with `type = "unknown_type"`
- **When**:
  - `onMessage(conn, <unknown type JSON>)`
- **Then**:
  - Implementation choice:
    - Either send back a `validation_error` indicating unknown type, **or**
    - Ignore silently
  - Test asserts whichever behavior is implemented and locks it in.

---

### WS-GEN2 — JSON Parsing Failure → validation_error

- **Given**:
  - Text that is not valid JSON at all (e.g., `"this is not json"`)
- **When**:
  - `onMessage(conn, "this is not json")`
- **Then**:
  - `conn` receives a `validation_error`:
    - `payload.message` indicates JSON parse error

---

## 8. Broadcasting Semantics (WS-BCAST-Series)

These tests focus on correct broadcast behavior across multiple connections.

### WS-BCAST1 — state_update Sent to All Connected Clients in Match

- **Given**:
  - Two FakeClientConnections registered for the same match: `connP1`, `connP2`
  - FakeMatchService `applyAction` returns a non-game-over state
- **When**:
  - P1 sends a valid action via `onMessage(connP1, ...)`
- **Then**:
  - Both `connP1` and `connP2` receive a `state_update` message
  - No extra/duplicate messages are sent

---

### WS-BCAST2 — game_over Sent to All Connected Clients

- **Given**:
  - Similar to WS-BCAST1, but `isGameOver = true` in result
- **When**:
  - P1 sends a valid action that ends the game
- **Then**:
  - Both P1 and P2 connections receive a single `game_over` message

---

### WS-BCAST3 — validation_error Only Sent to Initiator

- **Given**:
  - Two connections in match
  - FakeMatchService throws `IllegalArgumentException("Not your turn")`
- **When**:
  - P2 sends action when it is P1's turn
- **Then**:
  - Only P2 (the sender) receives `validation_error`
  - P1 receives nothing

---

## 9. Immutability & Separation of Concerns (WS-SEP-Series)

### WS-SEP1 — Handler Does Not Modify GameState Directly

- **Given**:
  - FakeMatchService returns a dummy new GameState
- **When**:
  - `onMessage` processes an `action`
- **Then**:
  - Verify via FakeMatchService that:
    - All state changes flowed through `MatchService.applyAction`
    - Handler does not attempt to inspect or mutate internal GameState fields directly
  - This can be asserted by:
    - Using a spy or a fake with counters / flags
    - Ensuring no extra methods beyond `applyAction` are called on `MatchService`

---

### WS-SEP2 — Handler Does Not Implement Game Rules

- This test is conceptual rather than code-enforced:
  - Code review should confirm that `MatchWebSocketHandler`:
    - Does not compute movement ranges
    - Does not check turn order
    - Does not calculate damage
  - Only delegates to `MatchService` and maps inputs/outputs.

---

## 10. Suggested Test Class Layout

Recommended JUnit 5 test class:

- `MatchWebSocketHandlerTest`
  - Uses nested classes:

    - `@Nested class ConnectionRegistryTests` (CR-Series)
    - `@Nested class JoinMatchTests` (WS-JOIN-Series)
    - `@Nested class ActionTests` (WS-ACT & WS-BCAST-Series)
    - `@Nested class GenericMessageTests` (WS-GEN-Series)
    - `@Nested class SeparationTests` (WS-SEP-Series)

- Use `@DisplayName` with IDs, e.g.:
  - `"WS-JOIN1 - join_match returns match_joined for P1"`
  - `"WS-ACT3 - invalid action returns validation_error"`

---

# End of SERVER_WS_TESTPLAN_V1.md
