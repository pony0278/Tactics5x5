# Timer System Design Decisions

## Overview

This document records all confirmed design decisions for the Timer System in Tactics 5x5 V3.

---

## Timer Types

| Timer | Duration | Timeout Result | Grace Period |
|-------|----------|----------------|--------------|
| Action Timer | 10,000 ms | Hero -1 HP + auto END_TURN | 500ms |
| Death Choice Timer | 5,000 ms | Default Obstacle (no HP penalty) | 500ms |
| Draft Timer | 60,000 ms | Random selection | 500ms |

---

## Complete Design Decisions (22 Items)

### Action Timer

| # | Decision | Details |
|---|----------|---------|
| 1 | Timeout Result | Hero -1 HP + Server auto END_TURN |
| 2 | Invalid Action | Does NOT reset Timer (continues countdown) |
| 3 | Timer Start | When Server sends YOUR_TURN message |
| 4 | Timer Precision | Millisecond-level |
| 5 | Network Grace | 500ms tolerance after expiry |
| 6 | Disconnection | Timer continues (no pause) |
| 7 | Consecutive Timeouts | No limit, rely on HP depletion |
| 8 | Game Start | Timer begins immediately |

### Death Choice Timer

| # | Decision | Details |
|---|----------|---------|
| 9 | Timeout Result | Default to Obstacle, NO HP penalty |
| 10 | Chooser | Dead minion's **owner** makes choice |
| 11 | Action Timer State | **Paused** during Death Choice |
| 12 | After Completion | Action Timer **resets to 10s** (not resume) |
| 13 | Multiple Deaths | **Sequential** processing, 5s each |
| 14 | Processing Order | By **Unit ID** (lowest first) |
| 15 | Turn Order | Does NOT affect turn order |
| 16 | Hero Death | No Death Choice (game ends immediately) |

### Draft Timer

| # | Decision | Details |
|---|----------|---------|
| 17 | Duration | 60 seconds total for entire draft |
| 18 | Timeout - Hero | Random 6 → 1 |
| 19 | Timeout - Minions | Random 2 from {TANK, ARCHER, ASSASSIN} |
| 20 | Timeout - Skill | Random 3 → 1 (from Hero's skill pool) |
| 21 | Both Players | Share same 60s Timer |
| 22 | Random Selection | Use RngProvider (deterministic) |

### Buff Interactions

| # | Decision | Details |
|---|----------|---------|
| 23 | SPEED Buff | Each of 2 actions gets 10s (total 20s possible) |
| 24 | SLOW Buff | Timer only during declaration phase |
| 25 | SLOW Preparation | No Timer (automatic execution) |

### Special Scenarios

| # | Decision | Details |
|---|----------|---------|
| 26 | Exhaustion Rule | Each consecutive action gets 10s |
| 27 | Round End Processing | Timer **paused** (BLEED, Decay, Pressure) |
| 28 | Action Resolution | Timer **paused** during entire resolution |
| 29 | Victory | **Stop all Timers** immediately |

### UI/Display

| # | Decision | Details |
|---|----------|---------|
| 30 | During Pause | Show currently active Timer only |
| 31 | Death Choice UI | Show Death Choice Timer, hide Action Timer |

---

## Timer State Machine

```
┌─────────────────────────────────────────────────────────┐
│                     Timer States                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│   ┌──────┐     start      ┌─────────┐                  │
│   │ IDLE │ ──────────────▶│ RUNNING │                  │
│   └──────┘                └────┬────┘                  │
│       ▲                        │                        │
│       │                   pause│resume                  │
│       │                        ▼                        │
│       │                   ┌─────────┐                  │
│       │                   │ PAUSED  │                  │
│       │                   └────┬────┘                  │
│       │                        │                        │
│       │         ┌──────────────┼──────────────┐        │
│       │         ▼              ▼              ▼        │
│       │   ┌──────────┐   ┌─────────┐   ┌─────────┐    │
│       └───│COMPLETED │   │ TIMEOUT │   │ RUNNING │    │
│           └──────────┘   └─────────┘   └─────────┘    │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Pause/Resume Triggers

### Pause Triggers
| Trigger | Timer Affected |
|---------|----------------|
| Death Choice starts | Action Timer |
| Action resolution begins | Action Timer |
| Round end processing | Action Timer |

### Resume Triggers (Reset to 10s)
| Trigger | Details |
|---------|---------|
| Death Choice completed | Action Timer resets to 10,000ms |
| Action resolution complete | Next unit gets 10,000ms |
| New round starts | First unit gets 10,000ms |

---

## WebSocket Message Formats

### YOUR_TURN
```json
{
  "type": "YOUR_TURN",
  "unitId": "p1_hero",
  "actionStartTime": 1702345678901,
  "timeoutMs": 10000,
  "timerType": "ACTION"
}
```

### DEATH_CHOICE
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

### DRAFT_START
```json
{
  "type": "DRAFT_START",
  "draftStartTime": 1702345678901,
  "timeoutMs": 60000,
  "timerType": "DRAFT"
}
```

### TIMEOUT
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

---

## Implementation Architecture

### Server Layer

```
server/
├── timer/
│   ├── TimerService.java       # Timer lifecycle management
│   ├── TimerState.java         # Timer state enum
│   ├── TimerConfig.java        # Duration constants
│   └── TimerCallback.java      # Timeout handlers
├── core/
│   └── MatchService.java       # Integrates with TimerService
└── ws/
    └── WebSocketHandler.java   # Timer message sending
```

### TimerService Responsibilities
1. Create/start timer for each action
2. Pause/resume on Death Choice
3. Handle timeout → callback to MatchService
4. Track remaining time for reconnection sync

### MatchService Integration
```java
// On player's turn
timerService.startActionTimer(playerId, () -> {
    // Timeout callback
    applyHeroDamage(playerId, 1);
    autoEndTurn(playerId);
});

// On Death Choice
timerService.pauseActionTimer();
timerService.startDeathChoiceTimer(ownerId, () -> {
    // Timeout callback
    spawnObstacle(deathPosition);
    timerService.resumeActionTimer(); // Resets to 10s
});
```

---

## Client Implementation Notes

### Timer Display
```javascript
// Calculate remaining time
const elapsed = Date.now() - serverTimestamp;
const remaining = timeoutMs - elapsed;

// Display with 500ms grace tolerance
if (remaining > -500) {
    displayTimer(Math.max(0, remaining));
}
```

### Reconnection Sync
```javascript
// On reconnection, server sends TIMER_SYNC
// Client recalculates based on server time
const remaining = message.remainingMs - (Date.now() - message.serverTime);
```

---

## Edge Cases Summary

| Scenario | Handling |
|----------|----------|
| Timeout kills Hero | Game ends, Player loses |
| Multiple Death Choices | Sequential by Unit ID, 5s each |
| SPEED + Death Choice | After choice, 10s for remaining action |
| SLOW timeout | Enters "preparing" END_TURN state |
| Disconnect during Death Choice | Timer continues, default Obstacle on timeout |
| Draft timeout - partial | Keep completed selections, random for rest |

---

## Constants

```java
public class TimerConfig {
    public static final long ACTION_TIMEOUT_MS = 10_000;
    public static final long DEATH_CHOICE_TIMEOUT_MS = 5_000;
    public static final long DRAFT_TIMEOUT_MS = 60_000;
    public static final long GRACE_PERIOD_MS = 500;
}
```
