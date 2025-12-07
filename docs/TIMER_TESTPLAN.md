# Timer System Test Plan

## Overview

This document defines test cases for the Timer System in Tactics 5x5 V3.

## Timer Types

| Timer | Duration | Timeout Result | Layer |
|-------|----------|----------------|-------|
| Action Timer | 10,000 ms | Hero -1 HP + auto END_TURN | Server |
| Death Choice Timer | 5,000 ms | Default to Obstacle (no HP penalty) | Server |
| Draft Timer | 60,000 ms | Random selection for incomplete choices | Server |

## Design Decisions Summary

| # | Decision |
|---|----------|
| 1 | Action timeout: Hero -1 HP + END_TURN |
| 2 | Death Choice timeout: Default Obstacle, no HP penalty |
| 3 | Death Choice pauses Action Timer |
| 4 | Timer starts when Server sends YOUR_TURN message |
| 5 | Network grace period: 500ms |
| 6 | Multiple Death Choices: Sequential, 5s each |
| 7 | Disconnection: Timer continues (no pause) |
| 8 | Round end processing: Timer paused |
| 9 | After Death Choice: Action Timer resets to 10s |
| 10 | Death Choice: Chosen by dead minion's owner |
| 11 | Multiple Death Choices: Process by Unit ID order |
| 12 | Display: Show currently active Timer only |
| 13 | Victory: Stop all Timers immediately |
| 14 | Consecutive timeouts: No limit, rely on HP depletion |
| 15 | SPEED buff: 10s per action |
| 16 | SLOW buff: Timer only on declaration, not during preparation |
| 17 | Action resolution: Timer paused during entire resolution |
| 18 | Game start: Timer begins immediately |
| 19 | Exhaustion consecutive actions: 10s each |
| 20 | Draft: 60s total, random selection on timeout |

---

## Test Series Overview

| Series | Category | Test Count |
|--------|----------|------------|
| TA | Action Timer - Basic | 12 |
| TD | Death Choice Timer | 15 |
| TF | Draft Timer | 10 |
| TB | Buff Interactions (SPEED/SLOW) | 10 |
| TE | Exhaustion Rule | 6 |
| TN | Network & Sync | 8 |
| TR | Round Processing | 6 |
| TV | Victory & End Game | 5 |
| TI | Integration | 8 |

**Total: 80 test cases**

---

## TA-Series: Action Timer Basic (12 tests)

### TA-001: Action Timer starts on YOUR_TURN message
**Priority**: Critical
```
Given: Game in progress, Player 1's turn
When: Server sends YOUR_TURN message with actionStartTime
Then: Action Timer starts counting down from 10,000ms
```

### TA-002: Valid action within time limit
**Priority**: Critical
```
Given: Action Timer at 5,000ms remaining
When: Player submits valid MOVE action
Then: 
  - Action is processed
  - Timer resets for next unit
  - No HP penalty
```

### TA-003: Action timeout triggers END_TURN
**Priority**: Critical
```
Given: Action Timer at 0ms (timeout)
When: Timer expires
Then:
  - Server auto-executes END_TURN for current unit
  - Player's Hero loses 1 HP
  - Turn passes to next unit
```

### TA-004: Invalid action does not reset Timer
**Priority**: High
```
Given: Action Timer at 3,000ms remaining
When: Player submits invalid action (e.g., move to occupied tile)
Then:
  - Action rejected
  - Timer continues (still ~3,000ms remaining)
  - No reset
```

### TA-005: Timer message contains correct fields
**Priority**: High
```
Given: Server prepares YOUR_TURN message
When: Message is sent
Then: Message contains:
  {
    "type": "YOUR_TURN",
    "actionStartTime": <unix_timestamp_ms>,
    "timeoutMs": 10000,
    "timerType": "ACTION"
  }
```

### TA-006: Timer starts immediately at game start
**Priority**: High
```
Given: Draft phase completed, battle begins
When: First YOUR_TURN message sent to Player 1
Then: Timer starts immediately (no delay)
```

