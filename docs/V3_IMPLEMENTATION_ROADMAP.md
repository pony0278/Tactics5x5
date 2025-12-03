# V3_IMPLEMENTATION_ROADMAP.md — Core Engine Development Checklist

## Current Status

| Component | Status |
|-----------|--------|
| V1 Basic Rules (move, attack, turn) | ✅ Complete |
| V2 Range Rules (moveRange/attackRange) | ✅ Complete |
| V1 BUFF System (STUN, ROOT, POISON) | 🔄 Tests done, implementation in progress |
| WebSocket Server | ✅ Complete |
| Existing Tests | ✅ 235 passing |

---

## V3 Core Engine — Development Phases

```
Phase 0: Complete V1 BUFF Foundation    [Prerequisite]
    ↓
Phase 1: Model Layer Extension          [Foundation]
    ↓
Phase 2: V3 BUFF System                 [Core Mechanic]
    ↓
Phase 3: Unit Passive Abilities         [Combat Mechanic]
    ↓
Phase 4: Hero Skill System              [Core Gameplay]
    ↓
Phase 5: Game Flow Extension            [Complete Game]
    ↓
Phase 6: Draft Phase                    [Pre-game Selection]
```

---

## Phase 0: Complete V1 BUFF Foundation (Prerequisite)

**Goal**: Ensure V1 BUFF system works correctly as foundation for V3

| # | Task | Complexity | Dependency | Tests |
|---|------|------------|------------|-------|
| 0.1 | Complete RuleEngine BUFF integration | Medium | — | RuleEngineBuffFlowTest |
| 0.2 | STUN/ROOT validation logic | Low | 0.1 | BV1-BV7 |
| 0.3 | bonusMoveRange/bonusAttackRange | Low | 0.1 | BV8-BV12 |
| 0.4 | bonusAttack damage calculation | Low | 0.1 | BA1-BA4 |
| 0.5 | Round-end BUFF processing | Medium | 0.1 | BL1-BL7 |
| 0.6 | POISON damage | Low | 0.5 | BP1-BP5 |

**Completion Criteria**: `mvn test` all pass, especially `RuleEngineBuffFlowTest`

**Estimated Time**: 2-4 hours

---

## Phase 1: Model Layer Extension (Foundation)

**Goal**: Extend data models to support V3 features

### 1.1 Unit Model Extension

| # | Task | Complexity | File |
|---|------|------------|------|
| 1.1.1 | Add `UnitCategory` enum (HERO, MINION) | Low | model/UnitCategory.java |
| 1.1.2 | Add `MinionType` enum (TANK, ARCHER, ASSASSIN) | Low | model/MinionType.java |
| 1.1.3 | Add `HeroClass` enum (6 classes) | Low | model/HeroClass.java |
| 1.1.4 | Add fields to Unit class | Medium | model/Unit.java |
| 1.1.5 | Update GameStateFactory | Medium | util/GameStateFactory.java |

**New Unit Fields**:
```java
UnitCategory category;      // HERO or MINION
MinionType minionType;      // null if HERO
HeroClass heroClass;        // null if MINION
String selectedSkillId;     // Hero only
int skillCooldown;          // Hero only
int shield;                 // Endure skill
boolean invisible;          // Smoke Bomb
boolean invulnerable;       // Ascended Form
boolean isTemporary;        // Shadow Clone
int temporaryDuration;      // Rounds remaining
Map<String, Object> skillState;  // Warp Beacon position, etc.
```

### 1.2 BUFF Model Extension

| # | Task | Complexity | File |
|---|------|------------|------|
| 1.2.1 | Add `BuffType` enum (6 BUFF types) | Low | buff/BuffType.java |
| 1.2.2 | Extend BuffFlags | Low | buff/BuffFlags.java |
| 1.2.3 | Add BuffFactory | Low | buff/BuffFactory.java |

**BuffType Enum**:
```java
enum BuffType {
    POWER, LIFE, SPEED,      // Positive
    WEAKNESS, BLEED, SLOW    // Negative
}
```

### 1.3 Map Element Models

| # | Task | Complexity | File |
|---|------|------------|------|
| 1.3.1 | Add `BuffTile` class | Low | model/BuffTile.java |
| 1.3.2 | Add `Obstacle` class | Low | model/Obstacle.java |
| 1.3.3 | Add fields to GameState | Medium | model/GameState.java |

