# 5x5 Tactics Engine

## Project Overview
A 5x5 tactical board game featuring a game engine, WebSocket server, and web client.

## Tech Stack
- Java 17
- Maven 3.8+
- JUnit 5.10
- Jetty 11 (WebSocket)
- Vanilla JavaScript client

## Project Structure
```
├── docs/                    # Specification documents (must read)
├── src/main/java/com/tactics/
│   ├── engine/              # Game core (pure Java, no external dependencies)
│   │   ├── model/           # GameState, Unit, Board, Position
│   │   ├── action/          # Action, ActionType
│   │   ├── buff/            # BuffInstance, BuffModifier, BuffFlags
│   │   ├── rules/           # RuleEngine, ActionValidator, ActionExecutor
│   │   ├── skill/           # SkillExecutor, SkillDefinition, SkillRegistry
│   │   └── util/            # GameStateFactory, Serializer, RngProvider
│   └── server/              # WebSocket server
│       ├── core/            # Match, MatchService, MatchRegistry
│       ├── dto/             # Message objects
│       └── ws/              # WebSocket handlers
├── src/test/java/           # Tests
└── client/                  # Web frontend (HTML/CSS/JS)
```

## Common Commands
```bash
# Compile
mvn compile

# Run all tests
mvn test

# Run single test class
mvn test -Dtest=RuleEngineBuffFlowTest

# Run single test method
mvn test -Dtest=RuleEngineBuffFlowTest#testStunnedUnitCannotMove

# Package
mvn clean package

# Start server
java -jar target/tactics-engine-1.0-SNAPSHOT.jar
# or
mvn exec:java

# Client URL
http://localhost:8080/client/index.html
```

---

## ⚠️ VERSION NOTE

**V3 is the current development target.**

V1/V2 files are legacy reference. For new features, always refer to V3 documents.

---

## 🚧 Current Development Phase

| Status | Phase | Description |
|--------|-------|-------------|
| ✅ | Phase 0 | Complete V1 BUFF Foundation |
| ✅ | Phase 1 | Model Layer Extension |
| ✅ | Phase 2 | V3 BUFF System |
| ✅ | Phase 3 | Guardian Passive |
| ✅ | Phase 4 | Hero Skill System (4 sub-phases) |
| ✅ | **Code Health** | RuleEngine refactoring (SkillExecutor extraction) |
| 🔄 | **Phase 5** | Game Flow Extension |
| ⬜ | Phase 6 | Draft Phase |

### Phase 4 Sub-phases: Hero Skill System

| Status | Sub-phase | Description | Skills |
|--------|-----------|-------------|--------|
| ✅ | Phase 4A | Core framework + simple skills | Validation, cooldown, Endure, Spirit Hawk |
| ✅ | Phase 4B | Damage/heal skills | Elemental Blast, Trinity, Shockwave, Nature's Power, Power of Many |
| ✅ | Phase 4C | Movement skills | Heroic Leap, Smoke Bomb, Warp Beacon, Spectral Blades |
| ✅ | Phase 4D | Complex skills | Wild Magic, Elemental Strike, Death Mark, Ascended Form, Shadow Clone, Feint, Challenge |

**Current Task**: Phase 5 - Game Flow Extension

**Reference Documents**:
- `/docs/SKILL_SYSTEM_V3.md` - Full skill system specification
- `/docs/HERO_SKILLS_REFERENCE.md` - Quick skill reference (中英對照)
- `/docs/SKILL_SYSTEM_V3_TESTPLAN.md` - 201 test cases

---

## 📋 Code Health Refactoring (Completed)

**Goal**: Improve code maintainability by splitting RuleEngine.java (~3,300 lines) into focused classes.

### Full Refactoring Summary

| Component | Lines | Responsibility |
|-----------|-------|----------------|
| RuleEngine.java | 98 | Facade - delegates to specialized components |
| ActionValidator.java | 764 | All validation logic |
| ActionExecutor.java | 1,164 | All apply/execution logic |
| SkillExecutor.java | ~1,100 | All skill implementations |

**Before**: RuleEngine.java ~3,300 lines (monolithic)
**After**: RuleEngine.java 98 lines (clean facade) + 3 specialized classes

### Extraction History

1. **SkillExecutor Extraction** (Phase 4D completion)
   - Extracted all 19 skill implementations
   - RuleEngine reduced from ~3,300 to 2,316 lines

