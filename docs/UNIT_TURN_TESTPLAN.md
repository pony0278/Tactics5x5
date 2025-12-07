# Unit-by-Unit Turn System Test Plan

## Overview

This document defines test cases for the Unit-by-Unit Turn System migration.

## Design Decisions Reference

| Decision | Value |
|----------|-------|
| SPEED Buff | Consecutive actions (both actions before switch) |
| Unit Selection | Free choice (player picks any unused unit) |
| END_TURN | Marks only ONE unit as acted |
| Turn Switch | After each action, check `getNextActingPlayer()` |

---

## Test Series Overview

| Series | Category | Test Count |
|--------|----------|------------|
| UT | Unit Turn - Basic | 12 |
| UE | Unit Turn - Exhaustion Rule | 8 |
| US | Unit Turn - SPEED Buff | 6 |
| UV | Unit Turn - Validation | 8 |
| UI | Unit Turn - Integration | 6 |

**Total: 40 test cases**

---

## UT-Series: Unit Turn Basic (12 tests)

### UT-001: Single unit acts, turn switches to opponent
**Priority**: Critical
```
Given: 
  - P1 has: Hero, Minion1, Minion2 (all unacted)
  - P2 has: Hero, Minion1, Minion2 (all unacted)
  - Current player: P1
When: P1's Hero performs MOVE action
Then:
  - P1's Hero marked as acted
  - Current player switches to P2
  - P2 can choose any of their 3 units
```

### UT-002: Turn alternates between players
**Priority**: Critical
```
Given: Initial state, P1's turn
When: 
  1. P1's Hero acts
  2. P2's Hero acts
  3. P1's Minion1 acts
  4. P2's Minion1 acts
Then: Turn alternates P1 → P2 → P1 → P2
```

### UT-003: END_TURN marks only acting unit
**Priority**: Critical
```
Given: P1's turn, none of P1's units have acted
When: P1 calls END_TURN for Hero
Then:
  - P1's Hero.hasActed = true
  - P1's Minion1.hasActed = false
  - P1's Minion2.hasActed = false
  - Turn switches to P2
```

### UT-004: Player can choose any unacted unit
**Priority**: Critical
```
Given:
  - P1's turn
  - P1's Hero has acted
  - P1's Minion1 and Minion2 have NOT acted
When: P1 submits action for Minion2 (skipping Minion1)
Then: Action is valid and processed
```

### UT-005: Round ends when all units have acted
**Priority**: Critical
```
Given: 
  - P1: 2 units (Hero, Minion1)
  - P2: 2 units (Hero, Minion1)
  - Round 1
When: All 4 units act (any order)
Then:
  - Round increments to 2
  - All units reset to hasActed = false
```

### UT-006: MOVE action switches turn
**Priority**: High
```
Given: P1's turn, P1's Hero unacted
When: P1's Hero performs MOVE
Then:
  - Hero marked as acted
  - Turn switches to P2
```

### UT-007: ATTACK action switches turn
**Priority**: High
```
Given: P1's turn, P1's Hero unacted, enemy in range
When: P1's Hero performs ATTACK
Then:
  - Hero marked as acted
  - Turn switches to P2
```

### UT-008: USE_SKILL action switches turn
**Priority**: High
```
Given: P1's turn, P1's Hero unacted, skill available
When: P1's Hero performs USE_SKILL
Then:
  - Hero marked as acted
  - Turn switches to P2
```

### UT-009: MOVE_AND_ATTACK action switches turn
**Priority**: High
```
Given: P1's turn, P1's Assassin unacted
When: P1's Assassin performs MOVE_AND_ATTACK
Then:
  - Assassin marked as acted
  - Turn switches to P2
```

### UT-010: Cannot act with already-acted unit
**Priority**: High
```
Given: 
  - P1's turn (via Exhaustion)
  - P1's Hero already acted this round
When: P1 tries to act with Hero again
Then: Action rejected (unit already acted)
```

### UT-011: Unit hasActed resets at round start
**Priority**: High
```
Given: Round 1 ended, all units acted
When: Round 2 starts
Then: All units have hasActed = false
```

### UT-012: getUnactedUnits returns correct list
**Priority**: Medium
```
Given:
  - P1's Hero acted
  - P1's Minion1 not acted
  - P1's Minion2 not acted
When: getUnactedUnits(PLAYER_1) called
Then: Returns [Minion1, Minion2]
```

---

## UE-Series: Exhaustion Rule (8 tests)

### UE-001: Exhaustion - opponent takes consecutive turns
**Priority**: Critical
```
Given:
  - P1 has: 1 unit (Hero)
  - P2 has: 3 units (Hero, Minion1, Minion2)
When: P1's Hero acts
Then:
  - P2's turn
  - After P2's Hero acts → still P2's turn (P1 exhausted)
  - After P2's Minion1 acts → still P2's turn
  - After P2's Minion2 acts → Round End
```

