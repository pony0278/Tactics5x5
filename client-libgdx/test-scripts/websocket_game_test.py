#!/usr/bin/env python3
"""
Automated WebSocket test for Tactics 5x5
Simulates two players completing a full game
"""

import asyncio
import websockets
import json
import time

SERVER_URL = "ws://localhost:8080/ws"
MATCH_ID = f"test-{int(time.time() * 1000)}"

async def player(name, hero, minions, actions):
    """Simulate a player"""
    async with websockets.connect(SERVER_URL) as ws:
        print(f"[{name}] Connected")

        # Join match
        await ws.send(json.dumps({
            "type": "join_match",
            "payload": {"matchId": MATCH_ID, "playerId": name}
        }))

        # Wait for match_joined
        msg = await ws.recv()
        data = json.loads(msg)
        print(f"[{name}] Received: {data.get('type')}")

        # Send draft pick
        await ws.send(json.dumps({
            "type": "action",
            "payload": {
                "playerId": name,
                "actionType": "DRAFT_PICK",
                "heroClass": hero,
                "minion1": minions[0],
                "minion2": minions[1]
            }
        }))
        print(f"[{name}] Sent DRAFT_PICK: {hero}")

        # Game loop - wait for state updates and send actions
        while True:
            try:
                msg = await asyncio.wait_for(ws.recv(), timeout=30)
                data = json.loads(msg)
                msg_type = data.get("type")
                print(f"[{name}] Received: {msg_type}")

                if msg_type == "game_over":
                    winner = data.get("payload", {}).get("winner")
                    print(f"[{name}] Game Over! Winner: {winner}")
                    break

                # Check if it's our turn and send action
                if msg_type == "state_update":
                    state = data.get("payload", {})
                    current_player = state.get("currentPlayerId")
                    if current_player == name and actions:
                        action = actions.pop(0)
                        await ws.send(json.dumps(action))
                        print(f"[{name}] Sent action: {action.get('payload', {}).get('actionType')}")

            except asyncio.TimeoutError:
                print(f"[{name}] Timeout waiting for message")
                break

async def main():
    print("=== Tactics 5x5 WebSocket Test ===\n")

    # Define player actions (simplified - just end turns)
    p1_actions = [
        {"type": "action", "payload": {"playerId": "player-1", "actionType": "END_TURN"}}
        for _ in range(10)
    ]
    p2_actions = [
        {"type": "action", "payload": {"playerId": "player-2", "actionType": "END_TURN"}}
        for _ in range(10)
    ]

    # Run both players concurrently
    await asyncio.gather(
        player("player-1", "WARRIOR", ["TANK", "ARCHER"], p1_actions),
        player("player-2", "MAGE", ["TANK", "ASSASSIN"], p2_actions)
    )

    print("\n=== Test Complete ===")

if __name__ == "__main__":
    asyncio.run(main())