2. **ActionValidator Extraction**
   - Extracted all validation logic (validateAction, validateMove, validateAttack, etc.)
   - RuleEngine reduced from 2,316 to 1,608 lines

3. **ActionExecutor Extraction**
   - Extracted all apply logic (applyAction, applyMove, applyAttack, etc.)
   - RuleEngine reduced from 1,608 to 98 lines

**RuleEngine now acts as a clean facade**:
```java
public class RuleEngine {
    private final ActionValidator actionValidator;
    private final ActionExecutor actionExecutor;

    public ValidationResult validateAction(GameState state, Action action) {
        return actionValidator.validateAction(state, action);
    }

    public GameState applyAction(GameState state, Action action) {
        return actionExecutor.applyAction(state, action);
    }
}
```

**All 393 tests pass after refactoring.**

---

## 📋 Phase 4D Completed Items

| Status | Task | Description |
|--------|------|-------------|
| ✅ | Wild Magic | Mage: 1 damage to ALL enemies + 33% random debuff each |
| ✅ | Elemental Strike | Duelist: 3 damage + player-chosen debuff (BLEED/SLOW/WEAKNESS) |
| ✅ | Death Mark | Rogue: Mark target 2 rounds, +2 damage taken, heal on kill |
| ✅ | Ascended Form | Cleric: Invulnerable 1 round, 2x healing, cannot attack |
| ✅ | Shadow Clone | Rogue: Spawn 1HP/1ATK clone for 2 rounds |
| ✅ | Feint | Duelist: Dodge next attack, counter 2 damage |
| ✅ | Challenge | Duelist: Mark enemy, 50% damage to others, counter on attack |
| ✅ | Write tests | 14 Phase 4D tests added |
| ✅ | Run all tests | 393 tests passing |

### Phase 4C Completed Items
- ✅ Implemented Heroic Leap (Warrior: Leap to tile, 2 damage to adjacent enemies on landing)
- ✅ Implemented Smoke Bomb (Rogue: Teleport, invisible 1 round, blind adjacent enemies)
- ✅ Implemented Warp Beacon (Mage: Place beacon or teleport to existing beacon)
- ✅ Implemented Spectral Blades (Huntress: 1 damage to all enemies in a line, pierces)
- ✅ Added BuffType.BLIND (cannot attack for 1 round, duration 1)
- ✅ Added BuffFlags.blindBuff for BLIND buff tracking
- ✅ Added Unit.invisible field and related mechanics
- ✅ Added Unit.skillState map for beacon storage
- ✅ Added invisible expiry at round end
- ✅ Added invisible break on attack/skill use
- ✅ Added BLIND and INVISIBLE validation for ATTACK and MOVE_AND_ATTACK
- ✅ Added serialization for BuffFlags.blindBuff
- ✅ Added 26 Phase 4C tests (all passing, 374 total tests)

### Phase 4B Completed Items
- ✅ Implemented Elemental Blast (Mage: 3 damage, 50% random debuff)
- ✅ Implemented Trinity (Cleric: Heal 3 HP, remove debuff, apply LIFE)
- ✅ Implemented Shockwave (Warrior: 1 damage adjacent + knockback)
- ✅ Implemented Nature's Power (Huntress: +2 damage for 2 attacks, LIFE buff)
- ✅ Implemented Power of Many (Cleric: Heal all 1 HP, +1 ATK for 1 round)
- ✅ Added BuffFlags.lifeBuff for LIFE buff tracking
- ✅ Added Unit.bonusAttackDamage/bonusAttackCharges for Nature's Power
- ✅ Added serialization for new Unit fields (bonusAttackDamage, bonusAttackCharges)
- ✅ Added serialization for BuffFlags.lifeBuff
- ✅ Integrated Nature's Power bonus damage in applyAttack(), applyAttackObstacle(), applyMoveAndAttack()
- ✅ Added 31 Phase 4B tests (all passing, 379 total tests)

### Phase 4A Completed Items
- ✅ Created skill package (TargetType, SkillEffect, SkillDefinition, SkillRegistry)
- ✅ Implemented validateUseSkill() with all target type validations
- ✅ Implemented applyUseSkill() framework with skill dispatch
- ✅ Implemented cooldown decrement at round end
- ✅ Implemented Endure skill (3 shield, removes BLEED)
- ✅ Implemented Spirit Hawk skill (2 damage at range 4)
- ✅ Added 24 skill tests (all passing)

