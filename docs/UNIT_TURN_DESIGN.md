# Unit-by-Unit Turn System Design Decisions

## Overview

This document records design decisions for migrating from team-based turns to unit-by-unit alternating turns.

---

## Current vs Target System

| Aspect | Current (Team-based) | Target (Unit-by-unit) |
|--------|---------------------|----------------------|
| Turn Scope | Entire team acts, then switch | One unit acts, then switch |
| END_TURN | Marks ALL units as acted | Marks ONE unit as acted |
| Unit Selection | Any unit, any order | Player chooses which unused unit |
| Exhaustion | Not applicable | Opponent takes consecutive turns |

---

## Confirmed Design Decisions

### 1. SPEED Buff: Consecutive Actions (Option B)

```
P1 has Unit A (SPEED buff) and Unit B
P2 has Unit X and Unit Y

Turn Order:
1. P1-UnitA (action 1)
2. P1-UnitA (action 2)  ← SPEED: both actions consecutively
3. P2-UnitX
4. P1-UnitB
5. P2-UnitY
6. Round End
```

**Rationale**:
- Simpler implementation
- Consistent with Timer design (10s per action)
- Tactically sensible (SPEED = rapid consecutive strikes)

### 2. Unit Selection: Free Choice (Option A)

```
P1's turn, has 3 unused units: Hero, Minion1, Minion2
→ P1 can choose ANY of these 3 to act
```

**Implementation**:
- Action.actingUnitId specifies which unit
- Validation: unit must belong to current player AND not yet acted this round

### 3. Turn Switching Logic

```java
// After each action (MOVE, ATTACK, USE_SKILL, END_TURN):
PlayerId next = getNextActingPlayer(state);
// - If opponent has unused units → switch to opponent
// - If current player has unused units → stay (Exhaustion!)
// - If all units acted → round end
```

### 4. Exhaustion Rule

```
P1: 1 unit remaining (Hero)
P2: 3 units remaining (Hero, Minion1, Minion2)

Turn Order:
1. P1-Hero acts
2. P2-Hero acts
3. P2-Minion1 acts (Exhaustion: P1 has no unused units)
4. P2-Minion2 acts (Exhaustion continues)
5. Round End
```

Each consecutive action gets fresh 10s timer.

---

## Implementation Impact Analysis

### Phase 1: Core Engine Changes

| File | Change | Complexity |
|------|--------|------------|
| ActionExecutor.java | `applyEndTurn()` marks only acting unit | Medium |
| ActionExecutor.java | Add turn switch after MOVE/ATTACK/USE_SKILL | Medium |
| ActionValidator.java | Validate actingUnitId (belongs to player, not acted) | Medium |
| GameState.java | May need `getUnactedUnits(PlayerId)` helper | Low |

### Phase 2: Timer Integration

| File | Change | Complexity |
|------|--------|------------|
| MatchService.java | Timer tracks per-unit, not per-team | Low |
| WebSocket messages | YOUR_TURN includes available unit IDs | Low |

### Phase 3: Test Updates

| Test Class | Impact | Reason |
|------------|--------|--------|
| RuleEngineApplyActionTest | 🔴 HIGH | Assumes END_TURN ends team |
| MatchServiceTest | 🔴 HIGH | Turn switching logic |
| MatchServiceTimerTest | 🟡 MEDIUM | Update to per-unit timer |
| RuleEngineSkillTest | 🟡 MEDIUM | Post-skill turn control |
| RuleEngineSpeedBuffTest | 🟡 MEDIUM | SPEED consecutive actions |
| RuleEngineGuardianTest | 🟢 LOW | Guardian logic unchanged |
| BuffFactoryTest | 🟢 LOW | Buff creation unchanged |
| DraftIntegrationTest | 🟢 LOW | Draft unaffected |

---

## WebSocket Protocol Changes

### YOUR_TURN Message (Updated)

```json
{
  "type": "YOUR_TURN",
  "playerId": "PLAYER_1",
  "availableUnitIds": ["p1_hero", "p1_minion_1"],  // Units that can act
  "actionStartTime": 1702345678901,
  "timeoutMs": 10000,
  "timerType": "ACTION"
}
```

### ACTION Message (No change needed)

```json
{
  "type": "ACTION",
  "actionType": "MOVE",
  "actingUnitId": "p1_hero",  // Already exists
  "targetPosition": {"x": 2, "y": 1}
}
```

---

## Migration Strategy

### Step 1: Analyze Test Impact (Current)
- Count affected tests
- Identify test patterns to update

### Step 2: Create New Test Class
- `RuleEngineUnitTurnTest.java` for unit-by-unit specific tests
- Write failing tests first (TDD)

### Step 3: Core Implementation
- Modify `applyEndTurn()` first
- Update turn switching after actions
- Add validation for actingUnitId

### Step 4: Update Existing Tests
- Batch update tests by pattern
- Many tests just need to add explicit END_TURN calls

### Step 5: Timer Integration
- Update MatchService timer to per-unit
- Update Exhaustion tests

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Breaking many tests | Analyze first, batch update by pattern |
| Client compatibility | Update WebSocket messages incrementally |
| SPEED buff complexity | Test thoroughly with dedicated test class |
| Regression | Run full test suite after each step |

---

## Success Criteria

- [ ] All 584+ existing tests pass (with updates)
- [ ] New unit-by-unit tests pass
- [ ] Exhaustion Rule works correctly
- [ ] SPEED buff consecutive actions work
- [ ] Timer resets correctly per-unit
- [ ] Client receives correct YOUR_TURN messages
