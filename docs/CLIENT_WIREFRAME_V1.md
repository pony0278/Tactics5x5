# CLIENT_WIREFRAME_V1 — 5x5 Tactics Minimal Web Client Specification (Version 1)

## 1. Scope

This document defines the **minimal browser client** for 5x5 Tactics V1.

It specifies:

- UI layout and components
- Client-side state model
- WebSocket connection behavior
- User interaction flow (select unit, move, attack, end turn)
- Mapping between UI actions and WS_PROTOCOL_V1 messages
- Handling of server messages (`match_joined`, `state_update`, `validation_error`, `game_over`)

The client is intentionally minimal:

- Single-page HTML
- Vanilla JavaScript (no framework required)
- Simple CSS grid for the 5×5 board
- No login, no lobby, no matchmaking

It must be consistent with:

- `TECH_ARCH.md`
- `GAME_RULES_V1.md`
- `WS_PROTOCOL_V1.md`
- `SERVER_TECH_ARCH_V1.md`

---

## 2. Goals

The client should:

1. Connect to a running server via WebSocket.
2. Join a specified match as P1 or P2.
3. Render the 5×5 board and all units from `GameState`.
4. Allow the local player to issue:
   - MOVE
   - ATTACK
   - MOVE_AND_ATTACK
   - END_TURN
5. Visualize:
   - Current player turn
   - Unit HP and ownership
   - Game-over state (winner)
   - Validation errors (e.g. "Not your turn", "Invalid move")

The client must **not**:

- Contain any game rules (no local move validation).
- Decide legal moves or damage.
- Predict future state (no client-side simulation).

All gameplay authority resides on the server.

---

## 3. High-Level Architecture

### 3.1 Components

Minimal components:

- `WebSocketManager`
  - Opens/closes WS connection
  - Sends messages to server
  - Dispatches incoming messages to handlers

- `ClientState`
  - Holds current local view of:
    - `matchId`
    - `playerId`
    - `connected` status
    - `gameState` (from server)
    - `selectedUnitId` (for issuing actions)
    - `pendingActionType` (MOVE / ATTACK / MOVE_AND_ATTACK)
    - `lastErrorMessage`

- `BoardView`
  - Renders 5×5 grid
  - Renders units inside cells
  - Handles click events on cells

- `ControlsView`
  - Shows:
    - Current player info (whose turn)
    - Local player id
    - Buttons: MOVE, ATTACK, MOVE+ATTACK, END TURN
    - Error and status messages

### 3.2 Data Flow

1. Page loads → JavaScript creates `WebSocketManager` → connects to server.
2. On connect:
   - Client sends `join_match` with:
     - `matchId` (hard-coded or URL param)
     - `playerId` (hard-coded P1/P2 toggle for V1)
3. Server responds with `match_joined` containing initial `GameState`.
4. Client stores `gameState` in `ClientState` and re-renders board + UI.
5. User interacts (clicks units/cells/buttons) → client builds an `action` message → sends via WebSocket.
6. Server responds with:
   - `state_update` (ongoing game), or
   - `game_over` (final state), or
   - `validation_error` (invalid action).
7. Client updates `ClientState` and UI accordingly.

---

## 4. UI Layout

### 4.1 Overall Layout

Single-page layout:

- Top bar:
  - Game title: "5x5 Tactics (Prototype)"
  - Connection status: "Connected / Disconnected"
- Left side:
  - 5×5 board
- Right side:
  - Player info
  - Action controls
  - Log/error panel

### 4.2 Board Layout

- Use a 5×5 CSS grid:
  - Container: `display: grid; grid-template-columns: repeat(5, 1fr);`
  - Each cell is a clickable `<div>` with:
    - Data attributes: `data-x`, `data-y`
    - Visual state:
      - Empty tile background
      - If a unit is present: show owner + HP
      - If selected: highlight
      - If it's the local player's unit: subtle tint

Example markup:

```html
<div id="board">
  <div class="cell" data-x="0" data-y="0"></div>
  ...
  <div class="cell" data-x="4" data-y="4"></div>
</div>
```

### 4.3 Unit Rendering

Inside each `cell`:

- If a unit exists at `(x,y)`:
  - Show:
    - Owner: "P1" or "P2"
    - HP: numeric value
  - Example: `"P1 (10)"`

Color hints (minimal, optional):

- P1 units: one color (e.g., blue border)
- P2 units: another color (e.g., red border)
- Selected unit: highlighted border or background

---

## 5. Client State Model

`ClientState` fields (in JS object form):

```js
const clientState = {
  matchId: "match-1",
  playerId: "P1", // or "P2"
  connected: false,
  gameState: null, // last GameState from server
  selectedUnitId: null, // string or null
  pendingActionType: null, // "MOVE", "ATTACK", "MOVE_AND_ATTACK"
  lastErrorMessage: null, // string or null
};
```