### UE-002: Exhaustion - each consecutive action gets 10s timer
**Priority**: Critical
```
Given: Exhaustion scenario (P1 has 0 unacted, P2 has 2 unacted)
When: P2 takes consecutive actions
Then:
  - Action 1: fresh 10s timer
  - Action 2: fresh 10s timer
  - Each action independent
```

### UE-003: Exhaustion ends when round ends
**Priority**: High
```
Given: P2 took consecutive turns due to P1 exhaustion
When: New round starts
Then:
  - P1 has units to act again
  - Normal alternation resumes
```

### UE-004: Both players have units - no exhaustion
**Priority**: High
```
Given:
  - P1 has: 2 unacted units
  - P2 has: 2 unacted units
When: P1's unit acts
Then: Turn switches to P2 (no consecutive turns)
```

### UE-005: Exhaustion with SPEED buff
**Priority**: Medium
```
Given:
  - P1 has: 1 unit (Hero with SPEED, 2 actions)
  - P2 has: 2 units
When: 
  1. P1's Hero acts (action 1)
  2. P1's Hero acts (action 2) - SPEED consecutive
  3. P1 now exhausted
Then: P2 takes 2 consecutive turns
```

### UE-006: Exhaustion after unit death
**Priority**: Medium
```
Given:
  - P1 has: 2 units (Hero, Minion1)
  - P2 has: 2 units
When: P2 kills P1's Minion1, P1's Hero already acted
Then: P1 exhausted, P2 takes remaining turns
```

### UE-007: Exhaustion alternates if both have 1 unit each
**Priority**: Medium
```
Given:
  - P1 has: 1 unit (Hero)
  - P2 has: 1 unit (Hero)
When: P1's Hero acts
Then: Turn switches to P2 (no exhaustion, P2 also has only 1)
```

### UE-008: Exhaustion with 0 units = round ends
**Priority**: Low
```
Given:
  - P1 has: 0 unacted units
  - P2 has: 0 unacted units
When: Last unit acts
Then: Round ends, no exhaustion
```

---

## US-Series: SPEED Buff (6 tests)

### US-001: SPEED unit acts twice consecutively
**Priority**: Critical
```
Given:
  - P1's Hero has SPEED buff
  - P2 has units available
When: P1's Hero performs first action
Then:
  - Hero NOT marked as fully acted
  - Turn stays with P1
  - P1 must act with same Hero again
```

### US-002: SPEED - second action completes turn
**Priority**: Critical
```
Given:
  - P1's Hero has SPEED buff
  - P1's Hero performed first action
When: P1's Hero performs second action
Then:
  - Hero marked as acted
  - Turn switches to P2
```

### US-003: SPEED - cannot switch to different unit mid-SPEED
**Priority**: High
```
Given:
  - P1's Hero has SPEED buff
  - P1's Hero performed first action
When: P1 tries to act with Minion1
Then: Action rejected (must complete SPEED actions with Hero)
```

### US-004: SPEED - END_TURN forfeits remaining action
**Priority**: High
```
Given:
  - P1's Hero has SPEED buff
  - P1's Hero performed first action
When: P1 calls END_TURN for Hero
Then:
  - Hero marked as acted (forfeits second action)
  - Turn switches to P2
```

### US-005: SPEED - timer resets for each action
**Priority**: High
```
Given: P1's Hero has SPEED buff
When: First action completes
Then:
  - Fresh 10s timer for second action
  - Total possible time: 20s
```

### US-006: SPEED + Exhaustion interaction
**Priority**: Medium
```
Given:
  - P1's Hero has SPEED buff (only unit)
  - P2 has 2 units
Turn Order:
  1. P1's Hero (action 1)
  2. P1's Hero (action 2) - SPEED
  3. P2's Hero - P1 exhausted
  4. P2's Minion1 - P1 exhausted
  5. Round End
```

---

## UV-Series: Validation (8 tests)

### UV-001: Validate actingUnitId belongs to current player
**Priority**: Critical
```
Given: P1's turn
When: Action submitted with actingUnitId = "p2_hero"
Then: Action rejected (unit belongs to opponent)
```

### UV-002: Validate actingUnitId exists
**Priority**: Critical
```
Given: P1's turn
When: Action submitted with actingUnitId = "invalid_id"
Then: Action rejected (unit not found)
```

### UV-003: Validate unit has not acted this round
**Priority**: Critical
```
Given: P1's turn, P1's Hero already acted
When: Action submitted for P1's Hero
Then: Action rejected (unit already acted)
```

### UV-004: Validate unit is alive
**Priority**: High
```
Given: P1's turn, P1's Minion1 is dead
When: Action submitted for P1's Minion1
Then: Action rejected (unit is dead)
```

### UV-005: END_TURN requires actingUnitId
**Priority**: High
```
Given: P1's turn
When: END_TURN submitted without actingUnitId
Then: Action rejected (must specify which unit ends turn)
```

### UV-006: END_TURN actingUnitId must be unacted
**Priority**: High
```
Given: P1's turn, P1's Hero already acted
When: END_TURN submitted for P1's Hero
Then: Action rejected (unit already acted)
```