---

## ✅ Recently Implemented Rule Changes

These rules have been implemented:

| Rule | Status | Description |
|------|--------|-------------|
| Obstacle HP | ✅ | Obstacles have 3 HP, any unit can attack via ATTACK action |
| POWER instant destroy | ✅ | POWER buff destroys obstacles in 1 hit |
| Active player wins | ✅ | On simultaneous death, attacker wins |
| Exhaustion Rule | ✅ | Opponent takes consecutive turns when one side exhausted |
| Remove DESTROY_OBSTACLE | ✅ | Removed - use ATTACK on obstacles instead |
| Minion Decay | ✅ | Minions lose 1 HP per round at round end |
| Round 8 Pressure | ✅ | All units lose 1 HP per round after R8 |

---

## Core Documents — V3 (Current)

### Start Here
- `/docs/README.md` - Document index and reading order

### Game Rules (V3)
- `/docs/GAME_RULES_V3.md` - Core gameplay rules (hero, minions, victory)
- `/docs/GAME_FLOW_V3.md` - Complete game phases and flow
- `/docs/UNIT_TYPES_V3.md` - Hero & Minion definitions
- `/docs/BUFF_SYSTEM_V3.md` - 6 BUFF types (POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW)
- `/docs/SKILL_SYSTEM_V3.md` - Hero skill architecture (18 skills)
- `/docs/DESIGN_DECISIONS_V3.md` - Confirmed design decisions

### Quick Reference
- `/docs/V3_QUICK_REFERENCE.md` - Key numbers and stats
- `/docs/HERO_SKILLS_REFERENCE.md` - Skill quick reference (中英對照)

### Test Plans (V3)
- `/docs/BUFF_SYSTEM_V3_TESTPLAN.md` - 141 test cases for BUFF system
- `/docs/GUARDIAN_TESTPLAN.md` - 81 test cases for Guardian passive
- `/docs/SKILL_SYSTEM_V3_TESTPLAN.md` - 201 test cases for Skill system

### Development Roadmap
- `/docs/V3_IMPLEMENTATION_ROADMAP.md` - Phase-by-phase development checklist

---

## Legacy Documents — V1/V2 (Reference Only)

### Architecture & Skeletons
- `/docs/TECH_ARCH.md` - System architecture overview
- `/docs/ENGINE_SKELETON_V1.md` - Engine class skeleton
- `/docs/SERVER_SKELETON_V1.md` - Server class skeleton

### Game Rules (Legacy)
- `/docs/GAME_RULES_V1.md` - V1 basic rules
- `/docs/GAME_RULES_V2.md` - V2 range rules (moveRange/attackRange)
- `/docs/UNIT_TYPES_V1.md` - Unit type definitions
- `/docs/BUFF_SYSTEM_V1.md` - V1 Buff system specification

### Test Plans (Legacy)
- `/docs/RULEENGINE_TESTPLAN_V1.md` - RuleEngine tests
- `/docs/RULEENGINE_VALIDATE_V2_TESTPLAN.md` - V2 validation tests
- `/docs/BUFF_SYSTEM_V1_TESTPLAN.md` - V1 Buff system tests

### Protocol
- `/docs/WS_PROTOCOL_V1.md` - WebSocket message format

---

## Architecture Rules (Important!)

### Layering Principles
1. **ENGINE is fully self-contained** - `engine/` must not reference `server/` or `client/`
2. **SERVER operates through RuleEngine** - Must not directly mutate engine class state
3. **CLIENT depends only on WebSocket protocol** - Must not embed game rule logic

### Code Standards
- **Determinism** - All engine logic must be deterministic; randomness only via `RngProvider`
- **Immutability** - `GameState`, `Unit`, etc. are immutable; modifications require new instances
- **No unauthorized additions** - Do not add classes/methods/fields not defined in skeleton docs
- **Test verification** - All existing tests must pass after modifications

---

## 🔧 Development Guidelines for Claude CLI

### 1. Architecture Principles (最高優先)

#### 1.1 High Cohesion, Low Coupling (高內聚、低耦合)

**High Cohesion (高內聚)**:
- Each class should have ONE clear responsibility
- Related functionality should be grouped together
- If a class does multiple unrelated things, split it

