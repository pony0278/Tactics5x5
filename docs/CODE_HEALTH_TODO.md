# Code Health TODO

This document tracks code health issues identified during code review. Items are prioritized by severity and impact.

**Last Updated**: 2025-12-07
**Total Tests**: 692 passing
**Next Review**: After ActionExecutor split

---

## Quick Wins (< 30 minutes each)

These items provide high impact with low effort. Do these first!

| Task | Impact | Time | Dependencies | Status |
|------|--------|------|--------------|--------|
| Create RuleEngineHelper.java | Removes 4 duplicate methods | 30 min | None | ✅ Done |
| Split SkillRegistry by hero | Better organization | 20 min | None | ✅ Done |
| Extract JsonHelper type parsers | Cleaner code | 30 min | None | ✅ Done |

---

## Priority 1 - Critical (Should Fix Soon)

### 1.1 ActionExecutor.java - 1,419 lines

**Status**: ⚠️ Needs Split  
**Location**: `src/main/java/com/tactics/engine/rules/ActionExecutor.java`  
**Estimated Time**: 2-3 hours  
**Dependencies**: RuleEngineHelper.java (Quick Win)

**Problem**: File exceeds 1,000 line limit significantly. Contains too many responsibilities.

**Suggested Split**:

| New Class | Responsibility | Methods to Extract | Est. Lines |
|-----------|----------------|-------------------|------------|
| `AttackExecutor.java` | Attack action logic | `applyAttack()`, `applyAttackObstacle()`, `applyMoveAndAttack()` | ~300 |
| `MoveExecutor.java` | Move action logic | `applyMove()`, buff tile triggering | ~150 |
| `TurnManager.java` | Turn/round management | `processTurnEnd()`, `processRoundEndAfterAction()`, `getNextActingPlayer()` | ~200 |
| `GameOverChecker.java` | Victory condition logic | `checkGameOver()`, `checkMinionDeath()` | ~150 |

**Large Methods to Split**:

| Method | Lines | Split Into |
|--------|-------|------------|
| `applyAttack()` | 102 | `calculateDamage()`, `applyDamageToTarget()`, `handleGuardian()` |
| `applyMoveAndAttack()` | 100 | Reuse `applyMove()` + `applyAttack()` |
| `executeOnePreparingAction()` | 105 | `executePreparedMove()`, `executePreparedAttack()`, `executePreparedSkill()` |
| `processTurnEnd()` | 84 | `processBuffDecay()`, `processBleedDamage()`, `checkRoundEnd()` |
| `applyMove()` | 73 | `validateMoveTarget()`, `triggerBuffTile()`, `updateUnitPosition()` |
| `checkGameOver()` | 71 | `checkHeroDeath()`, `determineWinner()` |

**Affected Tests** (run after refactoring):
- `RuleEngineApplyActionTest.java`
- `RuleEngineUnitTurnTest.java`
- `RuleEngineExhaustionTest.java`
- `RuleEngineSkillTest.java` (all phases)
- `RuleEngineGuardianTest.java`
- `RuleEngineAttritionTest.java`

**Acceptance Criteria**:
- [ ] ActionExecutor.java < 500 lines
- [ ] Each new class < 400 lines
- [ ] No method > 50 lines
- [ ] All 655 tests pass
- [ ] No duplicate code between new classes

---

### 1.2 SkillExecutor.java - 1,127 lines

**Status**: ⚠️ Needs Split  
**Location**: `src/main/java/com/tactics/engine/skill/SkillExecutor.java`  
**Estimated Time**: 2 hours  
**Dependencies**: None

**Problem**: File exceeds 1,000 line limit. All 19 skill implementations in one file.

**Suggested Split by Hero Class**:

