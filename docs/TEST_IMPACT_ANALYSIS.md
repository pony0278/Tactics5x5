# Test Impact Analysis: Unit-by-Unit Turn Migration

## Overview

This document analyzes which tests will be affected by migrating from team-based to unit-by-unit turns.

---

## Test Pattern Categories

### Pattern A: Tests that call END_TURN once for entire team

**Current Pattern**:
```java
// P1 moves unit A, then unit B, then ends turn
state = applyAction(Action.move("p1_hero", pos(2,1)), state);
state = applyAction(Action.move("p1_minion_1", pos(1,1)), state);
state = applyAction(Action.endTurn("PLAYER_1"), state);
// Now P2's turn
```

**Required Change**:
```java
// Each action needs to be followed by turn switch or explicit unit marking
state = applyAction(Action.move("p1_hero", pos(2,1)), state);
// Turn automatically switches to P2 if P2 has unused units
// OR P1 must END_TURN for p1_hero specifically
```

**Impact**: 🔴 HIGH - Many tests follow this pattern

---

### Pattern B: Tests that assume currentPlayer stays same

**Current Pattern**:
```java
state = applyAction(Action.move("p1_hero", pos(2,1)), state);
assertEquals(PlayerId.PLAYER_1, state.getCurrentPlayer()); // Still P1's turn
state = applyAction(Action.attack("p1_hero", target), state);
```

**Required Change**:
```java
state = applyAction(Action.move("p1_hero", pos(2,1)), state);
// After MOVE, turn may switch to P2!
// If testing consecutive actions by same unit, need SPEED buff
```

**Impact**: 🔴 HIGH - Assumption broken

---

### Pattern C: Tests that check currentPlayer after action

**Current Pattern**:
```java
state = applyAction(Action.endTurn("PLAYER_1"), state);
assertEquals(PlayerId.PLAYER_2, state.getCurrentPlayer());
```

**Required Change**:
```java
// END_TURN now only marks ONE unit as acted
// Turn switch depends on whether opponent has unused units
state = applyAction(Action.endTurn("p1_hero"), state);  // Acting unit ID
// getCurrentPlayer() may be P1 or P2 depending on game state
```

**Impact**: 🟡 MEDIUM - Logic change, but predictable

---

### Pattern D: Tests for round end

**Current Pattern**:
```java
// P1 ends turn, P2 ends turn → round should end
state = applyAction(Action.endTurn("PLAYER_1"), state);
state = applyAction(Action.endTurn("PLAYER_2"), state);
assertEquals(2, state.getCurrentRound()); // New round
```

**Required Change**:
```java
// All units from both sides must have acted
// P1: hero, minion1, minion2 (3 units)
// P2: hero, minion1, minion2 (3 units)
// Need 6 actions total before round ends
```

**Impact**: 🟡 MEDIUM - Need to account for all units

---

### Pattern E: Exhaustion-related tests