### TA-007: Hero HP penalty on timeout
**Priority**: Critical
```
Given: Player 1's Hero has 5 HP
When: Action Timer expires (timeout)
Then: Player 1's Hero HP = 4
```

### TA-008: Timeout when Hero at 1 HP causes defeat
**Priority**: Critical
```
Given: Player 1's Hero has 1 HP
When: Action Timer expires (timeout)
Then:
  - Hero HP = 0
  - Player 1 loses
  - Game ends
```

### TA-009: Multiple consecutive timeouts
**Priority**: High
```
Given: Player 1's Hero has 3 HP
When: Player 1 times out 3 times consecutively
Then:
  - After timeout 1: Hero HP = 2
  - After timeout 2: Hero HP = 1
  - After timeout 3: Hero HP = 0, Player 1 loses
```

### TA-010: Timer precision is millisecond-level
**Priority**: Medium
```
Given: Server sends actionStartTime = 1702345678901
When: Client receives at 1702345678950 (49ms delay)
Then: Client displays ~9,951ms remaining (accounts for delay)
```

### TA-011: Timer pauses during action resolution
**Priority**: High
```
Given: Player submits ATTACK action at 5,000ms remaining
When: Attack resolves (damage calculation, state update)
Then:
  - Timer paused during resolution
  - Next unit's Timer starts fresh at 10,000ms after resolution
```

### TA-012: END_TURN action resets Timer for next unit
**Priority**: High
```
Given: Player manually submits END_TURN at 2,000ms remaining
When: Action processed
Then:
  - No HP penalty (voluntary end turn)
  - Next unit's Timer starts at 10,000ms
```

---

## TD-Series: Death Choice Timer (15 tests)

### TD-001: Death Choice Timer starts on minion death
**Priority**: Critical
```
Given: Player 1's minion dies from attack
When: Death is processed
Then:
  - Death Choice Timer starts (5,000ms)
  - Action Timer is PAUSED
  - Player 1 (owner) must choose Obstacle or BUFF Tile
```

### TD-002: Death Choice timeout defaults to Obstacle
**Priority**: Critical
```
Given: Death Choice Timer at 0ms (timeout)
When: Timer expires without selection
Then:
  - Obstacle spawned at death position
  - No HP penalty
  - Game continues
```

### TD-003: Valid Death Choice within time limit
**Priority**: Critical
```
Given: Death Choice Timer at 3,000ms remaining
When: Player selects BUFF_TILE
Then:
  - Random BUFF Tile spawned at death position
  - Death Choice Timer stops
  - Action Timer resumes (reset to 10,000ms)
```

### TD-004: Death Choice pauses Action Timer
**Priority**: Critical
```
Given: Action Timer at 4,000ms remaining
When: Attack kills enemy minion, Death Choice starts
Then:
  - Action Timer paused at 4,000ms
  - Death Choice Timer starts at 5,000ms
```

### TD-005: After Death Choice, Action Timer resets to 10s
**Priority**: High
```
Given: Action Timer was paused at 4,000ms
When: Death Choice completed
Then:
  - Action Timer resets to 10,000ms (not 4,000ms)
  - Next unit continues
```

### TD-006: Death Choice by dead minion's owner
**Priority**: Critical
```
Given: Player 1 attacks and kills Player 2's minion
When: Death Choice starts
Then: Player 2 (owner of dead minion) makes the choice
```

### TD-007: Death Choice message format
**Priority**: High
```
Given: Minion death triggers Death Choice
When: Server sends message
Then: Message contains:
  {
    "type": "DEATH_CHOICE",
    "actionStartTime": <unix_timestamp_ms>,
    "timeoutMs": 5000,
    "timerType": "DEATH_CHOICE",
    "deadUnitId": "p2_minion_1",
    "deathPosition": {"x": 2, "y": 3}
  }
```