```java
// ❌ BAD: Low cohesion - one class doing everything
class GameManager {
    void validateMove() { }
    void applyDamage() { }
    void sendWebSocket() { }
    void renderUI() { }
}

// ✅ GOOD: High cohesion - each class has one job
class RuleEngine { void validateAction(); void applyAction(); }
class DamageCalculator { int calculate(Unit attacker, Unit target); }
class WebSocketHandler { void sendMessage(); }
```

**Low Coupling (低耦合)**:
- Classes should depend on abstractions, not concrete implementations
- Minimize direct dependencies between modules
- Use dependency injection when possible

```java
// ❌ BAD: High coupling - direct instantiation
class MatchService {
    private RuleEngine engine = new RuleEngine();  // Tight coupling
}

// ✅ GOOD: Low coupling - dependency injection
class MatchService {
    private final RuleEngine engine;
    public MatchService(RuleEngine engine) {  // Injected
        this.engine = engine;
    }
}
```

#### 1.2 Layer Separation (分層架構)

```
┌─────────────────────────────────────────────────┐
│  CLIENT (client/)                               │  ← UI only, no game logic
├─────────────────────────────────────────────────┤
│  SERVER (server/)                               │  ← Orchestration, WebSocket
│  ├── ws/        → WebSocket handlers            │
│  ├── core/      → Match management              │
│  └── dto/       → Data transfer objects         │
├─────────────────────────────────────────────────┤
│  ENGINE (engine/)                               │  ← Pure game logic
│  ├── rules/     → RuleEngine, validation        │
│  ├── model/     → GameState, Unit, Position     │
│  ├── action/    → Action, ActionType            │
│  ├── buff/      → BuffInstance, BuffModifier    │
│  ├── skill/     → SkillExecutor, SkillRegistry  │
│  └── util/      → Factory, Serializer, Rng      │
└─────────────────────────────────────────────────┘
```

**Layer Rules**:
| From | Can Access | Cannot Access |
|------|------------|---------------|
| CLIENT | WebSocket only | SERVER, ENGINE |
| SERVER | ENGINE | CLIENT internals |
| ENGINE | Nothing external | SERVER, CLIENT |

#### 1.3 Modularization (模組化)

**Single Responsibility per File**:
```java
// ❌ BAD: Multiple responsibilities in one file
// BuffSystem.java containing BuffInstance, BuffModifier, BuffFactory, BuffValidator

// ✅ GOOD: One responsibility per file
buff/
├── BuffInstance.java      → Data model
├── BuffModifier.java      → Modifier calculations
├── BuffFactory.java       → Creation logic
├── BuffFlags.java         → Boolean flags
└── BuffValidator.java     → Validation rules
```

**Package by Feature**:
```java
// ❌ BAD: Package by layer only
model/
├── Unit.java
├── Buff.java
├── Skill.java

// ✅ GOOD: Package by feature (for complex features)
buff/
├── BuffInstance.java
├── BuffModifier.java
└── BuffFactory.java
skill/
├── SkillDefinition.java
├── SkillExecutor.java
└── SkillRegistry.java
```

#### 1.4 Dependency Direction

```
High-level (stable) ← Low-level (volatile)

RuleEngine ← SkillExecutor ← SkillRegistry
     ↑
GameState ← Unit ← Position
```

- High-level modules should NOT depend on low-level details
- Both should depend on abstractions
- Changes in low-level modules should NOT break high-level modules

#### 1.5 When to Extract New Classes

Extract a new class when:
- A class exceeds **500 lines** → Consider splitting
- A class exceeds **1000 lines** → Must split (like SkillExecutor extraction)
- A method exceeds **50 lines** → Extract helper methods or new class
- Multiple methods share common logic → Extract to helper class

**Example**: SkillExecutor was extracted from RuleEngine
- Before: RuleEngine ~3,300 lines
- After: RuleEngine ~2,300 lines + SkillExecutor ~1,100 lines

---

### 2. Code Style (程式碼風格)

#### 2.1 Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Class | PascalCase, noun | `GameState`, `RuleEngine`, `SkillExecutor` |
| Interface | PascalCase, adjective/noun | `Validatable`, `ActionHandler` |
| Method | camelCase, verb | `validateAction()`, `applyDamage()` |
| Variable | camelCase, descriptive | `targetUnit`, `buffDuration` |
| Constant | UPPER_SNAKE | `MAX_HP`, `BUFF_DURATION` |
| Package | lowercase | `com.tactics.engine.buff` |
| Test | ClassNameTest | `RuleEngineTest`, `SkillExecutorTest` |
| Enum | PascalCase (type), UPPER_SNAKE (values) | `BuffType.POWER`, `ActionType.ATTACK` |