### UV-007: MOVE actingUnitId must match unit at source
**Priority**: Medium
```
Given: 
  - P1's Hero at (2,0)
  - P1's Minion1 at (0,0)
When: MOVE from (2,0) with actingUnitId = "p1_minion_1"
Then: Action rejected (unit mismatch)
```

### UV-008: ATTACK actingUnitId must match attacker
**Priority**: Medium
```
Given: P1's Hero at (2,0), can attack target
When: ATTACK with actingUnitId = "p1_minion_1" (not at position)
Then: Action rejected (unit mismatch)
```

---

## UI-Series: Integration (6 tests)

### UI-001: Full round with 3v3 units
**Priority**: Critical
```
Given:
  - P1: Hero, Minion1, Minion2
  - P2: Hero, Minion1, Minion2
  - Round 1
When: All 6 units act in alternating fashion
Then:
  - Turn order: P1→P2→P1→P2→P1→P2
  - Round ends after 6 actions
  - Round 2 starts
```

### UI-002: Round with unequal unit counts
**Priority**: High
```
Given:
  - P1: Hero, Minion1 (2 units)
  - P2: Hero, Minion1, Minion2 (3 units)
When: All units act
Then:
  - P1→P2→P1→P2 (alternating until P1 exhausted)
  - P2→Round End (P2's last unit, exhaustion)
```

### UI-003: Unit death mid-round affects turn order
**Priority**: High
```
Given:
  - P1: Hero, Minion1
  - P2: Hero, Minion1
  - P1's Hero acts, kills P2's Minion1
When: Turn continues
Then:
  - P2 has 1 unacted unit (Hero)
  - P1 has 1 unacted unit (Minion1)
  - Alternation continues normally
```

### UI-004: SPEED + regular unit in same round
**Priority**: High
```
Given:
  - P1: Hero (SPEED), Minion1
  - P2: Hero, Minion1
Turn Order:
  1. P1's Hero (action 1)
  2. P1's Hero (action 2) - SPEED
  3. P2's Hero
  4. P1's Minion1
  5. P2's Minion1
  6. Round End
```

### UI-005: Timer integration - per unit
**Priority**: High
```
Given: Unit-by-unit turn system active
When: Each unit's turn starts
Then:
  - Fresh 10s timer
  - YOUR_TURN message includes available unit IDs
  - Timer independent of previous unit's time
```

### UI-006: Death Choice pauses then resumes unit turn
**Priority**: Medium
```
Given:
  - P1's Hero attacks, kills P2's Minion1
  - Death Choice required
When: Death Choice completed
Then:
  - Turn properly switches to next player
  - Timer resets to 10s
  - Correct unit availability
```

---

## WebSocket Message Updates

### YOUR_TURN (Updated)
```json
{
  "type": "YOUR_TURN",
  "playerId": "PLAYER_1",
  "availableUnitIds": ["p1_hero", "p1_minion_1"],
  "speedUnitId": null,
  "actionStartTime": 1702345678901,
  "timeoutMs": 10000,
  "timerType": "ACTION"
}
```

### YOUR_TURN during SPEED
```json
{
  "type": "YOUR_TURN",
  "playerId": "PLAYER_1",
  "availableUnitIds": ["p1_hero"],
  "speedUnitId": "p1_hero",
  "speedActionsRemaining": 1,
  "actionStartTime": 1702345678901,
  "timeoutMs": 10000,
  "timerType": "ACTION"
}
```

---

## Implementation Priority

### Phase 1: Core Turn Logic
| Tests | Description |
|-------|-------------|
| UT-001 ~ UT-005 | Basic turn switching |
| UV-001 ~ UV-004 | Core validation |

### Phase 2: Action Types
| Tests | Description |
|-------|-------------|
| UT-006 ~ UT-012 | All action types switch turn |
| UV-005 ~ UV-008 | Action-specific validation |

### Phase 3: SPEED Buff
| Tests | Description |
|-------|-------------|
| US-001 ~ US-006 | SPEED consecutive actions |

### Phase 4: Exhaustion Rule
| Tests | Description |
|-------|-------------|
| UE-001 ~ UE-008 | Exhaustion scenarios |

### Phase 5: Integration
| Tests | Description |
|-------|-------------|
| UI-001 ~ UI-006 | Full game flow |

---

## Test File Structure

```
src/test/java/com/tactics/engine/rules/
├── RuleEngineUnitTurnTest.java      # UT-series
├── RuleEngineExhaustionTest.java    # UE-series
├── RuleEngineSpeedTurnTest.java     # US-series
├── RuleEngineUnitValidationTest.java # UV-series
└── RuleEngineUnitTurnIntegrationTest.java # UI-series
```

---

## Success Criteria

- [ ] All 40 new unit-turn tests pass
- [ ] All 584 existing tests pass (with updates)
- [ ] Turn alternates correctly between players
- [ ] Exhaustion Rule triggers when appropriate
- [ ] SPEED buff allows consecutive actions
- [ ] Timer resets correctly per-unit
- [ ] WebSocket messages include unit availability
