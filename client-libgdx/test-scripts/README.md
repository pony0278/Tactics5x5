# WebSocket Game Test Scripts

Automated test scripts that simulate two players completing a full game via WebSocket.

## Files

- `WebSocketGameTest.java` - Java test (recommended, no dependencies)
- `websocket_game_test.py` - Python test (requires `websockets` package)

## Running the Java Test

### 1. Start the game server (in project root):

```bash
cd /mnt/d/blueStack/Tactics5x5
mvn exec:java
```

### 2. Run the test:

```bash
cd client-libgdx/test-scripts
java WebSocketGameTest.java
```

### Expected Output

```
=== Tactics 5x5 WebSocket Test ===
Match ID: test-1765275298297

[P1] Connected
[P1] Sent: join_match (matchId=test-1765275298297)
[P1] Received: match_joined
[P1]   Server assigned: P1
[P2] Connected
[P2] Sent: join_match (matchId=test-1765275298297)
[P1] Received: game_ready
[P1] Game is ready - both players joined!
[P2] Received: match_joined
[P2]   Server assigned: P2
[P2] Received: game_ready
[P2] Game is ready - both players joined!
[P1] Received: your_turn
[P1] Sent: END_TURN (#1)
...
[P1] === GAME OVER! Winner: P1 ===
[P2] === GAME OVER! Winner: P1 ===

=== Test Complete ===
```

## Success Criteria

- [x] Both players connect successfully
- [x] Both players join the same match
- [x] Game ready message received
- [x] State updates received
- [x] Game progresses with END_TURN actions
- [x] Game ends with a winner

## Notes

- The server assigns `P1` and `P2` to players in order of joining
- Actions require `matchId` in the payload
- Test simulates a game by both players sending END_TURN
- Game ends when one hero is killed (via BLEED damage over time)