#### 2.2 Method Design

**Keep Methods Small** (< 30 lines ideal, < 50 max):
```java
// ❌ BAD: 100+ line method doing everything
void processAction(Action action) {
    // validation logic...
    // damage calculation...
    // state update...
    // death processing...
    // victory check...
}

// ✅ GOOD: Small, focused methods
void processAction(Action action) {
    ValidationResult result = validateAction(action);
    if (!result.isValid()) return;
    
    applyAction(action);
    processDeaths();
    checkVictory();
}
```

**Method Parameter Limit**: Max 3-4 parameters
```java
// ❌ BAD: Too many parameters
void createUnit(String id, int hp, int atk, int move, int range, PlayerId owner, Position pos)

// ✅ GOOD: Use builder or parameter object
Unit unit = Unit.builder()
    .id(id)
    .hp(hp)
    .attack(atk)
    .position(pos)
    .build();
```

#### 2.3 Immutability

**Prefer Immutable Objects**:
```java
// ❌ BAD: Mutable state
class Unit {
    private int hp;
    public void setHp(int hp) { this.hp = hp; }
}

// ✅ GOOD: Immutable with copy-on-write
class Unit {
    private final int hp;
    public Unit withHp(int newHp) {
        return new Unit(this.id, newHp, this.attack, ...);
    }
}
```

#### 2.4 Error Handling

```java
// ❌ BAD: Silent failure
void applyBuff(Unit unit, BuffType type) {
    if (unit == null) return;  // Silent failure
}

// ✅ GOOD: Explicit validation result
ValidationResult applyBuff(Unit unit, BuffType type) {
    if (unit == null) {
        return ValidationResult.invalid("Unit cannot be null");
    }
    // ...
}
```

#### 2.5 Comments

```java
// ❌ BAD: Obvious comments
int hp = 5;  // Set hp to 5

// ✅ GOOD: Explain WHY, not WHAT
// Guardian cannot protect itself - damage goes directly to TANK
if (target.equals(guardian)) {
    return applyDirectDamage(target, damage);
}

// ✅ GOOD: Document complex business rules
/**
 * Wild Magic deals 1 damage to ALL enemies and has 33% chance
 * to apply a random debuff (BLEED, SLOW, or WEAKNESS) to each.
 * Uses RngProvider for deterministic randomness.
 */
GameState applyWildMagic(GameState state, Unit caster) { }
```

---

### 3. Testing Guidelines (測試規範)

#### 3.1 Test-Driven Development (TDD)

**Development Flow**:
```
1. Read test plan (e.g., GUARDIAN_TESTPLAN.md)
2. Write failing test first
3. Write minimal code to pass test
4. Refactor if needed
5. Repeat
```

**Before implementing any feature**:
1. Check if test plan exists in `docs/`
2. If yes → Follow the test plan
3. If no → Ask user or create test cases first

#### 3.2 Test Structure

**One Test Class per Production Class**:
```
src/main/java/com/tactics/engine/
├── rules/RuleEngine.java
├── skill/SkillExecutor.java

src/test/java/com/tactics/engine/
├── rules/RuleEngineTest.java
├── rules/RuleEngineGuardianTest.java  (feature-specific)
├── skill/SkillExecutorTest.java
```

**Test Naming Convention**:
```java
@Test
void testMethodName_scenario_expectedResult() { }

// Examples:
void testValidateAction_moveToOccupiedTile_returnsInvalid() { }
void testApplyDamage_guardianAdjacent_damageRedirected() { }
void testGetBuff_expiredDuration_returnsNull() { }
```

#### 3.3 Test Organization (AAA Pattern)

```java
@Test
void testGuardianInterceptsAttack() {
    // Arrange - Setup test state
    GameState state = createTestState();
    Unit tank = placeUnit(MinionType.TANK, pos(2, 2), PLAYER_1);
    Unit target = placeUnit(MinionType.ARCHER, pos(2, 3), PLAYER_1);
    Unit attacker = placeUnit(MinionType.ASSASSIN, pos(2, 4), PLAYER_2);
    
    // Act - Execute the action
    GameState result = ruleEngine.applyAction(
        Action.attack(attacker.getId(), target.getPosition(), target.getId()),
        state
    );
    
    // Assert - Verify results
    assertThat(getUnit(result, tank.getId()).getHp()).isEqualTo(originalTankHp - 1);
    assertThat(getUnit(result, target.getId()).getHp()).isEqualTo(originalTargetHp);
}
```