**Current Pattern**: None (team-based doesn't have exhaustion)

**Required Change**: Add new tests for Exhaustion Rule

**Impact**: 🟢 NEW TESTS - No existing tests to break

---

### Pattern F: SPEED buff tests

**Current Pattern**:
```java
// SPEED unit gets 2 actions in same turn
state = applyAction(Action.move("p1_hero", pos(2,1)), state);  // Action 1
assertEquals(PlayerId.PLAYER_1, state.getCurrentPlayer());  // Still P1
state = applyAction(Action.attack("p1_hero", target), state); // Action 2
```

**Required Change**:
```java
// SPEED unit gets 2 consecutive actions (confirmed: Option B)
// Same logic, but need to verify turn stays with SPEED unit
state = applyAction(Action.move("p1_hero", pos(2,1)), state);
// Should still be P1's turn because hero has actionsRemaining = 1
assertEquals(PlayerId.PLAYER_1, state.getCurrentPlayer());
```

**Impact**: 🟡 MEDIUM - Logic similar, may need adjustment

---

## Estimated Test Counts by Impact

| Impact Level | Estimated Tests | Action Needed |
|--------------|-----------------|---------------|
| 🔴 HIGH | ~80-120 | Significant refactor |
| 🟡 MEDIUM | ~40-60 | Minor adjustments |
| 🟢 LOW/NONE | ~400+ | No change needed |

---

## Test Classes Analysis

### 🔴 HIGH IMPACT

| Test Class | Est. Tests | Reason |
|------------|------------|--------|
| RuleEngineApplyActionTest | ~30 | END_TURN semantics change |
| MatchServiceTest | ~20 | Turn switching logic |
| RuleEngineSkillTest | ~24 | Post-skill turn state |
| RuleEngineSkillPhase4BTest | ~31 | Post-skill turn state |
| RuleEngineSkillPhase4CTest | ~26 | Movement skill turn state |
| RuleEngineSkillPhase4DTest | ~14 | Complex skill turn state |

### 🟡 MEDIUM IMPACT

| Test Class | Est. Tests | Reason |
|------------|------------|--------|
| MatchServiceTimerTest | ~25 | Timer per-unit vs per-team |
| RuleEngineSpeedBuffTest | ~7 | SPEED action tracking |
| RuleEngineSlowBuffTest | ~7 | SLOW action tracking |
| RuleEngineDeathChoiceTest | ~17 | Turn state after death |

### 🟢 LOW/NO IMPACT

| Test Class | Est. Tests | Reason |
|------------|------------|--------|
| RuleEngineValidateActionTest | ~40 | Validation logic unchanged |
| RuleEngineValidateActionV2Test | ~20 | Range validation unchanged |
| BuffFactoryTest | ~12 | Buff creation unchanged |
| BuffTileTest | ~10 | Tile logic unchanged |
| RuleEngineGuardianTest | ~16 | Guardian intercept unchanged |
| RuleEngineAttritionTest | ~11 | Decay/pressure unchanged |
| DraftStateTest | ~44 | Draft unaffected |
| DraftResultTest | ~12 | Draft unaffected |
| DraftSetupServiceTest | ~38 | Draft unaffected |
| DraftIntegrationTest | ~16 | Draft unaffected |
| GameStateSerializerTest | ~10 | Serialization unchanged |

---

## Migration Strategy

### Step 1: Create Feature Flag (Optional)
```java
public class GameConfig {
    public static boolean UNIT_BY_UNIT_TURNS = false; // Toggle
}
```

### Step 2: Write New Tests First
Create `RuleEngineUnitTurnTest.java` with unit-by-unit specific tests.
These will fail initially.

### Step 3: Implement Core Changes
1. `applyEndTurn()` - mark only acting unit
2. Turn switching after each action
3. `actingUnitId` validation

### Step 4: Batch Update Existing Tests

**Common Fix Pattern A**: Add explicit END_TURN per unit
```java
// Before
state = applyAction(Action.endTurn("PLAYER_1"), state);

// After
state = applyAction(Action.endTurn("p1_hero"), state);
state = applyAction(Action.endTurn("p1_minion_1"), state);
state = applyAction(Action.endTurn("p1_minion_2"), state);
```

**Common Fix Pattern B**: Check correct player after action
```java
// Before
assertEquals(PlayerId.PLAYER_1, state.getCurrentPlayer());

// After - depends on game state
PlayerId expected = getExpectedPlayer(state); // Helper method
assertEquals(expected, state.getCurrentPlayer());
```

### Step 5: Run Full Test Suite
After each batch of changes, run `mvn test` to verify.

---

## Recommended Implementation Order

| Order | Task | Est. Time | Dependencies |
|-------|------|-----------|--------------|
| 1 | Create UNIT_TURN_TESTPLAN.md | 1 hour | None |
| 2 | Create RuleEngineUnitTurnTest.java | 2 hours | Task 1 |
| 3 | Modify applyEndTurn() | 1 hour | None |
| 4 | Add turn switch after actions | 2 hours | Task 3 |
| 5 | Add actingUnitId validation | 1 hour | Task 3 |
| 6 | Update HIGH impact tests | 4 hours | Tasks 3-5 |
| 7 | Update MEDIUM impact tests | 2 hours | Task 6 |
| 8 | Timer integration | 1 hour | Task 7 |
| 9 | Final verification | 1 hour | All |

**Total Estimated Time**: ~15 hours

---

## Questions for Claude CLI

Before starting implementation, Claude CLI should ask:

1. Should I create a feature flag for gradual migration?
2. Should I update tests in batches or all at once?
3. What's the priority: passing tests or complete feature?

---

## Success Metrics

- [ ] All existing tests pass (with updates)
- [ ] New unit-by-unit tests: 20+ tests
- [ ] Exhaustion Rule tests: 6+ tests  
- [ ] SPEED consecutive action tests: 4+ tests
- [ ] No regression in game logic