### TD-008: Multiple Death Choices - sequential processing
**Priority**: High
```
Given: Wild Magic kills 2 enemy minions (p2_minion_1, p2_minion_2)
When: Deaths are processed
Then:
  - First: Death Choice for p2_minion_1 (5s)
  - After first choice: Death Choice for p2_minion_2 (5s)
  - Sequential, not parallel
```

### TD-009: Multiple Death Choices - Unit ID order
**Priority**: High
```
Given: Skill kills p2_minion_2 and p2_minion_1 simultaneously
When: Death Choices are queued
Then: Process in Unit ID order: p2_minion_1 first, then p2_minion_2
```

### TD-010: Death Choice does not affect turn order
**Priority**: High
```
Given: 
  - Player 1's unit attacks
  - Kills Player 2's minion
  - Death Choice completed
When: Game resumes
Then: Normal turn order continues (Player 1's action is complete)
```

### TD-011: Death Choice timeout - no HP penalty
**Priority**: High
```
Given: Player 2's minion dies, Death Choice starts
When: Death Choice Timer expires (5s timeout)
Then:
  - Obstacle spawned (default)
  - Player 2's Hero HP unchanged
  - No penalty for timeout
```

### TD-012: Death Choice during SPEED buff double action
**Priority**: Medium
```
Given: 
  - Player 1's unit has SPEED buff (2 actions)
  - First action kills enemy minion
When: Death Choice starts
Then:
  - Action Timer paused
  - After Death Choice: Timer resets to 10s for second action
```

### TD-013: Display shows Death Choice Timer during choice
**Priority**: Medium
```
Given: Death Choice in progress
When: Client renders UI
Then:
  - Death Choice Timer displayed (5s countdown)
  - Action Timer NOT displayed (paused)
```

### TD-014: Hero death does not trigger Death Choice
**Priority**: High
```
Given: Attack kills enemy Hero
When: Death is processed
Then:
  - No Death Choice (only for minions)
  - Game ends immediately
  - Victory declared
```

### TD-015: Death Choice with multiple owners
**Priority**: Medium
```
Given: AOE skill kills both players' minions (1 each)
When: Death Choices processed
Then:
  - Unit ID order determines sequence
  - Each owner makes their own choice
  - 5s for each choice
```

---

## TF-Series: Draft Timer (10 tests)

### TF-001: Draft Timer starts at draft phase begin
**Priority**: Critical
```
Given: Match created, both players connected
When: Draft phase begins
Then: Draft Timer starts at 60,000ms for both players
```

### TF-002: Draft completed within time limit
**Priority**: Critical
```
Given: Draft Timer at 30,000ms remaining
When: Both players complete all selections
Then:
  - Draft ends normally
  - Battle phase begins
  - No random selections
```

### TF-003: Draft timeout - random Hero selection
**Priority**: Critical
```
Given: Player 1 has not selected Hero Class
When: Draft Timer expires (60s)
Then: System randomly selects 1 of 6 Hero Classes for Player 1
```

### TF-004: Draft timeout - random Minion selection
**Priority**: Critical
```
Given: Player 1 has not selected Minions
When: Draft Timer expires (60s)
Then: System randomly selects 2 different Minion types from {TANK, ARCHER, ASSASSIN}
```

### TF-005: Draft timeout - random Skill selection
**Priority**: Critical
```
Given: Player 1 has not selected Skill (Hero Class already chosen)
When: Draft Timer expires (60s)
Then: System randomly selects 1 of 3 skills available for that Hero Class
```

### TF-006: Partial draft timeout
**Priority**: High
```
Given: Player 1 selected Hero and Minions, but not Skill
When: Draft Timer expires (60s)
Then:
  - Hero: Keep player's selection
  - Minions: Keep player's selection
  - Skill: Randomly selected
```

### TF-007: Draft Timer message format
**Priority**: High
```
Given: Draft phase begins
When: Server sends message
Then: Message contains:
  {
    "type": "DRAFT_START",
    "draftStartTime": <unix_timestamp_ms>,
    "timeoutMs": 60000,
    "timerType": "DRAFT"
  }
```

