# SERVER_SKELETON_V1 — 5x5 Tactics Server Class Skeleton (Version 1)

## 1. Scope

This document defines the **server-side Java class skeleton** for 5x5 Tactics V1.

It specifies:

- Packages and class names
- Fields (with types)
- Constructors
- Getters
- Method signatures (no logic)

It must be consistent with:

- `TECH_ARCH.md`
- `SERVER_TECH_ARCH_V1.md`
- `WS_PROTOCOL_V1.md`
- `ENGINE_SKELETON_V1.md`

Important constraints:

- **No business logic** in this skeleton.
- No persistence, no framework vendor lock-in.
- No WebSocket framework-specific types (e.g. no direct Spring, Jetty, etc).
- Only **fields, constructors, getters, and empty method bodies**.
- All state transitions must delegate to the Engine layer (`RuleEngine`, `GameState`).

---

## 2. Package Overview

All server classes live under:

- `com.tactics.server`

Sub-packages:

- `com.tactics.server.core` — Match management, in-memory registry
- `com.tactics.server.ws` — WebSocket abstraction & message routing
- `com.tactics.server.dto` — Protocol DTOs for JSON payloads

Engine imports (from existing engine module):

- `com.tactics.engine.model.GameState`
- `com.tactics.engine.model.PlayerId`
- `com.tactics.engine.action.Action`
- `com.tactics.engine.rules.RuleEngine`
- `com.tactics.engine.util.GameStateSerializer`

---

## 3. Core Match Model (com.tactics.server.core)

### 3.1 MatchId

**Package**: `com.tactics.server.core`

**Purpose**: Strongly typed wrapper for a match identifier.

**Fields**:

- `private final String value;`

**Members**:

- Constructor: `public MatchId(String value)`
- Getter: `public String getValue()`

---

### 3.2 ClientSlot

**Package**: `com.tactics.server.core`

**Purpose**: Enumerates the two player slots in a match.

**Type**:

```java
public enum ClientSlot {
    P1,
    P2
}
```

No additional methods required.

---

### 3.3 Match

**Package**: `com.tactics.server.core`

**Purpose**: Represents a single active match and its server-side state.

**Fields**:

- `private final MatchId matchId;`
- `private final GameState state;`
- `private final java.util.Map<ClientSlot, com.tactics.server.ws.ClientConnection> connections;`

Notes:

- `state` is immutable (new `Match` created when state changes).
- `connections` maps P1/P2 to their active WebSocket connection (nullable).

**Members**:

- Constructor:

  ```java
  public Match(MatchId matchId,
               GameState state,
               java.util.Map<ClientSlot, com.tactics.server.ws.ClientConnection> connections)
  ```

- Getters:
  - `public MatchId getMatchId()`
  - `public GameState getState()`
  - `public java.util.Map<ClientSlot, com.tactics.server.ws.ClientConnection> getConnections()`

No logic (no connect/disconnect helpers) in this skeleton.

---

### 3.4 MatchRegistry

**Package**: `com.tactics.server.core`

**Purpose**: In-memory registry for all active matches.

**Fields**:

- `private final java.util.Map<String, Match> matches;`

Notes:

- Key is `matchId` as String for easier integration with protocol.

**Members**:

- Constructor:

  ```java
  public MatchRegistry(java.util.Map<String, Match> matches)
  ```

- Getters:
  - `public java.util.Map<String, Match> getMatches()`

- Methods (no logic, empty bodies for now):

  ```java
  public Match getMatch(String matchId) {
      // TODO: implement
      return null;
  }

  public Match createMatch(String matchId, GameState initialState) {
      // TODO: implement
      return null;
  }

  public void updateMatchState(String matchId, GameState newState) {
      // TODO: implement
  }

  public java.util.Collection<Match> listMatches() {
      // TODO: implement
      return java.util.Collections.emptyList();
  }
  ```

---

### 3.5 MatchService

**Package**: `com.tactics.server.core`

**Purpose**: High-level service for orchestrating match operations.

**Fields**:

- `private final MatchRegistry matchRegistry;`
- `private final RuleEngine ruleEngine;`
- `private final GameStateSerializer gameStateSerializer;`

**Members**:

- Constructor:

  ```java
  public MatchService(MatchRegistry matchRegistry,
                      RuleEngine ruleEngine,
                      GameStateSerializer gameStateSerializer)
  ```

- Getters:
  - `public MatchRegistry getMatchRegistry()`
  - `public RuleEngine getRuleEngine()`
  - `public GameStateSerializer getGameStateSerializer()`

- Methods (signatures only, no logic):

  ```java
  public Match getOrCreateMatch(String matchId) {
      // TODO: implement
      return null;
  }

  public Match findMatch(String matchId) {
      // TODO: implement
      return null;
  }

  public GameState getCurrentState(String matchId) {
      // TODO: implement
      return null;
  }

  public GameState applyAction(String matchId,
                               PlayerId playerId,
                               Action action) {
      // TODO: implement
      return null;
  }
  ```

---

## 4. WebSocket Abstraction (com.tactics.server.ws)

### 4.1 ClientConnection

**Package**: `com.tactics.server.ws`

**Purpose**: Framework-agnostic abstraction of a WebSocket connection.

**Type**:

```java
public interface ClientConnection {

    String getId();

    void sendMessage(String message);
}
```

Notes:

- `getId()` can be implementation-specific (session id, uuid, etc).
- `sendMessage` will send serialized JSON text.

---

### 4.2 ConnectionRegistry

**Package**: `com.tactics.server.ws`

**Purpose**: Tracks active connections and their mapping to matches/slots (optional, V1 minimal).

**Fields**:

- `private final java.util.Map<String, ClientConnection> connections;`

**Members**:

- Constructor:

  ```java
  public ConnectionRegistry(java.util.Map<String, ClientConnection> connections)
  ```

- Getter:
  - `public java.util.Map<String, ClientConnection> getConnections()`

- Methods (no logic):

  ```java
  public void register(ClientConnection connection) {
      // TODO: implement
  }

  public void unregister(ClientConnection connection) {
      // TODO: implement
  }

  public ClientConnection findById(String id) {
      // TODO: implement
      return null;
  }
  ```

---

### 4.3 MatchWebSocketHandler

**Package**: `com.tactics.server.ws`

**Purpose**: Main entry point for WebSocket events; routes messages to MatchService.

**Fields**:

- `private final MatchService matchService;`
- `private final ConnectionRegistry connectionRegistry;`

**Members**:

- Constructor:

  ```java
  public MatchWebSocketHandler(MatchService matchService,
                               ConnectionRegistry connectionRegistry)
  ```

- Getters:
  - `public MatchService getMatchService()`
  - `public ConnectionRegistry getConnectionRegistry()`

- Event methods (no logic, called by underlying WS framework):

  ```java
  public void onOpen(ClientConnection connection) {
      // TODO: implement
  }

  public void onClose(ClientConnection connection) {
      // TODO: implement
  }

  public void onMessage(ClientConnection connection, String text) {
      // TODO: implement
  }
  ```

---

## 5. Protocol DTOs (com.tactics.server.dto)

DTOs mirror `WS_PROTOCOL_V1` for structured message handling before/after JSON serialization.

### 5.1 IncomingMessage

**Package**: `com.tactics.server.dto`

**Purpose**: Generic wrapper for incoming WS messages.

**Fields**:

- `private final String type;`
- `private final java.util.Map<String, Object> payload;`

**Members**:

- Constructor:

  ```java
  public IncomingMessage(String type,
                         java.util.Map<String, Object> payload)
  ```

- Getters:
  - `public String getType()`
  - `public java.util.Map<String, Object> getPayload()`

---

### 5.2 JoinMatchRequest

**Package**: `com.tactics.server.dto`

**Purpose**: Structured representation of `join_match` payload.

**Fields**:

- `private final String matchId;`
- `private final String playerId;`

**Members**:

- Constructor:

  ```java
  public JoinMatchRequest(String matchId, String playerId)
  ```