### 5.1 GameState Representation

The client receives a JSON-friendly map version of `GameState`. For V1, assume:

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

Exact field names must match `GameStateSerializer.toJsonMap()`.

The client must not modify this object; it should treat `gameState` as read-only and re-render from it.

---

## 6. WebSocket Integration

### 6.1 Connection Flow

1. On page load:
   - Create new `WebSocket` with URL configured (e.g. `ws://localhost:8080/ws`).
2. On `open`:
   - Set `clientState.connected = true`.
   - Send initial `join_match` message:

```json
{
  "type": "join_match",
  "payload": {
    "matchId": "match-1",
    "playerId": "P1"
  }
}
```

3. On `close`:
   - Set `clientState.connected = false`.
   - Show "Disconnected" in UI.

4. On `message`:
   - Parse JSON.
   - Route based on `type`:
     - `match_joined`
     - `state_update`
     - `validation_error`
     - `game_over`

### 6.2 Outgoing Messages

#### 6.2.1 join_match

Sent once on connect:

```json
{
  "type": "join_match",
  "payload": {
    "matchId": "<string>",
    "playerId": "<P1|P2>"
  }
}
```

#### 6.2.2 action

Generic shape:

```json
{
  "type": "action",
  "payload": {
    "matchId": "<string>",
    "playerId": "<P1|P2>",
    "action": {
      "type": "MOVE" | "ATTACK" | "MOVE_AND_ATTACK" | "END_TURN",
      "targetX": 1,
      "targetY": 2,
      "targetUnitId": "someUnitId"
    }
  }
}
```

Field rules:

- MOVE:
  - `type = "MOVE"`
  - `targetX`, `targetY` required
  - `targetUnitId = null`
- ATTACK:
  - `type = "ATTACK"`
  - `targetX`, `targetY`, `targetUnitId` all required
- MOVE_AND_ATTACK:
  - `type = "MOVE_AND_ATTACK"`
  - `targetX`, `targetY`, `targetUnitId` all required
- END_TURN:
  - `type = "END_TURN"`
  - `targetX`, `targetY`, `targetUnitId` all **null**

The client must always send `matchId` and `playerId` from `ClientState`.

---

## 7. Server Message Handling

### 7.1 match_joined

Shape:

```json
{
  "type": "match_joined",
  "payload": {
    "matchId": "<string>",
    "playerId": "<P1|P2>",
    "state": { ... GameState map ... }
  }
}
```

Client behavior:

- Store `payload.state` in `clientState.gameState`.
- Ensure `clientState.matchId` and `clientState.playerId` are set.
- Clear:
  - `selectedUnitId`
  - `pendingActionType`
  - `lastErrorMessage`
- Re-render board and controls.

---

### 7.2 state_update

Shape:

```json
{
  "type": "state_update",
  "payload": {
    "state": { ... GameState map ... }
  }
}
```

Client behavior:

- Replace `clientState.gameState`.
- Clear:
  - `pendingActionType`
  - `lastErrorMessage`
- Keep `selectedUnitId` (optional decision; may keep selection if unit still alive).
- Re-render board and controls.

---

### 7.3 validation_error

Shape:

```json
{
  "type": "validation_error",
  "payload": {
    "message": "<string>",
    "action": { ... original ActionPayload ... }
  }
}
```

Client behavior:

- Set `clientState.lastErrorMessage = payload.message`.
- Optionally log in a simple `<div id="error">`.
- Do not modify `gameState`.
- Do not auto-clear selection or pendingActionType (optional; decision can be defined in implementation).

---

### 7.4 game_over

Shape:

```json
{
  "type": "game_over",
  "payload": {
    "winner": "<P1|P2|null>",
    "state": { ... final GameState ... }
  }
}
```

Client behavior:

- Set `clientState.gameState` to final state.
- Show "Game Over" banner.
- Show winner:
  - If `winner` is non-null, display `"Winner: P1"`, etc.
  - If null, display `"Game Over (no winner)"`.
- Disable further input (e.g., disable action buttons).
- Leave board visible.

---

## 8. User Interaction Flow

### 8.1 Selecting a Unit

Rules:

- The client treats all units as clickable.
- When user clicks a cell with a unit:
  - If unit belongs to local `playerId`:
    - Set `clientState.selectedUnitId = unit.id`.
    - Highlight that cell.
  - If unit belongs to opponent:
    - For ATTACK / MOVE_AND_ATTACK, this may become the `targetUnitId`.
    - For MOVE, clicking enemy units does nothing.

No rules are checked client-side (e.g., range, adjacency).
Illegal actions are handled server-side via `validation_error`.

---

### 8.2 Selecting an Action Type

Client provides buttons:

- "Move"
- "Attack"
- "Move + Attack"
- "End Turn"