### TF-008: Both players share same Draft Timer
**Priority**: High
```
Given: Draft Timer at 30,000ms
When: Player 1 completes all selections
Then:
  - Timer continues for Player 2
  - Player 1 waits
  - Draft ends when both complete OR timer expires
```

### TF-009: Draft random selection is deterministic
**Priority**: Medium
```
Given: Draft timeout for Player 1
When: Random selection needed
Then: Use RngProvider for deterministic selection (reproducible for replay)
```

### TF-010: Draft Timer stops on completion
**Priority**: Medium
```
Given: Draft Timer at 45,000ms remaining
When: Both players complete all selections
Then:
  - Draft Timer stops immediately
  - Battle phase begins
  - No wait for remaining time
```

---

## TB-Series: Buff Interactions (10 tests)

### TB-001: SPEED buff - each action gets 10s
**Priority**: Critical
```
Given: Unit has SPEED buff (2 actions per turn)
When: First action completed
Then:
  - First action: 10s Timer
  - Second action: Fresh 10s Timer
  - Total possible: 20s for 2 actions
```

### TB-002: SPEED buff - timeout on first action
**Priority**: High
```
Given: Unit has SPEED buff, Timer at 0 (first action timeout)
When: Timer expires
Then:
  - Hero -1 HP
  - First action = END_TURN (forfeited)
  - Second action still available with fresh 10s
```

### TB-003: SPEED buff - timeout on second action
**Priority**: High
```
Given: Unit has SPEED buff, completed first action
When: Second action Timer expires
Then:
  - Hero -1 HP
  - Second action forfeited
  - Turn passes to next unit
```

### TB-004: SLOW buff - Timer on declaration only
**Priority**: Critical
```
Given: Unit has SLOW buff
When: Player declares action
Then:
  - Must declare within 10s Timer
  - Once declared, action enters "preparing" state
  - No Timer during preparation phase
```

### TB-005: SLOW buff - timeout on declaration
**Priority**: High
```
Given: Unit has SLOW buff, Timer at 0 (declaration timeout)
When: Timer expires without declaration
Then:
  - Hero -1 HP
  - Unit enters "preparing" state with END_TURN
  - Preparation still happens next round
```

### TB-006: SLOW buff - preparation executes without Timer
**Priority**: High
```
Given: SLOW unit declared ATTACK last round, now preparing
When: Preparation round begins
Then:
  - Action executes automatically at round start
  - No Timer (automatic execution)
  - No player input needed
```

### TB-007: SPEED + Death Choice interaction
**Priority**: Medium
```
Given: SPEED unit's first action kills minion
When: Death Choice starts
Then:
  - Death Choice Timer (5s)
  - After Death Choice: 10s for second action
```

### TB-008: SLOW + Death Choice interaction
**Priority**: Medium
```
Given: SLOW unit's preparing action kills minion
When: Death Choice starts
Then:
  - Death Choice Timer (5s)
  - After Death Choice: Continue normal flow
```

### TB-009: SPEED buff expires mid-turn
**Priority**: Low
```
Given: SPEED buff duration = 1, unit has 2 actions
When: First action completes and SPEED expires (if applicable)
Then: 
  - Note: SPEED typically lasts full turn
  - Design decision: SPEED actions = 2 for entire turn once granted
```

### TB-010: Multiple buffs don't affect Timer duration
**Priority**: Medium
```
Given: Unit has POWER, LIFE, and SPEED buffs
When: Action Timer starts
Then: Timer is still 10,000ms (buffs don't modify timer)
```

---

## TE-Series: Exhaustion Rule (6 tests)

### TE-001: Consecutive actions - each gets 10s
**Priority**: Critical
```
Given: 
  - Player 1 has 1 unit remaining
  - Player 2 has 3 units remaining
  - Player 1's unit acts
When: Player 2 takes consecutive turns
Then:
  - P2 Unit 1: 10s Timer
  - P2 Unit 2: 10s Timer
  - P2 Unit 3: 10s Timer
  - Each action independent
```