- Getters:
  - `public String getMatchId()`
  - `public String getPlayerId()`

---

### 5.3 ActionPayload

**Package**: `com.tactics.server.dto`

**Purpose**: Represents the inner `"action"` object from `WS_PROTOCOL_V1`.

**Fields**:

- `private final String type;`
- `private final Integer targetX;`  // nullable
- `private final Integer targetY;`  // nullable
- `private final String targetUnitId;`  // nullable

**Members**:

- Constructor:

  ```java
  public ActionPayload(String type,
                       Integer targetX,
                       Integer targetY,
                       String targetUnitId)
  ```

- Getters:
  - `public String getType()`
  - `public Integer getTargetX()`
  - `public Integer getTargetY()`
  - `public String getTargetUnitId()`

---

### 5.4 ActionRequest

**Package**: `com.tactics.server.dto`

**Purpose**: Structured representation of `action` message payload.

**Fields**:

- `private final String matchId;`
- `private final String playerId;`
- `private final ActionPayload action;`

**Members**:

- Constructor:

  ```java
  public ActionRequest(String matchId,
                       String playerId,
                       ActionPayload action)
  ```

- Getters:
  - `public String getMatchId()`
  - `public String getPlayerId()`
  - `public ActionPayload getAction()`

---

### 5.5 OutgoingMessage

**Package**: `com.tactics.server.dto`

**Purpose**: Generic wrapper for outgoing WS messages.

**Fields**:

- `private final String type;`
- `private final Object payload;`

**Members**:

- Constructor:

  ```java
  public OutgoingMessage(String type, Object payload)
  ```

- Getters:
  - `public String getType()`
  - `public Object getPayload()`

---

### 5.6 MatchJoinedPayload

**Package**: `com.tactics.server.dto`

**Purpose**: Payload for `match_joined` messages.

**Fields**:

- `private final String matchId;`
- `private final String playerId;`
- `private final java.util.Map<String, Object> state;` // serialized GameState

**Members**:

- Constructor:

  ```java
  public MatchJoinedPayload(String matchId,
                            String playerId,
                            java.util.Map<String, Object> state)
  ```

- Getters:
  - `public String getMatchId()`
  - `public String getPlayerId()`
  - `public java.util.Map<String, Object> getState()`

---

### 5.7 StateUpdatePayload

**Package**: `com.tactics.server.dto`

**Purpose**: Payload for `state_update` messages.

**Fields**:

- `private final java.util.Map<String, Object> state;`

**Members**:

- Constructor:

  ```java
  public StateUpdatePayload(java.util.Map<String, Object> state)
  ```

- Getter:
  - `public java.util.Map<String, Object> getState()`

---

### 5.8 ValidationErrorPayload

**Package**: `com.tactics.server.dto`

**Purpose**: Payload for `validation_error` messages.

**Fields**:

- `private final String message;`
- `private final ActionPayload action;`

**Members**:

- Constructor:

  ```java
  public ValidationErrorPayload(String message,
                                ActionPayload action)
  ```

- Getters:
  - `public String getMessage()`
  - `public ActionPayload getAction()`

---

### 5.9 GameOverPayload

**Package**: `com.tactics.server.dto`

**Purpose**: Payload for `game_over` messages.

**Fields**:

- `private final String winner;`
- `private final java.util.Map<String, Object> state;`

**Members**:

- Constructor:

  ```java
  public GameOverPayload(String winner,
                         java.util.Map<String, Object> state)
  ```

- Getters:
  - `public String getWinner()`
  - `public java.util.Map<String, Object> getState()`

---

## 6. Implementation Notes

- All classes in this skeleton must be implemented as **POJOs** with:
  - Private fields
  - Constructors
  - Getters
  - No business logic
- All `// TODO: implement` method bodies should initially throw:
  - `throw new UnsupportedOperationException("Not implemented yet");`
- No WebSocket or JSON framework is specified in this skeleton.
  - Integration with a concrete framework will occur in a separate layer or module.

---

# End of SERVER_SKELETON_V1.md