**New GameState Fields**:
```java
List<BuffTile> buffTiles;
List<Obstacle> obstacles;
int currentRound;
DeathChoice pendingDeathChoice;  // Awaiting player choice
```

### 1.4 Action Type Extension

| # | Task | Complexity | File |
|---|------|------------|------|
| 1.4.1 | Add USE_SKILL to ActionType | Low | action/ActionType.java |
| 1.4.2 | Add DESTROY_OBSTACLE to ActionType | Low | action/ActionType.java |
| 1.4.3 | Add DEATH_CHOICE to ActionType | Low | action/ActionType.java |
| 1.4.4 | Extend Action class | Low | action/Action.java |

### 1.5 Serialization Update

| # | Task | Complexity | File |
|---|------|------------|------|
| 1.5.1 | Update GameStateSerializer | Medium | util/GameStateSerializer.java |
| 1.5.2 | Serialization tests | Medium | GameStateSerializerTest.java |

**Estimated Time**: 4-6 hours

---

## Phase 2: V3 BUFF System (Core Mechanic)

**Goal**: Implement complete behavior for 6 V3 BUFF types

### 2.1 Basic BUFF Effects

| # | Task | Complexity | Test Series |
|---|------|------------|-------------|
| 2.1.1 | POWER: +3 ATK, +1 HP | Low | BM5, BA1 |
| 2.1.2 | LIFE: +3 HP (instant) | Low | BM6, BL5 |
| 2.1.3 | WEAKNESS: -2 ATK, -1 HP | Low | BM8, BL7 |
| 2.1.4 | BLEED: -1 HP per round | Medium | BBL1-BBL8 |

### 2.2 Special BUFF Effects

| # | Task | Complexity | Test Series |
|---|------|------------|-------------|
| 2.2.1 | POWER: Block MOVE_AND_ATTACK | Medium | BP1-BP3, BV2 |
| 2.2.2 | POWER: Enable DESTROY_OBSTACLE | Medium | BP5-BP8 |
| 2.2.3 | SPEED: Double action system | High | BSP1-BSP10 |
| 2.2.4 | SLOW: Preparing state system | High | BSL1-BSL12 |

### 2.3 BUFF Tile System

| # | Task | Complexity | Test Series |
|---|------|------------|-------------|
| 2.3.1 | Death choice mechanism | Medium | BT1, BT11, BT14 |
| 2.3.2 | BUFF tile generation | Low | BT2-BT4 |
| 2.3.3 | BUFF tile trigger | Medium | BT5-BT8 |
| 2.3.4 | BUFF tile expiration | Low | BT9-BT10 |
| 2.3.5 | Obstacle system | Medium | BT11-BT13 |

### 2.4 BUFF Stacking Rules

| # | Task | Complexity | Test Series |
|---|------|------------|-------------|
| 2.4.1 | Same type refresh | Low | BST1 |
| 2.4.2 | Different type stacking | Low | BST2-BST3 |
| 2.4.3 | SPEED + SLOW cancellation | Medium | BST5, BV19 |

**Estimated Time**: 8-12 hours

---

## Phase 3: Unit Passive Abilities (Combat Mechanic)

**Goal**: Implement minion passive abilities

### 3.1 TANK Guardian Passive

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 3.1.1 | Normal attack interception | High | Custom |
| 3.1.2 | Skill damage interception | High | SG1-SG10 |
| 3.1.3 | Multiple TANK priority | Medium | SG10 |
| 3.1.4 | Dead TANK cannot intercept | Low | SG7 |

### 3.2 ARCHER/ASSASSIN Passives

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 3.2.1 | ARCHER: attackRange = 3 | Low | Already in V2 |
| 3.2.2 | ASSASSIN: moveRange = 4 | Low | Already in V2 |

**Estimated Time**: 4-6 hours

---

## Phase 4: Hero Skill System (Core Gameplay)

**Goal**: Implement 18 hero skills

### 4.1 Skill Infrastructure

| # | Task | Complexity | Test Series |
|---|------|------------|-------------|
| 4.1.1 | SkillDefinition class | Low | SM1-SM15 |
| 4.1.2 | SkillEffect class | Low | SM6-SM7 |
| 4.1.3 | SkillRegistry | Medium | SM8-SM10 |
| 4.1.4 | Skill cooldown system | Medium | SC1-SC12 |
| 4.1.5 | USE_SKILL validation | Medium | SV1-SV18 |
| 4.1.6 | USE_SKILL execution | Medium | SA1-SA14 |