### TE-002: Timeout during consecutive actions
**Priority**: High
```
Given: Player 2 taking consecutive action (2nd of 3)
When: Action Timer expires
Then:
  - Player 2's Hero -1 HP
  - 2nd unit's action = END_TURN
  - 3rd unit still gets 10s Timer
```

### TE-003: Death Choice during consecutive actions
**Priority**: Medium
```
Given: Player 2's consecutive action kills Player 1's last minion
When: Death Choice starts
Then:
  - Death Choice Timer (5s) for Player 1
  - After choice: Player 2 continues consecutive actions with 10s
```

### TE-004: Exhaustion ends when round ends
**Priority**: High
```
Given: Player 2 completed all consecutive actions
When: Round ends
Then:
  - Normal turn order resumes next round
  - Each unit gets standard 10s Timer
```

### TE-005: Exhaustion after timeout kills last unit
**Priority**: Medium
```
Given: 
  - Player 1's last unit has 1 HP
  - Timeout causes Hero -1 HP
When: Hero dies from timeout penalty
Then:
  - Player 1 loses immediately
  - No Exhaustion rule applies (game over)
```

### TE-006: Both players exhausted simultaneously
**Priority**: Low
```
Given: Both players have 1 unit each
When: Both units have acted
Then:
  - Round ends
  - New round starts
  - Normal Timer (10s) for first unit
```

---

## TN-Series: Network & Sync (8 tests)

### TN-001: Network grace period - 500ms
**Priority**: Critical
```
Given: Server Timer shows 0ms remaining
When: Player action received within 500ms after expiry
Then:
  - Action accepted (within grace period)
  - No timeout penalty
```

### TN-002: Action rejected after grace period
**Priority**: Critical
```
Given: Server Timer expired 600ms ago
When: Player action received
Then:
  - Action rejected (outside 500ms grace)
  - Timeout already processed
```

### TN-003: Client Timer sync with Server timestamp
**Priority**: High
```
Given: Server sends actionStartTime = T
When: Client receives at T + 100ms
Then: Client Timer = 10000 - 100 = 9900ms remaining
```

### TN-004: Disconnection does not pause Timer
**Priority**: Critical
```
Given: Player 1 disconnects during their turn
When: 10s Timer expires
Then:
  - Hero -1 HP (timeout processed)
  - Turn passes to next unit
  - Timer continues regardless of connection
```

### TN-005: Reconnection syncs current Timer state
**Priority**: High
```
Given: 
  - Player 1 disconnected
  - Reconnects after 3 seconds
When: Reconnection established
Then:
  - Current game state sent
  - Timer shows remaining time (~7000ms if still their turn)
```

### TN-006: Server authoritative Timer
**Priority**: Critical
```
Given: Client Timer shows 1000ms, Server Timer shows 0ms
When: Server processes timeout
Then:
  - Server decision is final
  - Client must sync to server state
```

### TN-007: Clock drift handling
**Priority**: Medium
```
Given: Long game (30+ minutes)
When: Client/Server clocks drift
Then:
  - Each Timer message has fresh actionStartTime
  - No cumulative drift (reset each action)
```

### TN-008: Concurrent action submission (race condition)
**Priority**: Medium
```
Given: Timer at 100ms remaining
When: Player submits action as Timer expires on Server
Then:
  - If action received within grace (500ms): Accept action
  - If timeout processed first: Reject action, return error
```

---

## TR-Series: Round Processing (6 tests)

### TR-001: Timer paused during round end processing
**Priority**: Critical
```
Given: All units have acted, round ending
When: Round end processing begins (BLEED, Decay, Pressure)
Then:
  - No Timer active
  - Processing completes
  - New round starts with fresh 10s Timer
```

### TR-002: BLEED damage during round end - no Timer
**Priority**: High
```
Given: Unit has BLEED buff at round end
When: BLEED damage applied
Then:
  - Automatic (no player input)
  - No Timer needed
  - If death occurs: Death Choice Timer starts
```