#### 3.4 Test Coverage Requirements

| Category | Minimum Coverage | Priority |
|----------|------------------|----------|
| RuleEngine | 90%+ | Critical |
| SkillExecutor | 90%+ | Critical |
| Model classes | 80%+ | High |
| Utility classes | 70%+ | Medium |
| Server/WebSocket | 60%+ | Lower |

**Must Test**:
- All validation rules (valid AND invalid cases)
- All state transitions
- Edge cases documented in test plans
- Error conditions
- Guardian interception scenarios
- Buff interactions

**Run Tests After Every Change**:
```bash
mvn test                           # All tests
mvn test -Dtest=ClassName          # Specific class
mvn test -Dtest=ClassName#method   # Specific method
```

#### 3.5 Test Independence

```java
// ❌ BAD: Tests depend on each other
@Test void test1_createUnit() { sharedUnit = new Unit(); }
@Test void test2_moveUnit() { sharedUnit.move(); }  // Depends on test1

// ✅ GOOD: Each test is independent
@Test void testCreateUnit() { Unit unit = new Unit(); ... }
@Test void testMoveUnit() { Unit unit = new Unit(); unit.move(); ... }
```

---

### 4. Code Review Checklist (For Claude CLI)

Before completing any task, verify:

#### Architecture
- [ ] No circular dependencies introduced
- [ ] Layer boundaries respected (ENGINE → SERVER → CLIENT)
- [ ] New code follows single responsibility principle
- [ ] High cohesion within classes
- [ ] Low coupling between classes
- [ ] Classes under 500 lines (or justified exception)

#### Code Quality
- [ ] Methods are small and focused (< 30 lines ideal)
- [ ] Naming is clear and consistent
- [ ] No magic numbers (use constants)
- [ ] Immutability preserved where required
- [ ] Error handling is explicit (ValidationResult)

#### Testing
- [ ] All new code has corresponding tests
- [ ] Tests follow AAA pattern
- [ ] Edge cases covered
- [ ] All existing tests still pass (`mvn test`)
- [ ] Test names are descriptive

#### Documentation
- [ ] Complex logic has comments explaining WHY
- [ ] Public methods have Javadoc for non-obvious behavior
- [ ] Changes align with spec documents in `docs/`

---

### 5. Refactoring Guidelines

#### When to Refactor
- Before adding new features to complex code
- When tests are passing but code is hard to read
- When a class exceeds size limits (500+ lines)
- When duplicate code appears 3+ times

#### Refactoring Checklist
1. Ensure all tests pass BEFORE refactoring
2. Make small, incremental changes
3. Run tests after EACH change
4. Do NOT change behavior during refactoring
5. Commit after each successful refactor step

#### Safe Refactoring Steps
```
1. Extract Method     → Break large methods into smaller ones
2. Extract Class      → Split large class into focused classes
3. Move Method        → Move method to more appropriate class
4. Rename             → Improve naming for clarity
5. Remove Duplication → Extract shared code to helper
```

---

## V3 Key Concepts