### 4.2 WARRIOR Skills (Priority)

| # | Skill | Complexity | Test Series |
|---|-------|------------|-------------|
| 4.2.1 | Heroic Leap | Medium | SW1-SW4 |
| 4.2.2 | Shockwave | Medium | SW5-SW8 |
| 4.2.3 | Endure | Medium | SW9-SW12 |

### 4.3 MAGE Skills

| # | Skill | Complexity | Test Series |
|---|-------|------------|-------------|
| 4.3.1 | Elemental Blast | Low | SMG1-SMG4 |
| 4.3.2 | Warp Beacon | High | SMG5-SMG10, SSP1-SSP5 |
| 4.3.3 | Wild Magic | Medium | SMG11-SMG14 |

### 4.4 ROGUE Skills

| # | Skill | Complexity | Test Series |
|---|-------|------------|-------------|
| 4.4.1 | Smoke Bomb | Medium | SR1-SR5 |
| 4.4.2 | Death Mark | Medium | SR6-SR8 |
| 4.4.3 | Shadow Clone | High | SR9-SR14, SSP6-SSP9 |

### 4.5 HUNTRESS Skills

| # | Skill | Complexity | Test Series |
|---|-------|------------|-------------|
| 4.5.1 | Spirit Hawk | Low | SH1-SH2 |
| 4.5.2 | Spectral Blades | Medium | SH3-SH7 |
| 4.5.3 | Nature's Power | Medium | SH8-SH12, SSP16-SSP17 |

### 4.6 DUELIST Skills

| # | Skill | Complexity | Test Series |
|---|-------|------------|-------------|
| 4.6.1 | Challenge | High | SD1-SD6, SSP13-SSP15 |
| 4.6.2 | Elemental Strike | Low | SD7-SD9 |
| 4.6.3 | Feint | High | SD10-SD14, SSP10-SSP12 |

### 4.7 CLERIC Skills

| # | Skill | Complexity | Test Series |
|---|-------|------------|-------------|
| 4.7.1 | Trinity | Medium | SCL1-SCL5 |
| 4.7.2 | Power of Many | Low | SCL6-SCL8 |
| 4.7.3 | Ascended Form | Medium | SCL9-SCL12, SSP18 |

### 4.8 Skill Interactions

| # | Task | Complexity | Test Series |
|---|------|------------|-------------|
| 4.8.1 | Skill + BUFF interaction | Medium | SB1-SB16 |
| 4.8.2 | Skill + Guardian | Medium | SG1-SG10 |

**Estimated Time**: 16-24 hours

---

## Phase 5: Game Flow Extension (Complete Game)

**Goal**: Implement V3-specific game rules

### 5.1 Victory Conditions

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 5.1.1 | Hero death = game over | Low | Custom |
| 5.1.2 | Simultaneous death resolution | Medium | Custom |
| 5.1.3 | Minion death doesn't end game | Low | Custom |

### 5.2 Decay System

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 5.2.1 | Minion -1 HP per round | Low | Custom |
| 5.2.2 | Round 8+ all units -1 HP | Low | Custom |

### 5.3 Turn Structure

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 5.3.1 | Alternating action order | Medium | Custom |
| 5.3.2 | Round-end processing order | Medium | BD1-BD6 |

### 5.4 Timer (Optional)

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 5.4.1 | 10-second action timer | Medium | Custom |
| 5.4.2 | Timeout penalty (Hero -1 HP) | Low | Custom |

**Estimated Time**: 4-6 hours

---

## Phase 6: Draft Phase (Pre-game Selection)

**Goal**: Implement pre-game selection mechanism

### 6.1 Minion Selection

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 6.1.1 | DraftState model | Low | Custom |
| 6.1.2 | Pick 2 from 3 minions logic | Medium | Custom |
| 6.1.3 | Hidden selection | Low | Custom |

### 6.2 Skill Selection

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 6.2.1 | Show 3 skills based on class | Low | Custom |
| 6.2.2 | Select 1 skill | Low | Custom |

### 6.3 Draft Protocol

| # | Task | Complexity | Tests |
|---|------|------------|-------|
| 6.3.1 | WebSocket Draft messages | Medium | Custom |
| 6.3.2 | Draft complete → Game start | Low | Custom |

**Estimated Time**: 4-6 hours

---

## Overview: Priority Order