| New Class | Hero | Skills | Est. Lines |
|-----------|------|--------|------------|
| `WarriorSkillExecutor.java` | Warrior | Endure, Shockwave, Heroic Leap | ~200 |
| `MageSkillExecutor.java` | Mage | Elemental Blast, Wild Magic, Warp Beacon | ~200 |
| `RogueSkillExecutor.java` | Rogue | Smoke Bomb, Death Mark, Shadow Clone | ~200 |
| `ClericSkillExecutor.java` | Cleric | Trinity, Power of Many, Ascended Form | ~180 |
| `HuntressSkillExecutor.java` | Huntress | Spirit Hawk, Nature's Power, Spectral Blades | ~180 |
| `DuelistSkillExecutor.java` | Duelist | Elemental Strike, Feint, Challenge | ~180 |

**Alternative**: Split by skill category:

| New Class | Category | Skills |
|-----------|----------|--------|
| `DamageSkillExecutor.java` | Damage | Spirit Hawk, Elemental Blast, Wild Magic, Spectral Blades |
| `MovementSkillExecutor.java` | Movement | Heroic Leap, Smoke Bomb, Warp Beacon |
| `BuffSkillExecutor.java` | Buff/Heal | Endure, Trinity, Nature's Power, Power of Many, Ascended Form |
| `DebuffSkillExecutor.java` | Debuff | Death Mark, Elemental Strike, Shockwave |
| `SpecialSkillExecutor.java` | Special | Shadow Clone, Feint, Challenge |

**Large Methods to Split**:

| Method | Lines | Split Into |
|--------|-------|------------|
| `applySkillShockwave()` | 66 | `dealShockwaveDamage()`, `applyKnockback()` |
| `applySkillWildMagic()` | 62 | `dealWildMagicDamage()`, `applyRandomDebuffs()` |
| `executeSkill()` | 51 | Keep as dispatcher, but use strategy pattern |

**Affected Tests**:
- `RuleEngineSkillTest.java`
- `RuleEngineSkillPhase4BTest.java`
- `RuleEngineSkillPhase4CTest.java`
- `RuleEngineSkillPhase4DTest.java`

**Acceptance Criteria**:
- [ ] SkillExecutor.java < 300 lines (dispatcher only)
- [ ] Each hero executor < 250 lines
- [ ] No method > 50 lines
- [ ] All 655 tests pass
- [ ] Skills easily findable by hero class

---

### 1.3 JsonHelper.parseValue() - REFACTORED

**Status**: ✅ RESOLVED
**Completed**: 2025-12-07
**Solution**: Extracted type-specific parsing methods

**Original Problem**: Single method handling all JSON parsing (was ~80 lines, not 325 as noted).

**Suggested Split**:

```java
public class JsonHelper {
    public static Object parseValue(String json, int[] index) {
        char c = skipWhitespace(json, index);
        switch (c) {
            case '"': return parseString(json, index);
            case '{': return parseObject(json, index);
            case '[': return parseArray(json, index);
            case 't': case 'f': return parseBoolean(json, index);
            case 'n': return parseNull(json, index);
            default: return parseNumber(json, index);
        }
    }
    
    private static String parseString(String json, int[] index) { ... }
    private static Map<String, Object> parseObject(String json, int[] index) { ... }
    private static List<Object> parseArray(String json, int[] index) { ... }
    private static Boolean parseBoolean(String json, int[] index) { ... }
    private static Object parseNull(String json, int[] index) { ... }
    private static Number parseNumber(String json, int[] index) { ... }
}
```

**Affected Tests**:
- `JsonHelperTest.java`
- `GameStateSerializerTest.java`

**Acceptance Criteria**:
- [ ] parseValue() < 30 lines
- [ ] Each parse method < 50 lines
- [ ] All serialization tests pass
- [ ] No behavior change

---

### 1.4 SkillRegistry.initializeSkills() - REFACTORED

**Status**: ✅ RESOLVED
**Completed**: 2025-12-07
**Solution**: Split into 6 hero-specific registration methods

**Original Problem**: Single method registering all 19 skills with inline definitions.

**Suggested Split**:

```java
public class SkillRegistry {
    private static final Map<SkillId, SkillDefinition> SKILLS = new HashMap<>();
    
    static {
        registerWarriorSkills();
        registerMageSkills();
        registerRogueSkills();
        registerClericSkills();
        registerHuntressSkills();
        registerDuelistSkills();
    }
    
    private static void registerWarriorSkills() {
        register(SkillId.ENDURE, SkillDefinition.builder()...);
        register(SkillId.SHOCKWAVE, SkillDefinition.builder()...);
        register(SkillId.HEROIC_LEAP, SkillDefinition.builder()...);
    }
    // ... other hero registration methods
}
```

**Affected Tests**:
- `SkillRegistryTest.java` (if exists)
- `RuleEngineSkillTest.java`

**Acceptance Criteria**:
- [ ] initializeSkills() removed or < 20 lines
- [ ] Each hero registration method < 50 lines
- [ ] Skills easily findable by hero class
- [ ] All skill tests pass

---

## Priority 2 - High (Should Fix)

### 2.1 Duplicate Helper Methods

**Status**: ✅ RESOLVED
**Completed**: 2025-12-07
**Solution**: Created RuleEngineHelper.java

**Problem**: The following methods are duplicated between `ActionValidator.java` and `ActionExecutor.java`:

| Method | Purpose | Occurrences |
|--------|---------|-------------|
| `findUnitById()` | Find unit by ID in list | 2 |
| `getBuffsForUnit()` | Get buffs for a unit | 2 |
| `hasSpeedBuff()` | Check for SPEED buff | 2 |
| `hasPowerBuff()` | Check for POWER buff | 2 |

**Solution**: Create `RuleEngineHelper.java`:

```java
package com.tactics.engine.rules;

import com.tactics.engine.model.*;
import com.tactics.engine.buff.BuffInstance;
import java.util.*;

public class RuleEngineHelper {
    
    public static Unit findUnitById(List<Unit> units, String unitId) {
        return units.stream()
            .filter(u -> u.getId().equals(unitId))
            .findFirst()
            .orElse(null);
    }
    
    public static List<BuffInstance> getBuffsForUnit(GameState state, String unitId) {
        return state.getBuffs().stream()
            .filter(b -> b.getTargetUnitId().equals(unitId))
            .collect(Collectors.toList());
    }
    
    public static boolean hasSpeedBuff(List<BuffInstance> buffs) {
        return buffs.stream().anyMatch(b -> b.getType() == BuffType.SPEED);
    }
    
    public static boolean hasPowerBuff(List<BuffInstance> buffs) {
        return buffs.stream().anyMatch(b -> b.getType() == BuffType.POWER);
    }
    
    public static boolean hasSlowBuff(List<BuffInstance> buffs) {
        return buffs.stream().anyMatch(b -> b.getType() == BuffType.SLOW);
    }
    
    public static boolean hasWeaknessBuff(List<BuffInstance> buffs) {
        return buffs.stream().anyMatch(b -> b.getType() == BuffType.WEAKNESS);
    }
    
    public static int getMaxActionsForUnit(List<BuffInstance> buffs) {
        return hasSpeedBuff(buffs) ? 2 : 1;
    }
}
```

**Affected Tests**: All tests should pass without changes (internal refactoring)

**Acceptance Criteria**:
- [ ] RuleEngineHelper.java created
- [ ] ActionValidator uses RuleEngineHelper
- [ ] ActionExecutor uses RuleEngineHelper
- [ ] No duplicate helper methods
- [ ] All 655 tests pass

---

### 2.2 ActionValidator.java - Large Methods

**Status**: ⚠️ Needs Split  
**Location**: `src/main/java/com/tactics/engine/rules/ActionValidator.java`  
**Estimated Time**: 1.5 hours  
**Dependencies**: RuleEngineHelper.java

**Methods to Split**:

| Method | Lines | Split Into |
|--------|-------|------------|
| `validateAttack()` | 123 | `validateAttackTarget()`, `validateAttackRange()`, `validateAttackerState()`, `checkGuardianIntercept()` |
| `validateMoveAndAttack()` | 104 | `validateMovePhase()`, `validateAttackPhase()` (reuse existing methods) |
| `validateMove()` | 89 | `validateMoveRange()`, `validateMoveTarget()`, `validateMoverState()` |
| `validateUseSkill()` | 57 | `validateSkillCooldown()`, `validateSkillTarget()`, `validateSkillRange()` |

**Acceptance Criteria**:
- [ ] No method > 50 lines
- [ ] Clear single responsibility per method
- [ ] All validation tests pass
- [ ] Error messages unchanged

---

### 2.3 MatchWebSocketHandler.handleJoinMatch() - 61 lines

**Status**: ⚠️ Needs Split  
**Location**: `src/main/java/com/tactics/server/ws/MatchWebSocketHandler.java:202`  
**Estimated Time**: 30 minutes  
**Dependencies**: None

**Problem**: Method handles multiple concerns.

**Suggested Split**:

```java
private void handleJoinMatch(Session session, Map<String, Object> data) {
    JoinRequest request = validateJoinRequest(data);
    if (request == null) return;
    
    PlayerAssignment assignment = assignPlayerToMatch(session, request);
    if (assignment == null) return;
    
    sendJoinConfirmation(session, assignment);
    
    if (assignment.isGameReady()) {
        broadcastGameStart(assignment.getMatch());
    }
}
```

**Acceptance Criteria**:
- [ ] handleJoinMatch() < 20 lines
- [ ] Each helper method < 30 lines
- [ ] WebSocket tests pass
- [ ] Join flow unchanged

---

## Priority 3 - Monitor

### 3.1 Files 500-900 Lines (Monitor)

These files are approaching the limit and should be monitored:

| File | Lines | Status | Action Trigger |
|------|-------|--------|----------------|
| `ActionValidator.java` | 865 | 🟡 Monitor | Split if > 1000 |
| `GameStateSerializer.java` | 787 | 🟡 Monitor | Split if > 1000 |
| `Unit.java` | 684 | 🟢 OK | Mostly constructors/getters |
| `MatchService.java` | 529 | 🟢 OK | Timer integration complete |

### 3.2 Methods 30-50 Lines (Monitor)

These methods are at the upper limit but acceptable:

| Method | Lines | File | Notes |
|--------|-------|------|-------|
| `createMinion()` | 49 | DraftSetupService.java | Complex but focused |
| `deserializeUnit()` | 48 | GameStateSerializer.java | Many fields |
| `handleActionTimeout()` | 48 | MatchService.java | Timer logic |
| `main()` | 47 | Main.java | Server setup |
| `handleAction()` | 47 | MatchWebSocketHandler.java | Dispatcher |
| `applySkillTrinity()` | 45 | SkillExecutor.java | Complex skill |

### 3.3 Test Files (Acceptable)

Large test files are acceptable when well-organized:

| File | Lines | Tests | Status |
|------|-------|-------|--------|
| `GameStateSerializerTest.java` | 1,360 | Many | Consider splitting by category |
| `MatchServiceTimerTest.java` | 993 | Many | Well-organized by timer type |
| `RuleEngineBuffFlowTest.java` | 973 | 35 | Well-organized with @Nested |
| `RuleEngineSkillPhase4BTest.java` | 912 | 31 | Phase-specific, acceptable |
| `MatchWebSocketHandlerTest.java` | 890 | Many | Uses @Nested, well-organized |

---

## Dependencies Graph