### TR-003: Minion Decay during round end - no Timer
**Priority**: High
```
Given: Minions alive at round end
When: Decay damage applied (-1 HP each)
Then:
  - Automatic processing
  - No Timer
  - Death Choice if minion dies
```

### TR-004: Round 8 Pressure - no Timer
**Priority**: High
```
Given: Round 8+ begins
When: Pressure damage applied (-1 HP all units)
Then:
  - Automatic processing
  - No Timer
  - Death Choice if minion dies
```

### TR-005: Multiple deaths during round end
**Priority**: Medium
```
Given: Round end kills 2 minions (BLEED + Decay)
When: Death Choices needed
Then:
  - Process by Unit ID order
  - 5s each Death Choice
  - Then new round Timer starts
```

### TR-006: New round Timer after round end
**Priority**: High
```
Given: Round end processing complete
When: New round begins
Then:
  - First acting unit gets 10s Timer
  - Timer starts when YOUR_TURN sent
```

---

## TV-Series: Victory & End Game (5 tests)

### TV-001: Victory stops all Timers
**Priority**: Critical
```
Given: Player 1 kills Player 2's Hero
When: Victory declared
Then:
  - All Timers stopped immediately
  - No further Timer messages sent
  - Game ends
```

### TV-002: Timeout causes victory (Hero HP = 1)
**Priority**: Critical
```
Given: Player 1's Hero has 1 HP
When: Action Timer expires
Then:
  - Hero -1 HP = 0
  - Player 1 loses (Player 2 wins)
  - All Timers stop
```

### TV-003: Simultaneous death - active player wins
**Priority**: High
```
Given: 
  - Player 1 (active) attacks
  - Both Heroes would die (counter-attack scenario)
When: Victory checked
Then:
  - Active player (Player 1) wins
  - Timers stop immediately
```

### TV-004: Draw scenario Timer handling
**Priority**: Low
```
Given: Round 20+ with no clear winner
When: Game continues
Then:
  - Timers continue normally (10s each)
  - Pressure damage will eventually determine winner
```

### TV-005: Surrender stops Timers
**Priority**: Medium
```
Given: Player 1 surrenders during their turn
When: Surrender processed
Then:
  - All Timers stop
  - Player 2 wins
  - Game ends immediately
```

---

## TI-Series: Integration Tests (8 tests)

### TI-001: Full game flow with Timers
**Priority**: Critical
```
Given: Two players start a match
When: Game plays through:
  1. Draft phase (60s limit)
  2. Battle phase (10s per action)
  3. Death Choice (5s when applicable)
  4. Round end processing (no timer)
  5. Victory
Then: All Timers function correctly throughout
```

### TI-002: Draft to Battle Timer transition
**Priority**: High
```
Given: Draft completed at 45s
When: Battle phase begins
Then:
  - Draft Timer stops
  - First action Timer starts (10s)
  - Smooth transition
```

### TI-003: Complex Death Choice scenario
**Priority**: High
```
Given: Wild Magic kills 2 minions, caster has SPEED buff
When: Full sequence executes:
  1. Wild Magic resolves
  2. Death Choice 1 (5s)
  3. Death Choice 2 (5s)
  4. SPEED second action (10s)
Then: All Timers correctly sequenced
```

### TI-004: Network disruption mid-game
**Priority**: High
```
Given: Player 1 disconnects during Death Choice
When: Death Choice Timer expires (5s)
Then:
  - Default Obstacle spawned
  - Game continues
  - Reconnection possible
```

### TI-005: Exhaustion + Death Choice + Round End
**Priority**: Medium
```
Given: 
  - Player 2 in consecutive actions (Exhaustion)
  - Kills minion (Death Choice)
  - Round ends after
When: Full sequence
Then:
  - Death Choice (5s)
  - Continue consecutive action (10s)
  - Round end (no timer)
  - New round (10s)
```