```
┌─────────────────────────────────────────────────────────────────┐
│  HIGHEST PRIORITY (Must complete first)                         │
├─────────────────────────────────────────────────────────────────┤
│  Phase 0: Complete V1 BUFF Foundation              [2-4 hours]  │
│  Phase 1: Model Layer Extension                    [4-6 hours]  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  HIGH PRIORITY (Core Mechanics)                                 │
├─────────────────────────────────────────────────────────────────┤
│  Phase 2: V3 BUFF System                          [8-12 hours]  │
│  Phase 3: Unit Passive Abilities (Guardian)        [4-6 hours]  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  MEDIUM PRIORITY (Core Gameplay)                                │
├─────────────────────────────────────────────────────────────────┤
│  Phase 4: Hero Skill System                       [16-24 hours] │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  LOWER PRIORITY (Complete Game)                                 │
├─────────────────────────────────────────────────────────────────┤
│  Phase 5: Game Flow Extension                      [4-6 hours]  │
│  Phase 6: Draft Phase                              [4-6 hours]  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Time Estimate Overview

| Phase | Content | Estimate | Cumulative |
|-------|---------|----------|------------|
| 0 | Complete V1 BUFF | 2-4 hrs | 2-4 hrs |
| 1 | Model Layer Extension | 4-6 hrs | 6-10 hrs |
| 2 | V3 BUFF System | 8-12 hrs | 14-22 hrs |
| 3 | Guardian Passive | 4-6 hrs | 18-28 hrs |
| 4 | Skill System | 16-24 hrs | 34-52 hrs |
| 5 | Game Flow | 4-6 hrs | 38-58 hrs |
| 6 | Draft Phase | 4-6 hrs | 42-64 hrs |

**Total**: Approximately **42-64 hours** development time (core engine, excluding UI)

---

## Claude CLI Suggested Command Order

### Phase 0
```bash
claude "Complete RuleEngine V1 BUFF integration, make all RuleEngineBuffFlowTest pass"
```

### Phase 1
```bash
claude "Extend Unit model per docs/UNIT_TYPES_V3.md, add UnitCategory, HeroClass fields"
claude "Add BuffTile and Obstacle classes, update GameState"
```

### Phase 2
```bash
claude "Implement 6 V3 BUFF types per docs/BUFF_SYSTEM_V3.md"
claude "Implement SLOW buff preparing state system"
claude "Implement SPEED buff double action system"
claude "Implement BUFF tile and obstacle system"
```

### Phase 3
```bash
claude "Implement TANK Guardian passive, intercept damage to adjacent allies"
```

### Phase 4
```bash
claude "Build skill system infrastructure: SkillDefinition, SkillRegistry, cooldown system"
claude "Implement WARRIOR's 3 skills"
# ... repeat for other classes
```

### Phase 5-6
```bash
claude "Implement V3 victory condition: hero death ends game"
claude "Implement minion decay and round 8 pressure system"
claude "Implement Draft phase minion and skill selection"
```

---

## Checkpoints

### ✅ Checkpoint 1: Models Complete
- [ ] All new classes compile
- [ ] GameStateSerializer updated
- [ ] Serialization tests pass

### ✅ Checkpoint 2: BUFF System Complete
- [ ] BUFF_SYSTEM_V3_TESTPLAN 141 tests pass
- [ ] SLOW/SPEED special states work

### ✅ Checkpoint 3: Skill System Complete
- [ ] SKILL_SYSTEM_V3_TESTPLAN 201 tests pass
- [ ] All 18 skills functional

### ✅ Checkpoint 4: Core Engine Complete
- [ ] All tests pass (~577+ tests)
- [ ] Full game playable (without UI)

---

## Test Case Overview

| Document | Test Count | Phase |
|----------|------------|-------|
| Existing Tests | 235 | ✅ |
| BUFF_SYSTEM_V3_TESTPLAN | 141 | Phase 2 |
| SKILL_SYSTEM_V3_TESTPLAN | 201 | Phase 4 |
| **Total** | **577+** | — |

---

## Key Milestones

| Milestone | Completion Criteria |
|-----------|---------------------|
| **M1: BUFF Ready** | Phase 0-2 complete, 141 BUFF tests pass |
| **M2: Combat Complete** | Phase 3 complete, Guardian interception works |
| **M3: Skills Ready** | Phase 4 complete, 201 skill tests pass |
| **M4: Core Complete** | Phase 5-6 complete, full game playable |

---

*Last updated: 2025-12-03*