```
┌─────────────────────────────────────────────────────────────┐
│                    Refactoring Order                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Quick Wins (Do First - No Dependencies)                    │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │ RuleEngineHelper│  │ JsonHelper      │                  │
│  │ (30 min)        │  │ (1 hour)        │                  │
│  └────────┬────────┘  └─────────────────┘                  │
│           │                                                 │
│           ▼                                                 │
│  Priority 1 (After Quick Wins)                              │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │ ActionExecutor  │  │ SkillExecutor   │                  │
│  │ Split (2-3 hr)  │  │ Split (2 hr)    │                  │
│  └────────┬────────┘  └─────────────────┘                  │
│           │                                                 │
│           ▼                                                 │
│  Priority 2 (After ActionExecutor)                          │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │ ActionValidator │  │ SkillRegistry   │                  │
│  │ Methods (1.5 hr)│  │ Split (1 hr)    │                  │
│  └─────────────────┘  └─────────────────┘                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Refactoring Schedule

### Suggested Order (Total: ~10 hours)

| Order | Task | Time | Cumulative |
|-------|------|------|------------|
| 1 | RuleEngineHelper.java (Quick Win) | 30 min | 30 min |
| 2 | JsonHelper.parseValue() | 1 hour | 1.5 hours |
| 3 | SkillRegistry.initializeSkills() | 1 hour | 2.5 hours |
| 4 | ActionExecutor split | 2-3 hours | 5 hours |
| 5 | SkillExecutor split | 2 hours | 7 hours |
| 6 | ActionValidator methods | 1.5 hours | 8.5 hours |
| 7 | MatchWebSocketHandler | 30 min | 9 hours |
| 8 | Final verification | 1 hour | 10 hours |

---

## Refactoring Guidelines

### Before Refactoring
1. ✅ Ensure all 655 tests pass
2. ✅ Create a branch for the refactoring work
3. ✅ Read relevant test files to understand expected behavior
4. ✅ Identify affected tests for each change

### During Refactoring
1. Make small, incremental changes
2. Run tests after each change
3. Commit after each successful step
4. Do NOT change behavior during refactoring

### Safe Refactoring Steps
1. **Extract Method** - Break large methods into smaller ones
2. **Extract Class** - Split large class into focused classes
3. **Move Method** - Move method to more appropriate class
4. **Extract Helper** - Consolidate duplicate code into utilities

### Post-Refactoring Checklist
- [ ] All 655 tests pass
- [ ] No new warnings introduced
- [ ] Code coverage maintained
- [ ] Performance not degraded
- [ ] Documentation updated

---

## Progress Tracking

| Item | Priority | Est. Time | Status | Date | Notes |
|------|----------|-----------|--------|------|-------|
| RuleEngineHelper.java | Quick Win | 30 min | ✅ Done | 2025-12-07 | Extracted 4 duplicate methods |
| JsonHelper refactor | P1 | 30 min | ✅ Done | 2025-12-07 | Extracted 8 type-specific methods |
| SkillRegistry refactor | Quick Win | 20 min | ✅ Done | 2025-12-07 | Split into 6 hero-specific methods |
| ActionExecutor split | P1 | 2-3 hours | ⬜ Not Started | - | - |
| SkillExecutor split | P1 | 2 hours | ⬜ Not Started | - | - |
| ActionValidator methods | P2 | 1.5 hours | ⬜ Not Started | - | - |
| MatchWebSocketHandler | P2 | 30 min | ⬜ Not Started | - | - |

---

## Metrics

### Current State
| Metric | Value | Target |
|--------|-------|--------|
| Files > 1000 lines | 2 | 0 |
| Methods > 100 lines | 4 | 0 |
| Methods > 50 lines | 14 | 0 |
| Duplicate methods | 0 | 0 |
| Total tests | 692 | Maintain |

### Target State (After Refactoring)
| Metric | Value |
|--------|-------|
| Files > 1000 lines | 0 |
| Methods > 100 lines | 0 |
| Methods > 50 lines | 0 |
| Duplicate methods | 0 |
| Total tests | 692+ |

---

*Last updated: 2025-12-07*
*Total tests: 692 passing*
*Next scheduled refactoring: ActionExecutor split*