Behavior:

- Clicking "Move":
  - `clientState.pendingActionType = "MOVE"`
- Clicking "Attack":
  - `clientState.pendingActionType = "ATTACK"`
- Clicking "Move + Attack":
  - `clientState.pendingActionType = "MOVE_AND_ATTACK"`
- Clicking "End Turn":
  - Immediately send an `END_TURN` action:

    ```json
    {
      "type": "action",
      "payload": {
        "matchId": ...,
        "playerId": ...,
        "action": {
          "type": "END_TURN",
          "targetX": null,
          "targetY": null,
          "targetUnitId": null
        }
      }
    }
    ```

  - After sending, clear `pendingActionType`.

The client does not check if it is actually this player's turn; server will enforce turn order.

---

### 8.3 Executing MOVE

Flow:

1. User clicks a friendly unit → `selectedUnitId = unit.id`.
2. User clicks "Move" → `pendingActionType = "MOVE"`.
3. User clicks an empty target cell `(tx, ty)`.
4. Client sends:

```json
{
  "type": "action",
  "payload": {
    "matchId": "<match-1>",
    "playerId": "<P1|P2>",
    "action": {
      "type": "MOVE",
      "targetX": tx,
      "targetY": ty,
      "targetUnitId": null
    }
  }
}
```

5. After sending:
   - Clear `pendingActionType`.
   - Optionally keep `selectedUnitId` or clear (implementation choice).

Any invalid move will return `validation_error`.

---

### 8.4 Executing ATTACK

Flow:

1. User selects a friendly unit (attacker) explicitly, or the client infers attacker only via server logic.
   - Minimal client: only select the **target** (enemy).
2. User clicks "Attack" → `pendingActionType = "ATTACK"`.
3. User clicks enemy unit at `(tx, ty)` with id `targetUnitId`.
4. Client sends:

```json
{
  "type": "action",
  "payload": {
    "matchId": "<match-1>",
    "playerId": "<P1|P2>",
    "action": {
      "type": "ATTACK",
      "targetX": tx,
      "targetY": ty,
      "targetUnitId": "<targetUnitId>"
    }
  }
}
```

5. Server infers attacker according to `GAME_RULES_V1` (adjacent friendly).

---

### 8.5 Executing MOVE_AND_ATTACK

Flow:

1. User selects "Move + Attack" → `pendingActionType = "MOVE_AND_ATTACK"`.
2. User clicks target cell `(tx, ty)` where they intend to move.
3. User clicks an enemy unit adjacent to the new position, or the client uses a 2-step UI (V1 can simplify).
4. Minimal V1 approach:

   - UI can require:
     - First click: target position (cell).
     - Second click: target enemy unit id.
   - Or simply:
     - One click on enemy unit cell, using its current position as `(targetX, targetY)` and `targetUnitId`.

Given WS_PROTOCOL_V1 for MOVE_AND_ATTACK, the client sends:

```json
{
  "type": "action",
  "payload": {
    "matchId": "<match-1>",
    "playerId": "<P1|P2>",
    "action": {
      "type": "MOVE_AND_ATTACK",
      "targetX": tx,
      "targetY": ty,
      "targetUnitId": "<targetUnitId>"
    }
  }
}
```

The exact UI flow for MOVE_AND_ATTACK in V1 can be kept minimal and refined later, as long as the payload conforms to WS_PROTOCOL_V1.

---

## 9. Error and Status Display

The client must display:

- Connection status:
  - "Connecting..."
  - "Connected"
  - "Disconnected"
- Current player:
  - `gameState.currentPlayer`
- Local player:
  - `clientState.playerId`
- Last error message:
  - `clientState.lastErrorMessage` (from `validation_error`)
- Game over message:
  - "Game Over: Winner P1" or similar.

UI elements:

- `<div id="status">` — connection, match, turn info
- `<div id="error">` — validation error text
- `<div id="log">` — optional action log

---

## 10. Future Extensions (Not in V1)

The client architecture should allow later additions:

- Click-to-highlight possible moves (client hints only)
- Animation for moves/attacks
- Per-unit panel with full stats
- Chat panel
- Multiple matches (select matchId)
- Player name instead of "P1"/"P2"
- Spectator mode (read-only)

None of these are required in V1.

---

## 11. Summary

The `CLIENT_WIREFRAME_V1` defines a **minimal, deterministic, server-authoritative** browser client:

- Renders a 5×5 board and units from `GameState`.
- Connects via WebSocket and uses `WS_PROTOCOL_V1`.
- Sends only intent (`action` messages); all rules are on the server.
- Reacts to `match_joined`, `state_update`, `validation_error`, and `game_over`.

The next step is to implement:

- `index.html`
- `client.js` (or similar)

following this specification.

---

# End of CLIENT_WIREFRAME_V1.md