### TI-006: SLOW + Death Choice + Victory
**Priority**: Medium
```
Given: 
  - SLOW unit's preparing attack will kill Hero
  - Attack also kills a minion
When: Preparation executes
Then:
  - Damage applied
  - Victory declared (no Death Choice for Hero)
  - Timers stop
```

### TI-007: Timer stress test - rapid actions
**Priority**: Low
```
Given: Players act quickly (1-2s per action)
When: 20 actions in sequence
Then:
  - All Timers reset correctly
  - No Timer drift
  - Server handles load
```

### TI-008: Full timeout game
**Priority**: Low
```
Given: Player 1 times out every action
When: Multiple timeouts occur
Then:
  - Hero HP decrements correctly
  - Game ends when HP = 0
  - All mechanics still function
```

---

## WebSocket Message Formats

### YOUR_TURN (Action Timer)
```json
{
  "type": "YOUR_TURN",
  "unitId": "p1_hero",
  "actionStartTime": 1702345678901,
  "timeoutMs": 10000,
  "timerType": "ACTION"
}
```

### DEATH_CHOICE (Death Choice Timer)
```json
{
  "type": "DEATH_CHOICE",
  "playerId": "PLAYER_2",
  "deadUnitId": "p2_minion_1",
  "deathPosition": {"x": 2, "y": 3},
  "actionStartTime": 1702345678901,
  "timeoutMs": 5000,
  "timerType": "DEATH_CHOICE"
}
```

### DRAFT_START (Draft Timer)
```json
{
  "type": "DRAFT_START",
  "draftStartTime": 1702345678901,
  "timeoutMs": 60000,
  "timerType": "DRAFT"
}
```

### TIMEOUT (Server notification)
```json
{
  "type": "TIMEOUT",
  "timerType": "ACTION",
  "playerId": "PLAYER_1",
  "penalty": {
    "type": "HERO_HP_LOSS",
    "amount": 1
  },
  "autoAction": "END_TURN"
}
```

### TIMER_SYNC (Reconnection)
```json
{
  "type": "TIMER_SYNC",
  "timerType": "ACTION",
  "remainingMs": 7500,
  "serverTime": 1702345678901
}
```

---

## Implementation Notes

### Server Layer Responsibilities
1. **TimerService** - Manages all timer instances
2. **MatchService** - Integrates timer with game flow
3. **WebSocketHandler** - Sends timer messages to clients

### Timer State Machine
```
IDLE → RUNNING → PAUSED → RUNNING → EXPIRED
         ↓                    ↓
      COMPLETED           TIMEOUT
```

### Pause Triggers
- Death Choice starts
- Action resolution in progress
- Round end processing

### Resume Triggers
- Death Choice completed/timeout
- Action resolution complete

### Reset Triggers
- Valid action completed
- Death Choice completed (reset to 10s)
- New round starts
- Turn passes to next unit

---

## Test Implementation Priority

### Phase 1: Core Timer (Must Have)
- TA-001 to TA-003 (Basic Action Timer)
- TD-001 to TD-003 (Basic Death Choice Timer)
- TF-001 to TF-003 (Basic Draft Timer)
- TN-001, TN-004, TN-006 (Network basics)

### Phase 2: Buff Interactions
- TB-001 to TB-006 (SPEED/SLOW)
- TE-001 to TE-002 (Exhaustion)

### Phase 3: Edge Cases
- Remaining tests from all series

### Phase 4: Integration
- TI-001 to TI-008

---

## Summary

| Category | Tests | Priority |
|----------|-------|----------|
| Action Timer | 12 | Critical |
| Death Choice Timer | 15 | Critical |
| Draft Timer | 10 | High |
| Buff Interactions | 10 | High |
| Exhaustion Rule | 6 | Medium |
| Network & Sync | 8 | Critical |
| Round Processing | 6 | High |
| Victory & End | 5 | High |
| Integration | 8 | Medium |
| **Total** | **80** | - |