### Victory Condition
**Kill the enemy Hero = Win** (minion deaths don't end the game)

### Team Composition
- 1 Hero (based on SPD character)
- 2 Minions (drafted from TANK, ARCHER, ASSASSIN)

### New Mechanics (V3)
| Mechanic | Description |
|----------|-------------|
| Hero Skills | Each hero has 1 skill (cooldown: 2 rounds) |
| BUFF Tiles | Spawn on minion death, trigger on step |
| Obstacles | 3 HP, any unit can attack, POWER = instant destroy |
| Guardian | TANK protects adjacent friendly units |
| Minion Decay | Minions lose 1 HP per round |
| Round 8 Pressure | All units lose 1 HP per round after R8 |
| Exhaustion Rule | Opponent takes consecutive turns when one side has no units left |
| Simultaneous Death | Active player (attacker) wins |
| SLOW Buff | Actions delayed by 1 round |
| SPEED Buff | Grants 2 actions per round |

### 6 BUFF Types
| BUFF | ATK | HP | Special |
|------|-----|----|---------|
| POWER | +3 | +1 | Blocks MOVE_AND_ATTACK, **1-hit obstacle destroy** |
| LIFE | — | +3 | — |
| SPEED | -1 | — | Double action per round |
| WEAKNESS | -2 | -1 | — |
| BLEED | — | -1/round | Damage over time |
| SLOW | — | — | Actions delayed 1 round |

---

## Current Progress

See `/docs/PROGRESS.md` for details.

### Completed
- [x] V1 basic rules
- [x] V2 range rules (moveRange/attackRange)
- [x] WebSocket server
- [x] Web client
- [x] Unit types (SWORDSMAN, ARCHER, TANK)
- [x] V3 Model Layer (UnitCategory, MinionType, HeroClass, BuffType enums)
- [x] V3 BUFF System (6 types: POWER, LIFE, SPEED, WEAKNESS, BLEED, SLOW)
- [x] V3 SPEED buff (2 actions per turn)
- [x] V3 SLOW buff (delayed actions)
- [x] V3 Buff Tile Triggering
- [x] V3 Round Tracking
- [x] V3 Guardian Passive (TANK protects adjacent allies)
- [x] Obstacle HP system (3 HP, attackable via ATTACK)
- [x] POWER buff instant obstacle destroy
- [x] Simultaneous death: active player wins
- [x] Exhaustion Rule
- [x] Minion Decay (-1 HP/round)
- [x] Round 8 Pressure (-1 HP/round to all)
- [x] Removed DESTROY_OBSTACLE (use ATTACK instead)
- [x] Phase 4A: Skill framework (validation, cooldown, Endure, Spirit Hawk)
- [x] Phase 4B: Damage/heal skills (Elemental Blast, Trinity, Shockwave, Nature's Power, Power of Many)
- [x] Phase 4C: Movement skills (Heroic Leap, Smoke Bomb, Warp Beacon, Spectral Blades)
- [x] Phase 4D: Complex skills (Wild Magic, Elemental Strike, Death Mark, Ascended Form, Shadow Clone, Feint, Challenge)
- [x] Code Health: Full RuleEngine refactoring (reduced from ~3,300 to 98 lines via ActionValidator, ActionExecutor, SkillExecutor extraction)

### In Progress
- [ ] Phase 5: Game Flow Extension

---

## Test Coverage

**Total: 393 tests passing**

### Existing Tests
| Test Class | Coverage |
|------------|----------|
| RuleEngineValidateActionTest | V1 validation logic |
| RuleEngineApplyActionTest | Action execution logic |
| RuleEngineValidateActionV2Test | V2 range validation |
| RuleEngineBuffFlowTest | V1 Buff system |
| MatchServiceTest | Game flow |
| GameStateSerializerTest | JSON serialization |

### V3 Tests (Implemented)
| Test Class | Coverage | Tests |
|------------|----------|-------|
| BuffFactoryTest | BM-Series: BuffType, modifiers | 12 |
| BuffTileTest | BT-Series: Tile trigger, instant HP | 10 |
| RuleEngineSpeedBuffTest | BSP-Series: 2 actions, tracking | 7 |
| RuleEngineSlowBuffTest | BSL-Series: Preparing state | 7 |
| RuleEngineGuardianTest | Guardian passive intercept | 16 |
| RuleEngineAttritionTest | Minion decay, Round 8 pressure | 11 |
| RuleEngineSkillTest | Skill framework, Endure, Spirit Hawk | 24 |
| RuleEngineSkillPhase4BTest | Phase 4B skills: damage/heal, bonus damage | 31 |
| RuleEngineSkillPhase4CTest | Phase 4C skills: movement, BLIND, invisible | 26 |
| RuleEngineSkillPhase4DTest | Phase 4D skills: complex skills, buff types | 14 |

### V3 Tests (To Be Implemented)
| Test Plan | Test Count |
|-----------|------------|
| SKILL_SYSTEM_V3_TESTPLAN.md | ~150 remaining |
| Remaining BUFF tests | ~100 |

---

## AI Development Guidelines

See `/prompt/CLAUDE.md` for detailed system prompt rules.

Key principles:
- **Must read relevant V3 specification documents before modifying code**
- Do not invent rules or fields not defined in documents
- Do not violate layering architecture
- Must run tests to verify after modifications
- For V3 features, always reference V3 documents (not V1/V2)
- **Follow Development Guidelines above (高內聚、低耦合)**
- **Use TDD: Write tests first, then implement**
- **Keep classes under 500 lines, methods under 30 lines**
